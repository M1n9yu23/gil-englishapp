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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.speech.OnDeviceRecognizer
import com.gyugle.gil.core.speech.RecognitionError
import com.gyugle.gil.core.speech.RecognizerAvailability
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoicePlayer
import com.gyugle.gil.core.speech.VoiceRecorder
import com.gyugle.gil.core.speech.VoiceRecording
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.core.speech.englishVoice
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.lesson.impl.domain.SpeechMatcher
import com.gyugle.gil.feature.lesson.impl.domain.introducedRatings
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.lesson.impl.domain.repository.LessonRunRepository
import com.gyugle.gil.feature.lesson.impl.domain.repository.ScoringNoticeRepository
import com.gyugle.gil.feature.lesson.impl.domain.stepsThatNeedSound
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Plays one lesson: asks whether to continue a run left halfway, shows each step, grades it,
 * queues a missed step once more, and ends on the result, which goes to the learner's record.
 *
 * The run is saved after every answer and step, and when the screen leaves the front, so leaving
 * the lesson or losing the process keeps the place. A return after process death goes straight
 * back to the step, or to the result; opening the lesson again later asks first.
 *
 * A shadowing recording is checked word by word when the phone's on-device recognizer has an
 * English model; otherwise the learner compares by ear and is told why, once.
 */
