/*
 * Copyright 2026 MinGyu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gyugle.gil.feature.review.impl.presentation.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.englishVoice
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.review.api.hasStageTest
import com.gyugle.gil.feature.review.impl.domain.ReviewDeck
import com.gyugle.gil.feature.review.impl.domain.StageTestPlan
import com.gyugle.gil.feature.review.impl.domain.StageTestPlanner
import com.gyugle.gil.feature.review.impl.domain.allowedWrong
import com.gyugle.gil.feature.review.impl.domain.gradeTyped
import com.gyugle.gil.feature.review.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.review.impl.domain.model.ReviewCard
import com.gyugle.gil.feature.review.impl.domain.pickTestItems
import com.gyugle.gil.feature.review.impl.domain.testCardKinds
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Runs the test of one stage for a learner who already knows it: an intro, then questions about
 * items spread over the stage, then the result.
 *
 * Nothing but study time is recorded while the test runs, so leaving drops the answers. The test
 * ends as soon as more answers are wrong than passing allows. A pass records the lessons left in
 * the stage and before it as finished, and starts a card for each item asked, rated by its answer.
 * The questions, the place in them, the answers and whether the test started survive process death.
 */
@HiltViewModel(assistedFactory = StageTestViewModel.Factory::class)
class StageTestViewModel @AssistedInject constructor(
    // A String, not StageId: Dagger cannot inject a value class parameter.
    @Assisted stageId: String,
    private val savedStateHandle: SavedStateHandle,
    private val planner: StageTestPlanner,
    private val progressObserver: ProgressObserver,
    private val progress: ProgressCommands,
    private val lexicon: LexiconObserver,
    private val deck: ReviewDeck,
    private val speaker: Speaker,
    private val clock: StudyClock,
    private val appSettings: AppSettingsObserver
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(stageId: String): StageTestViewModel
    }

    private val stageId = StageId(stageId)

    private val _state = MutableStateFlow(ReviewState(isStageTest = true))
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    private val eventChannel = Channel<ReviewEvent>()
    val events: Flow<ReviewEvent> = eventChannel.receiveAsFlow()

    private var test: Test? = null
    private var card: ReviewCard? = null
    private var hasVoice = false
    private var picked: Int? = null
    private var feedback: FeedbackUi? = null
    private var text: String = savedStateHandle[KEY_TEXT] ?: ""

    /** When the time now counting started; null while the screen is away or no card shows. */
    private var activeSince: Instant? = null
    private var isResumed = false

    private var soundJob: Job? = null

    /** Loading the test, the next card or recording a pass. Taps that would start another wait for it. */
    private var stepJob: Job? = null

    private var position: Int
        get() = savedStateHandle[KEY_POSITION] ?: 0
        set(value) {
            savedStateHandle[KEY_POSITION] = value
        }

    private var answers: List<Pair<StudyItemId, ReviewRating>>
        get() = savedStateHandle.get<ArrayList<String>>(KEY_ANSWERS).orEmpty().toRatings()
        set(value) {
            savedStateHandle[KEY_ANSWERS] = value.toSavedRatings()
        }

    /** Items no kind of question can be made for. They are passed over and do not count. */
    private var unaskable: Set<StudyItemId>
        get() = savedStateHandle.get<ArrayList<String>>(KEY_UNASKABLE).orEmpty().mapNotNullTo(mutableSetOf()) {
            StudyItemId.parse(it)
        }
        set(value) {
            savedStateHandle[KEY_UNASKABLE] = ArrayList(value.map(StudyItemId::value))
        }

    private var started: Boolean
        get() = savedStateHandle[KEY_STARTED] ?: false
        set(value) {
            savedStateHandle[KEY_STARTED] = value
        }

    private var recorded: Boolean
        get() = savedStateHandle[KEY_RECORDED] ?: false
        set(value) {
            savedStateHandle[KEY_RECORDED] = value
        }

    /** The defaults until the settings are read, which takes a moment at most. */
    private var settings = AppSettings()

    init {
        appSettings.observeSettings().onEach { settings = it }.launchIn(viewModelScope)
        load()
    }

    fun onAction(action: ReviewAction) {
        when (action) {
            ReviewAction.OnStartTestClick -> {
                if (test == null || started || stepJob?.isActive == true) return
                started = true
                stepJob = viewModelScope.launch { showNext() }
            }

            is ReviewAction.OnOptionClick -> {
                val choice = card as? ReviewCard.ChoiceCard ?: return
                if (feedback != null || action.index !in 0 until choice.optionCount) return
                picked = action.index
                answer(if (action.index == choice.answer) AnswerGrade.CORRECT else AnswerGrade.WRONG)
            }

            is ReviewAction.OnTextChange -> {
                if (feedback != null) return
                text = action.text
                savedStateHandle[KEY_TEXT] = action.text
                render()
            }

            ReviewAction.OnCheckClick -> {
                if (feedback != null || text.isBlank()) return
                when (val typed = card) {
                    is ReviewCard.Spell -> answer(gradeTyped(text, typed.word))
                    is ReviewCard.FillBlank -> answer(gradeTyped(text, typed.answer))
                    else -> Unit
                }
            }

            ReviewAction.OnContinueClick -> {
                if (feedback == null || stepJob?.isActive == true) return
                // Cleared at once, so a second tap while the next card loads does nothing.
                feedback = null
                card = null
                position += 1
                text = ""
                savedStateHandle[KEY_TEXT] = ""
                stepJob = viewModelScope.launch { if (cannotPass()) finish() else showNext() }
            }

            ReviewAction.OnPlayClick -> card?.spoken(hasVoice)?.let(::speak)

            ReviewAction.OnRetryClick -> when {
                stepJob?.isActive == true -> Unit
                test == null -> load()
                else -> stepJob = viewModelScope.launch { showNext() }
            }

            ReviewAction.OnCloseClick -> {
                soundJob?.cancel()
                // Taken now: the screen may be gone before it pauses.
                val millis = takeStudyTime()
                activeSince = null
                viewModelScope.launch {
                    recordStudyTime(millis)
                    eventChannel.send(ReviewEvent.Close)
                }
            }

            ReviewAction.OnResumed -> {
                isResumed = true
                startCounting()
            }

            ReviewAction.OnPaused -> {
                isResumed = false
                soundJob?.cancel()
                val millis = takeStudyTime()
                viewModelScope.launch { recordStudyTime(millis) }
            }

            // A test has no more cards to go on with.
            ReviewAction.OnKeepReviewingClick -> Unit
        }
    }

    private fun load() {
        show(ReviewPhase.Loading)
        stepJob?.cancel()
        stepJob = viewModelScope.launch {
            val plan = when (val read = planner.plan(stageId)) {
                is Outcome.Success -> read.data

                is Outcome.Failure -> {
                    Timber.w("Could not read the test of %s: %s", stageId.value, read.error)
                    show(ReviewPhase.Failed)
                    return@launch
                }
            }
            if (!hasStageTest(plan.items.size)) {
                Timber.w("Stage %s has too few items for a test", stageId.value)
                show(ReviewPhase.Failed)
                return@launch
            }
            val loaded = readTest(plan) ?: run {
                show(ReviewPhase.Failed)
                return@launch
            }
            hasVoice = speaker.waitForEnglishVoice()
            test = loaded
            if (started) showNext() else show(ReviewPhase.TestIntro(loaded.intro()))
        }
    }

    /**
     * The questions, picked the first time and saved, with wrong options from the stage's items and
     * the learner's own. Null when the record or the dictionary cannot be read.
     */
    private suspend fun readTest(plan: StageTestPlan): Test? {
        val seed: Int = savedStateHandle[KEY_SEED] ?: Random.nextInt().also { savedStateHandle[KEY_SEED] = it }
        val (completed, learnedIds) = try {
            progressObserver.observeCompletedLessons().first() to
                progressObserver.observeCards().first().map { it.itemId }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Timber.e(e, "Could not read the study record")
            return null
        }
        val learned = when (val read = lexicon.getItems(learnedIds)) {
            is Outcome.Success -> read.data

            is Outcome.Failure -> {
                Timber.w("Could not read the learned items: %s", read.error)
                return null
            }
        }
        val items = plan.items.associateBy { it.id }
        val saved = savedStateHandle.get<ArrayList<String>>(KEY_IDS)?.mapNotNull(StudyItemId::parse)
        val ids = saved ?: pickTestItems(plan.items, plan.questionCount, Random(seed)).map { it.id }.also {
            savedStateHandle[KEY_IDS] = ArrayList(it.map(StudyItemId::value))
        }
        // Saved too: once a pass is recorded, these lessons are no longer left to finish.
        val lessons = savedStateHandle.get<ArrayList<String>>(KEY_LESSONS)?.map(::LessonId)
            ?: plan.lessons.filter { it !in completed }.also {
                savedStateHandle[KEY_LESSONS] = ArrayList(it.map(LessonId::value))
            }
        return Test(
            plan = plan,
            ids = ids.filter { it in items },
            items = items,
            pool = (plan.items + learned).distinctBy { it.id },
            lessons = lessons,
            seed = seed
        )
    }

    /** Shows the card at [position], passing over questions already answered or that cannot be asked. */
    private suspend fun showNext() {
        val current = test ?: return
        val answered = answers.mapTo(mutableSetOf()) { it.first }
        while (position < current.ids.size) {
            val id = current.ids[position]
            val item = current.items.getValue(id)
            if (id !in answered && id !in unaskable) {
                val random = Random(current.seed + position)
                val kinds = testCardKinds(id, position, hasVoice)
                when (val made = deck.cardFor(item, kinds, current.pool, hasVoice, random)) {
                    is Outcome.Failure -> {
                        Timber.w("Could not make a test card: %s", made.error)
                        show(ReviewPhase.Failed)
                        return
                    }

                    is Outcome.Success -> {
                        val next = made.data
                        if (next == null) {
                            unaskable = unaskable + id
                        } else {
                            card = next
                            picked = null
                            feedback = null
                            render()
                            startCounting()
                            next.spoken(hasVoice)?.let(::speak)
                            return
                        }
                    }
                }
            }
            position += 1
        }
        finish()
    }

    private fun answer(grade: AnswerGrade) {
        val answeredCard = card ?: return
        feedback = answeredCard.feedback(grade).copy(comesBack = false)
        answers = answers + (answeredCard.itemId to grade.rating)
        render()
    }

    /** The questions that can be asked: those not passed over for want of a card. */
    private fun questionCount(current: Test): Int = current.ids.count { it !in unaskable }

    private fun wrongCount(): Int = answers.count { it.second == ReviewRating.AGAIN }

    private fun cannotPass(): Boolean {
        val current = test ?: return false
        return wrongCount() > allowedWrong(questionCount(current))
    }

    /** Shows how the test went, recording a pass first. A pass with nothing asked is no pass. */
    private suspend fun finish() {
        val current = test ?: return
        card = null
        recordStudyTime(takeStudyTime())
        val done = answers
        val passed = done.isNotEmpty() && !cannotPass()
        if (passed && !recorded) {
            val outcome = withContext(NonCancellable) { progress.skipLessons(current.lessons, done.toMap()) }
            if (outcome is Outcome.Failure) {
                Timber.w("Could not record the passed test: %s", outcome.error)
                show(ReviewPhase.Failed)
                return
            }
            recorded = true
        }
        show(
            ReviewPhase.TestFinished(
                TestResultUi(
                    stage = current.plan.stage.title.toLocalizedTextUi(),
                    passed = passed,
                    right = done.count { it.second != ReviewRating.AGAIN },
                    answered = done.size,
                    questions = questionCount(current),
                    lessons = current.lessons.size,
                    missed = done.filter { it.second == ReviewRating.AGAIN }
                        .mapNotNull { current.items[it.first]?.toResultItem() }
                )
            )
        )
    }

    private fun render() {
        val current = card ?: return
        val shown = test ?: return
        show(
            ReviewPhase.Asking(
                number = position + 1,
                count = shown.ids.size,
                kind = current.kindUi(hasVoice),
                card = current.toCardUi(hasVoice, picked, text),
                hasVoice = hasVoice,
                feedback = feedback
            )
        )
    }

    private fun show(phase: ReviewPhase) {
        _state.value = ReviewState(phase, isStageTest = true)
    }

    private fun speak(text: String) {
        if (!hasVoice) return
        soundJob?.cancel()
        val language = speaker.status.value.englishVoice(preferBritish = settings.accent == Accent.BRITISH)
        soundJob = viewModelScope.launch { speaker.speak(text, language, settings.firstPlayRate) }
    }

    private fun startCounting() {
        if (isResumed && card != null && activeSince == null) activeSince = clock.now()
    }

    /** Ends the time counted so far and returns it. Done before anything suspends, so it counts once. */
    private fun takeStudyTime(): Long {
        val since = activeSince ?: return 0
        val now = clock.now()
        activeSince = if (isResumed && card != null) now else null
        return (now - since).inWholeMilliseconds.coerceAtLeast(0)
    }

    private suspend fun recordStudyTime(millis: Long) {
        if (millis <= 0) return
        withContext(NonCancellable) {
            progress.recordStudyTime(millis.milliseconds).onFailure { Timber.w("Could not record study time: %s", it) }
        }
    }

    /**
     * The test as loaded: the questions in [ids], the stage's [items], the [pool] wrong options come
     * from, and the [lessons] a pass records.
     */
    private class Test(
        val plan: StageTestPlan,
        val ids: List<StudyItemId>,
        val items: Map<StudyItemId, LessonItem>,
        val pool: List<LessonItem>,
        val lessons: List<LessonId>,
        val seed: Int
    ) {
        fun intro(): TestIntroUi {
            val questions = ids.size
            return TestIntroUi(
                stage = plan.stage.title.toLocalizedTextUi(),
                questions = questions,
                passMark = questions - allowedWrong(questions),
                lessons = lessons.size
            )
        }
    }

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_LESSONS = "lessons"
        const val KEY_SEED = "seed"
        const val KEY_POSITION = "position"
        const val KEY_ANSWERS = "answers"
        const val KEY_UNASKABLE = "unaskable"
        const val KEY_STARTED = "started"
        const val KEY_RECORDED = "recorded"
        const val KEY_TEXT = "text"
    }
}
