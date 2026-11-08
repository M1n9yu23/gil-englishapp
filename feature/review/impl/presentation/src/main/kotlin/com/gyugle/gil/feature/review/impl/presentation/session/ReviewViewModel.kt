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
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.core.speech.englishVoice
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.review.impl.domain.ReviewDeck
import com.gyugle.gil.feature.review.impl.domain.gradeTyped
import com.gyugle.gil.feature.review.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.review.impl.domain.model.ReviewCard
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Runs one review session over the cards due today, at most [limit] of them, the longest overdue
 * first.
 *
 * Each answer is rated and recorded at once, so leaving keeps what was done and the next session
 * starts with the rest. The rating is saved for process death just before it is written; if the
 * process dies between the two, the card is passed over this session and stays due. The list of cards, the place in it and the ratings so far survive process
 * death; an answer shown when the process died is not shown again.
 */
@HiltViewModel(assistedFactory = ReviewViewModel.Factory::class)
class ReviewViewModel @AssistedInject constructor(
    @Assisted private val limit: Int,
    private val savedStateHandle: SavedStateHandle,
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
        fun create(limit: Int): ReviewViewModel
    }

    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    private val eventChannel = Channel<ReviewEvent>()
    val events: Flow<ReviewEvent> = eventChannel.receiveAsFlow()

    private var session: Session? = null
    private var card: ReviewCard? = null
    private var hasVoice = false
    private var picked: Int? = null
    private var feedback: FeedbackUi? = null
    private var text: String = savedStateHandle[KEY_TEXT] ?: ""

    /** Writes one rating at a time, so the result reads them all back. */
    private val writeLock = Mutex()
    private val writes = mutableListOf<Job>()

    /** When the time now counting started; null while the screen is away or no card shows. */
    private var activeSince: Instant? = null
    private var isResumed = false

    private var soundJob: Job? = null

    /** Loading the session or the next card. Taps that would start another wait for it. */
    private var stepJob: Job? = null

    /** Cards no kind of question can be made for; they stay due but do not count as left. */
    private val skipped = mutableSetOf<StudyItemId>()

    private var position: Int
        get() = savedStateHandle[KEY_POSITION] ?: 0
        set(value) {
            savedStateHandle[KEY_POSITION] = value
        }

    private var ratings: List<Pair<StudyItemId, ReviewRating>>
        get() = savedStateHandle.get<ArrayList<String>>(KEY_RATINGS).orEmpty().mapNotNull(::decodeRating)
        set(value) {
            savedStateHandle[KEY_RATINGS] =
                ArrayList(value.map { (id, rating) -> "${rating.name}$SEPARATOR${id.value}" })
        }

    /** The defaults until the settings are read, which takes a moment at most. */
    private var settings = AppSettings()

    init {
        appSettings.observeSettings().onEach { settings = it }.launchIn(viewModelScope)
        load()
    }

    fun onAction(action: ReviewAction) {
        when (action) {
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
                stepJob = viewModelScope.launch { showNext() }
            }

            ReviewAction.OnPlayClick -> card?.spoken(hasVoice)?.let(::speak)

            ReviewAction.OnRetryClick -> when {
                stepJob?.isActive == true -> Unit
                session == null -> load()
                else -> stepJob = viewModelScope.launch { showNext() }
            }

            ReviewAction.OnKeepReviewingClick -> {
                if (stepJob?.isActive == true) return
                session = null
                skipped.clear()
                savedStateHandle.remove<ArrayList<String>>(KEY_IDS)
                savedStateHandle.remove<ArrayList<String>>(KEY_RATINGS)
                position = 0
                load()
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
        }
    }

    private fun load() {
        _state.value = ReviewState(ReviewPhase.Loading)
        stepJob?.cancel()
        stepJob = viewModelScope.launch {
            val today = clock.today()
            val read = readRecord(today)
            if (read == null) {
                _state.value = ReviewState(ReviewPhase.Failed)
                return@launch
            }
            val (ids, cards) = read
            if (ids.isEmpty()) {
                _state.value = ReviewState(ReviewPhase.Empty(nextInDays(cards, today)))
                return@launch
            }
            val pool = when (val items = lexicon.getItems(cards.map { it.itemId })) {
                is Outcome.Success -> items.data

                is Outcome.Failure -> {
                    _state.value = ReviewState(ReviewPhase.Failed)
                    return@launch
                }
            }
            hasVoice = waitForVoice()
            session = Session(ids, cards.associateBy { it.itemId }, pool, seed = seed())
            showNext()
        }
    }

    /** The cards of this session, saved the first time, and every card the learner has. */
    private suspend fun readRecord(today: StudyDay): Pair<List<StudyItemId>, List<CardState>>? = try {
        val saved = savedStateHandle.get<ArrayList<String>>(KEY_IDS)?.mapNotNull(StudyItemId::parse)
        val ids = saved ?: progressObserver.observeDueCards(today).first().take(limit).map { it.itemId }.also {
            savedStateHandle[KEY_IDS] = ArrayList(it.map(StudyItemId::value))
        }
        ids to progressObserver.observeCards().first()
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Timber.e(e, "Could not read the review schedule")
        null
    }

    private fun seed(): Int = savedStateHandle[KEY_SEED] ?: Random.nextInt().also { savedStateHandle[KEY_SEED] = it }

    /** Shows the card at [position], passing over cards already answered or that cannot be asked. */
    private suspend fun showNext() {
        val current = session ?: return
        val answered = ratings.mapTo(mutableSetOf()) { it.first }
        while (position < current.ids.size) {
            val id = current.ids[position]
            val state = current.cards[id]
            if (id !in answered && state == null) skipped += id
            if (id !in answered && state != null) {
                val random = Random(current.seed + position)
                when (val made = deck.cardFor(state, current.pool, hasVoice, random)) {
                    is Outcome.Failure -> {
                        _state.value = ReviewState(ReviewPhase.Failed)
                        return
                    }

                    is Outcome.Success -> {
                        val next = made.data
                        if (next == null) {
                            skipped += id
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
        val itemId = answeredCard.itemId
        feedback = answeredCard.feedback(grade)
        ratings = ratings + (itemId to grade.rating)
        render()
        writes.removeAll { it.isCompleted }
        writes += viewModelScope.launch {
            // Leaving right after answering must not lose the rating.
            withContext(NonCancellable) {
                writeLock.withLock {
                    progress.recordReview(itemId, grade.rating).onFailure {
                        Timber.w("Could not record a review: %s", it)
                    }
                    when (grade) {
                        AnswerGrade.WRONG -> progress.recordMistake(itemId)
                        AnswerGrade.CORRECT -> progress.clearMistake(itemId)
                        AnswerGrade.CLOSE -> Outcome.Success(Unit)
                    }.onFailure { Timber.w("Could not update the mistakes: %s", it) }
                }
            }
        }
    }

    private suspend fun finish() {
        card = null
        recordStudyTime(takeStudyTime())
        writes.joinAll()
        val current = session
        val today = clock.today()
        val done = ratings
        val pool = current?.pool.orEmpty().associateBy { it.id }
        val result = try {
            val due = progressObserver.observeDueCards(today).first()
            val cards = progressObserver.observeCards().first()
            ResultUi(
                answered = done.size,
                right = done.count { it.second != ReviewRating.AGAIN },
                again = done.filter { it.second == ReviewRating.AGAIN }.mapNotNull { pool[it.first]?.toResultItem() },
                remaining = due.count { it.itemId !in skipped },
                nextInDays = if (due.none { it.itemId !in skipped }) nextInDays(cards, today) else null
            )
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Timber.e(e, "Could not read the review schedule")
            _state.value = ReviewState(ReviewPhase.Failed)
            return
        }
        _state.value = ReviewState(ReviewPhase.Finished(result))
    }

    private fun render() {
        val current = card ?: return
        val ids = session?.ids ?: return
        _state.value = ReviewState(
            ReviewPhase.Asking(
                number = position + 1,
                count = ids.size,
                kind = current.kindUi(hasVoice),
                card = current.toCardUi(hasVoice, picked, text),
                hasVoice = hasVoice,
                feedback = feedback
            )
        )
    }

    private fun speak(text: String) {
        if (!hasVoice) return
        soundJob?.cancel()
        val language = speaker.status.value.englishVoice(preferBritish = settings.accent == Accent.BRITISH)
        soundJob = viewModelScope.launch { speaker.speak(text, language, settings.firstPlayRate) }
    }

    /** While the engine is still connecting, waits a little: cards made without a voice never play. */
    private suspend fun waitForVoice(): Boolean {
        val status = withTimeoutOrNull(VOICE_WAIT) { speaker.status.first { it !is SpeakerStatus.Connecting } }
        return status is SpeakerStatus.Connected &&
            status.voice(SpeechLanguage.AMERICAN_ENGLISH) == VoiceState.INSTALLED
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

    private class Session(
        val ids: List<StudyItemId>,
        val cards: Map<StudyItemId, CardState>,
        val pool: List<LessonItem>,
        val seed: Int
    )

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_SEED = "seed"
        const val KEY_POSITION = "position"
        const val KEY_RATINGS = "ratings"
        const val KEY_TEXT = "text"
        const val SEPARATOR = '|'
        val VOICE_WAIT = 3.seconds

        fun decodeRating(stored: String): Pair<StudyItemId, ReviewRating>? {
            val rating =
                ReviewRating.entries.firstOrNull { it.name == stored.substringBefore(SEPARATOR) } ?: return null
            val id = StudyItemId.parse(stored.substringAfter(SEPARATOR)) ?: return null
            return id to rating
        }
    }
}

/** Days from [today] to the earliest card, never less than one; null when there are no cards. */
internal fun nextInDays(cards: List<CardState>, today: StudyDay): Int? =
    cards.minOfOrNull { it.dueDay }?.let { today.daysUntil(it).toInt().coerceAtLeast(1) }

private fun LessonItem.toResultItem(): ResultItemUi = if (id is SoundId) {
    ResultItemUi(text = text, detail = ipa, detailIsIpa = true)
} else {
    ResultItemUi(text = text, detail = meaningKo, detailIsIpa = false)
}

/** The first play follows the learner's speed; "play slowly" stays slow either way. */
private val AppSettings.firstPlayRate: SpeechRate
    get() = if (speed == SpeechSpeed.SLOW) SpeechRate.SLOW else SpeechRate.NORMAL