@HiltViewModel(assistedFactory = LessonViewModel.Factory::class)
class LessonViewModel @AssistedInject constructor(
    // A String, not LessonId: Dagger cannot inject a value class parameter.
    @Assisted lessonId: String,
    private val savedStateHandle: SavedStateHandle,
    private val courseObserver: CourseObserver,
    private val runs: LessonRunRepository,
    private val progress: ProgressCommands,
    private val speaker: Speaker,
    private val recorder: VoiceRecorder,
    private val voicePlayer: VoicePlayer,
    private val clock: StudyClock,
    private val appSettings: AppSettingsObserver,
    private val recognizer: OnDeviceRecognizer,
    private val scoringNotices: ScoringNoticeRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(lessonId: String): LessonViewModel
    }

    private val lessonId = LessonId(lessonId)

    private val _state = MutableStateFlow(LessonState())
    val state: StateFlow<LessonState> = _state.asStateFlow()

    private val eventChannel = Channel<LessonEvent>()
    val events: Flow<LessonEvent> = eventChannel.receiveAsFlow()

    private var lesson: Lesson? = null
    private var run: LessonRun? = null
    private var savedRun: LessonRun? = null
    private var layout: StepLayout? = null
    private var input = StepInput()
    private var recording: VoiceRecording? = null
    private var isRecording = false
    private var recordingFailed = false
    private var microphone = MicrophoneUi.NOT_ASKED
    private var showsNoVoiceNotice = false
    private var score: ScoreUi? = null

    /** What the recognizer said about English; null until a speaking step asks. */
    private var scoring: RecognizerAvailability? = null

    /** A notice waiting for a speaking step with the microphone allowed. */
    private var pendingNotice: ScoringNoticeUi? = null
    private var notice: ScoringNoticeUi? = null
    private var result: ResultUi? = null

    /** The ratings of a finished lesson, kept until [ProgressCommands.completeLesson] succeeds. */
    private var pendingRatings: Map<StudyItemId, ReviewRating> = emptyMap()

    /** Writes runs one at a time, so a later run never lands before an earlier one. */
    private val saveLock = Mutex()

    /** When the time now counting started; null while the screen is in the background. */
    private var activeSince: Instant? = null
    private var isResumed = false

    private var soundJob: Job? = null
    private var recordJob: Job? = null
    private var scoreJob: Job? = null

    /** The defaults until the settings are read, which takes a moment at most. */
    private var settings = AppSettings()

    init {
        appSettings.observeSettings().onEach { settings = it }.launchIn(viewModelScope)
        load()
        viewModelScope.launch {
            speaker.status.collect { status ->
                if (status !is SpeakerStatus.Connecting) skipIfSoundless()
                render()
            }
        }
    }

    fun onAction(action: LessonAction) {
        when (action) {
            LessonAction.OnRetryLoadClick -> load()

            LessonAction.OnResumeClick -> savedRun?.let(::begin)

            LessonAction.OnRestartClick -> restart()

            LessonAction.OnCloseClick -> onClose()

            LessonAction.OnExitConfirm -> leave()

            LessonAction.OnExitDismiss -> _state.update { it.copy(isExitConfirmShown = false) }

            LessonAction.OnPlayClick -> playStep(settings.firstPlayRate)

            LessonAction.OnPlaySlowlyClick -> playStep(SpeechRate.SLOW)

            is LessonAction.OnPlaySentenceClick -> speak(action.text)

            LessonAction.OnShowMeaningClick -> updateInput { it.copy(isMeaningShown = true) }

            is LessonAction.OnOptionClick -> pick(action.index)

            is LessonAction.OnTileClick -> updateInput { current ->
                if (current.feedback != null || action.bankIndex in current.chosen) {
                    current
                } else {
                    current.copy(chosen = current.chosen + action.bankIndex)
                }
            }

            is LessonAction.OnChosenTileClick -> updateInput { current ->
                if (current.feedback != null) {
                    current
                } else {
                    current.copy(chosen = current.chosen.filterIndexed { index, _ -> index != action.position })
                }
            }

            is LessonAction.OnTextChange -> {
                savedStateHandle[KEY_TEXT] = action.text
                updateInput { if (it.feedback != null) it else it.copy(text = action.text) }
            }

            LessonAction.OnCheckClick -> check()

            LessonAction.OnContinueClick -> next()

            LessonAction.OnRecordClick -> record()

            LessonAction.OnStopRecordingClick -> recorder.stop()

            LessonAction.OnPlayMineClick -> playMine()

            LessonAction.OnAllowMicrophoneClick -> viewModelScope.launch {
                eventChannel.send(
                    if (microphone == MicrophoneUi.PERMANENTLY_DENIED) {
                        LessonEvent.OpenAppSettings
                    } else {
                        LessonEvent.RequestMicrophone
                    }
                )
            }

            is LessonAction.OnMicrophoneState -> {
                microphone = action.state.toMicrophoneUi()
                showNoticeIfDue()
                render()
            }

            LessonAction.OnSaveRetryClick -> saveResult()

            LessonAction.OnNextLessonClick -> lesson?.next?.let { next ->
                viewModelScope.launch { eventChannel.send(LessonEvent.OpenLesson(next)) }
            }

            LessonAction.OnDoneClick -> viewModelScope.launch { eventChannel.send(LessonEvent.Close) }

            LessonAction.OnResumed -> {
                isResumed = true
                startCounting()
            }

            LessonAction.OnPaused -> {
                isResumed = false
                soundJob?.cancel()
                recorder.stop()
                val millis = takeStudyTime()
                viewModelScope.launch {
                    recordStudyTime(millis)
                    saveRun()
                }
            }
        }
    }

    private fun load() {
        _state.update { it.copy(phase = LessonPhase.Loading) }
        viewModelScope.launch {
            courseObserver.getLesson(lessonId)
                .onSuccess { loaded ->
                    lesson = loaded
                    _state.update { it.copy(title = loaded.title.toLocalizedTextUi()) }
                    if (restoreResult(loaded)) return@onSuccess
                    val saved = runs.get(lessonId)?.takeIf { it.stepCount == loaded.steps.size && !it.isFinished }
                    when {
                        saved == null -> begin(startRun(loaded))

                        // Back after the process died: the learner never left, so do not ask.
                        savedStateHandle.get<Boolean>(KEY_STARTED) == true -> begin(saved, keepInput = true)

                        saved.position == 0 && saved.outcomes.isEmpty() -> begin(saved)

                        else -> {
                            savedRun = saved
                            _state.update {
                                it.copy(
                                    phase = LessonPhase.AskResume(step = saved.position + 1, count = saved.queue.size)
                                )
                            }
                        }
                    }
                }
                .onFailure { error -> _state.update { it.copy(phase = LessonPhase.Failed(error.toLessonErrorUi())) } }
        }
    }

    /** A new run. Without a voice, the steps that only work with sound are left out from the start. */
    private suspend fun startRun(lesson: Lesson): LessonRun {
        val voice = withTimeoutOrNull(VOICE_WAIT) { speaker.status.first { it !is SpeakerStatus.Connecting } }
        val skipped = if (voice != null && !voice.hasVoice()) stepsThatNeedSound(lesson.steps) else emptySet()
        showsNoVoiceNotice = voice != null && !voice.hasVoice()
        return LessonRun.start(lesson.steps.size, skipped)
    }

    private fun restart() {
        val loaded = lesson ?: return
        // Only from the question, and only once: the new run takes a moment to start.
        if (_state.value.phase !is LessonPhase.AskResume) return
        savedRun = null
        _state.update { it.copy(phase = LessonPhase.Loading) }
        viewModelScope.launch {
            runs.clear(lessonId)
            begin(startRun(loaded))
        }
    }

    private fun begin(started: LessonRun, keepInput: Boolean = false) {
        savedRun = null
        savedStateHandle[KEY_STARTED] = true
        // A step answered just before the process died moves on; its answer is already saved.
        run = if (started.isAnswered) started.next() else started
        enterStep(keepInput = keepInput && !started.isAnswered)
        startCounting()
    }

    private fun enterStep(keepInput: Boolean = false) {
        val current = run ?: return
        val loaded = lesson ?: return
        soundJob?.cancel()
        recordJob?.cancel()
        scoreJob?.cancel()
        recording = null
        isRecording = false
        recordingFailed = false
        score = null
        notice = null
        if (current.isFinished) {
            finish()
            return
        }
        val index = current.currentStep ?: return
        val step = loaded.steps[index]
        // The voice can disappear between sessions; a step that needs it is passed over.
        if (!hasVoice() && step.needsSound()) {
            run = current.next()
            enterStep()
            return
        }
        input = if (keepInput) StepInput(text = savedStateHandle[KEY_TEXT] ?: "") else StepInput()
        if (!keepInput) savedStateHandle[KEY_TEXT] = ""
        layout = StepLayout(step, seed = seedFor(index, current.isRetry))
        render()
        autoPlay(step)
        if (step is LessonStep.Shadow) checkScoring()
    }

    private fun pick(displayIndex: Int) {
        val current = run ?: return
        val step = currentStep() ?: return
        if (input.feedback != null) return
        val (grade, feedback) = step.checkPick(displayIndex, input, layout ?: return, firstTry = !current.isRetry)
            ?: return
        val missed = feedback.grade == GradeUi.WRONG
        input = input.copy(
            picked = displayIndex,
            feedback = feedback,
            grade = grade,
            missedQuestion = input.missedQuestion || missed
        )
        if (grade != null) answer(grade)
        render()
    }

    private fun check() {
        val current = run ?: return
        val step = currentStep() ?: return
        if (!step.canCheck(input)) return
        val (grade, feedback) = step.checkTyped(input, layout ?: return, firstTry = !current.isRetry) ?: return
        input = input.copy(feedback = feedback, grade = grade)
        answer(grade)
        render()
        if (step is LessonStep.Dictation || step is LessonStep.Spell) playStep(settings.firstPlayRate)
    }

    private fun answer(grade: AnswerGrade) {
        val current = run ?: return
        val item = (currentStep() as? LessonStep.Graded)?.item
        run = current.answer(grade)
        viewModelScope.launch {
            saveRun()
            if (grade == AnswerGrade.WRONG && !current.isRetry && item != null) {
                progress.recordMistake(item).onFailure { Timber.w("Could not record a mistake: %s", it) }
            }
        }
    }

    private fun next() {
        val current = run ?: return
        val step = currentStep() ?: return
        if (step is LessonStep.Graded && input.grade == null) {
            // A passage moves to its next question; the step is graded after the last one.
            if (step is LessonStep.ReadPassage && input.feedback != null) {
                input = input.copy(question = input.question + 1, picked = null, feedback = null)
                render()
            }
            return
        }
        showsNoVoiceNotice = false
        run = current.next()
        viewModelScope.launch { saveRun() }
        enterStep()
    }

    private fun onClose() {
        if (_state.value.phase is LessonPhase.Playing) {
            _state.update { it.copy(isExitConfirmShown = true) }
        } else {
            viewModelScope.launch { eventChannel.send(LessonEvent.Close) }
        }
    }

    private fun leave() {
        _state.update { it.copy(isExitConfirmShown = false) }
        soundJob?.cancel()
        val millis = takeStudyTime()
        viewModelScope.launch {
            recordStudyTime(millis)
            saveRun()
            eventChannel.send(LessonEvent.Close)
        }
    }

    /**
     * Shows the result and saves it. Everything before the save happens at once, so the lesson
     * still reaches the record if the screen goes away right after the last step.
     */
    private fun finish() {
        val loaded = lesson ?: return
        val finished = run ?: return
        val millis = takeStudyTime()
        pendingRatings = introducedRatings(loaded.introduced.map { it.id }, loaded.steps, finished)
        val minutes = minutesOf(run?.studiedMillis ?: finished.studiedMillis)
        savedStateHandle[KEY_RESULT_MINUTES] = minutes
        savedStateHandle[KEY_RESULT_RATINGS] =
            ArrayList(pendingRatings.map { (item, rating) -> "${item.value}=${rating.name}" })
        result = resultOf(loaded, minutes, pendingRatings)
        render()
        saveResult()
        viewModelScope.launch { recordStudyTime(millis) }
    }

    /** After process death on the result: the same result, saved again if it was not saved yet. */
    private fun restoreResult(loaded: Lesson): Boolean {
        val minutes = savedStateHandle.get<Int>(KEY_RESULT_MINUTES) ?: return false
        val stored = savedStateHandle.get<ArrayList<String>>(KEY_RESULT_RATINGS) ?: return false
        pendingRatings = stored.mapNotNull { entry ->
            val parts = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val id = StudyItemId.parse(parts[0]) ?: return@mapNotNull null
            val rating = ReviewRating.entries.firstOrNull { it.name == parts[1] } ?: return@mapNotNull null
            id to rating
        }.toMap()
        result = resultOf(loaded, minutes, pendingRatings)
        if (savedStateHandle.get<Boolean>(KEY_RESULT_SAVED) == true) {
            result = result?.copy(isSaving = false)
            render()
        } else {
            saveResult()
        }
        return true
    }

    private fun resultOf(loaded: Lesson, minutes: Int, ratings: Map<StudyItemId, ReviewRating>) = ResultUi(
        minutes = minutes,
        goal = loaded.goal.toLocalizedTextUi(),
        items = loaded.introduced.map { item ->
            ResultItemUi(item.text, item.ipa, item.meaningKo, comesBackSoon = ratings[item.id] != ReviewRating.GOOD)
        },
        hasNext = loaded.next != null,
        isSaving = true,
        saveFailed = false
    )

    private fun saveResult() {
        if (result == null) return
        result = result?.copy(isSaving = true, saveFailed = false)
        render()
        val ratings = pendingRatings
        viewModelScope.launch {
            // Leaving the result while it saves must not cut the write short.
            val saved = withContext(NonCancellable) {
                progress.completeLesson(lessonId, ratings).onSuccess { runs.clear(lessonId) }
            }
            saved
                .onSuccess {
                    savedStateHandle[KEY_RESULT_SAVED] = true
                    result = result?.copy(isSaving = false)
                }
                .onFailure {
                    Timber.w("Could not save a finished lesson: %s", it)
                    result = result?.copy(isSaving = false, saveFailed = true)
                }
            render()
        }
    }

    private fun record() {
        if (isRecording || microphone != MicrophoneUi.GRANTED) return
        soundJob?.cancel()
        scoreJob?.cancel()
        recordJob = viewModelScope.launch {
            isRecording = true
            recordingFailed = false
            score = null
            render()
            recorder.record(MAX_RECORDING)
                .onSuccess { mine ->
                    recording = mine
                    if (scoring == RecognizerAvailability.READY) score(mine)
                }
                .onFailure { recordingFailed = true }
            isRecording = false
            render()
        }
    }

    /** Asks the recognizer what it heard and marks each word of the sentence by it. */
    private fun score(mine: VoiceRecording) {
        val sentence = (currentStep() as? LessonStep.Shadow)?.sentence?.text ?: return
        score = ScoreUi.Checking
        scoreJob = viewModelScope.launch {
            recognizer.recognize(mine, SpeechLanguage.AMERICAN_ENGLISH)
                .onSuccess { heard ->
                    score =
                        ScoreUi.Heard(SpeechMatcher.match(sentence, heard).map { MarkedWordUi(it.text, it.matched) })
                }
                .onFailure { error ->
                    score = when (error) {
                        RecognitionError.NO_SPEECH -> ScoreUi.NothingHeard

                        RecognitionError.UNAVAILABLE -> {
                            // The model went away since the check; compare by ear from now on.
                            scoring = RecognizerAvailability.UNAVAILABLE
                            null
                        }

                        RecognitionError.FAILED -> {
                            Timber.w("Could not check a recording")
                            null
                        }
                    }
                }
            render()
        }
    }

    /**
     * Finds out whether recordings can be checked. Asked again on each speaking step until the
     * answer is yes, so a model installed during the lesson is picked up.
     */
    private fun checkScoring() {
        if (scoring == RecognizerAvailability.READY) return
        viewModelScope.launch {
            val availability = recognizer.availability(SpeechLanguage.AMERICAN_ENGLISH)
            scoring = availability
            if (availability != RecognizerAvailability.READY && pendingNotice == null && notice == null &&
                !scoringNotices.wasShown()
            ) {
                pendingNotice = if (availability == RecognizerAvailability.NOT_DOWNLOADED) {
                    ScoringNoticeUi.MODEL_NOT_INSTALLED
                } else {
                    ScoringNoticeUi.UNSUPPORTED
                }
            }
            showNoticeIfDue()
            render()
        }
    }

    /** The notice means nothing without the microphone, so it waits until the learner can record. */
    private fun showNoticeIfDue() {
        val due = pendingNotice ?: return
        if (microphone != MicrophoneUi.GRANTED || currentStep() !is LessonStep.Shadow) return
        pendingNotice = null
        notice = due
        viewModelScope.launch {
            scoringNotices.markShown().onFailure { Timber.w("Could not remember the scoring notice: %s", it) }
        }
    }

    private fun playMine() {
        val mine = recording ?: return
        soundJob?.cancel()
        soundJob = viewModelScope.launch { voicePlayer.play(mine) }
    }

    private fun autoPlay(step: LessonStep) {
        if (!hasVoice()) return
        when (step) {
            is LessonStep.Listen, is LessonStep.Shadow, is LessonStep.ListenChoose, is LessonStep.ChooseMeaning,
            is LessonStep.Dictation, is LessonStep.MinimalPair, is LessonStep.Spell, is LessonStep.Dialogue
            -> playStep(settings.firstPlayRate)

            else -> Unit
        }
    }

    /** Plays what the step is about: its sentence, the played word of a pair, or every dialogue line. */
    private fun playStep(rate: SpeechRate) {
        val step = currentStep() ?: return
        val texts = when (step) {
            is LessonStep.Listen -> listOf(step.sentence.text)
            is LessonStep.Shadow -> listOf(step.sentence.text)
            is LessonStep.ChooseMeaning -> listOf(step.prompt.text)
            is LessonStep.ListenChoose -> listOf(step.audio.text)
            is LessonStep.Dictation -> listOf(step.sentence.text)
            is LessonStep.MinimalPair -> listOfNotNull(step.words.getOrNull(step.answer)?.text)
            is LessonStep.Spell -> listOf(step.answer)
            is LessonStep.Dialogue -> step.lines.map { it.sentence.text }
            is LessonStep.FillBlank -> listOf(step.sentence.text)
            else -> emptyList()
        }
        speak(texts, rate)
    }

    private fun speak(text: String) = speak(listOf(text), settings.firstPlayRate)

    private fun speak(texts: List<String>, rate: SpeechRate) {
        if (texts.isEmpty() || !hasVoice()) return
        soundJob?.cancel()
        soundJob = viewModelScope.launch {
            val language = speaker.status.value.englishVoice(preferBritish = settings.accent == Accent.BRITISH)
            for (text in texts) speaker.speak(text, language, rate)
        }
    }

    private fun startCounting() {
        if (isResumed && run != null && activeSince == null && result == null) activeSince = clock.now()
    }

    /**
     * Ends the time counted so far, adds it to the run and returns it for [recordStudyTime]. Done
     * at once, before anything suspends, so two callers never count the same time.
     */
    private fun takeStudyTime(): Long {
        val since = activeSince ?: return 0
        val now = clock.now()
        activeSince = if (isResumed && result == null) now else null
        val millis = (now - since).inWholeMilliseconds
        if (millis <= 0) return 0
        run = run?.studied(millis)
        return millis
    }

    private suspend fun recordStudyTime(millis: Long) {
        if (millis <= 0) return
        progress.recordStudyTime(millis.milliseconds).onFailure { Timber.w("Could not record study time: %s", it) }
    }

    private suspend fun saveRun() {
        saveLock.withLock {
            // The newest run once the lock is ours, so an older one never overwrites it.
            val latest = run?.takeIf { !it.isFinished } ?: return
            runs.save(lessonId, latest).onFailure { Timber.w("Could not save the place in a lesson: %s", it) }
        }
    }

    /** A sound-only step on screen when the voice turns out to be missing is passed over. */
    private fun skipIfSoundless() {
        val current = run ?: return
        val step = currentStep() ?: return
        if (result != null || hasVoice() || !step.needsSound() || input.feedback != null) return
        showsNoVoiceNotice = true
        run = current.next()
        viewModelScope.launch { saveRun() }
        enterStep()
    }

    private fun currentStep(): LessonStep? = run?.currentStep?.let { lesson?.steps?.getOrNull(it) }

    private fun updateInput(transform: (StepInput) -> StepInput) {
        input = transform(input)
        render()
    }

    /** While the engine is still connecting, a voice is assumed, so no step is skipped too early. */
    private fun hasVoice(): Boolean = speaker.status.value.let { it is SpeakerStatus.Connecting || it.hasVoice() }

    private fun seedFor(stepIndex: Int, isRetry: Boolean): Int =
        (lessonId.value.hashCode() * 31 + stepIndex) * 2 + if (isRetry) 1 else 0

    private fun render() {
        val finalResult = result
        if (finalResult != null) {
            _state.update { it.copy(phase = LessonPhase.Finished(finalResult), isExitConfirmShown = false) }
            return
        }
        val current = run ?: return
        val step = currentStep() ?: return
        val stepLayout = layout ?: return
        val voice = hasVoice()
        val recorderUi = RecorderUi(
            microphone = microphone,
            isRecording = isRecording,
            hasRecording = recording != null,
            failed = recordingFailed,
            score = score,
            notice = notice
        )
        _state.update {
            it.copy(
                phase = LessonPhase.Playing(
                    stepNumber = current.position + 1,
                    stepCount = current.queue.size,
                    isAgain = current.isRetry,
                    hasVoice = voice,
                    showsNoVoiceNotice = showsNoVoiceNotice,
                    step = step.toStepUi(input, stepLayout, voice, recorderUi),
                    feedback = input.feedback,
                    canCheck = step.canCheck(input)
                )
            )
        }
    }

    companion object {
        private const val KEY_STARTED = "started"
        private const val KEY_TEXT = "text"
        private const val KEY_RESULT_MINUTES = "result_minutes"
        private const val KEY_RESULT_RATINGS = "result_ratings"
        private const val KEY_RESULT_SAVED = "result_saved"

        /** How long a new lesson waits for the speech engine before treating it as missing. */
        private val VOICE_WAIT = 3.seconds

        /** A shadowed sentence is short; this leaves room for a slow first try. */
        private val MAX_RECORDING = 10.seconds
    }
}

private fun SpeakerStatus.hasVoice(): Boolean =
    this is SpeakerStatus.Connected && voice(SpeechLanguage.AMERICAN_ENGLISH) == VoiceState.INSTALLED

private fun LessonStep.needsSound(): Boolean = this is LessonStep.MinimalPair || this is LessonStep.Dictation

private fun PermissionState.toMicrophoneUi(): MicrophoneUi = when (this) {
    PermissionState.GRANTED -> MicrophoneUi.GRANTED
    PermissionState.DENIED -> MicrophoneUi.DENIED
    PermissionState.PERMANENTLY_DENIED -> MicrophoneUi.PERMANENTLY_DENIED
    PermissionState.NOT_DETERMINED -> MicrophoneUi.NOT_ASKED
}

/** The first play follows the learner's speed; "play slowly" stays slow either way. */
private val AppSettings.firstPlayRate: SpeechRate
    get() = if (speed == SpeechSpeed.SLOW) SpeechRate.SLOW else SpeechRate.NORMAL
