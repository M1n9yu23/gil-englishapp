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
package com.gyugle.gil.feature.dictionary.impl.presentation.entry

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.dictionary.impl.presentation.TestEntries
import com.gyugle.gil.feature.dictionary.impl.presentation.component.LocalizedTextUi
import com.gyugle.gil.feature.progress.api.FakeProgressCommands
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ProgressObserver
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EntryViewModelTest {
    private val lexicon = FakeLexiconObserver(TestEntries.run, TestEntries.thankYou, TestEntries.f)
    private val progress = FakeProgressObserver()
    private val commands = FakeProgressCommands(progress)
    private val speaker = FakeSpeaker()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: String) = EntryViewModel(id, lexicon, progress, commands, speaker)

    private fun EntryViewModel.word() = state.value.entry as EntryUi.Word

    @Test
    fun init_wordMetInNoSense_showsItsFirstStageSenseNow() = runTest {
        val word = viewModel("word.run").word()

        assertThat(word.learnedAny).isEqualTo(false)
        assertThat(word.now.map { it.id }).containsExactly("sense.run.v.move-fast")
        assertThat(word.later.map { it.id }).containsExactly("sense.run.v.manage")
        assertThat(word.later.single().stage).isEqualTo(LocalizedTextUi("자립", "On your own"))
    }

    @Test
    fun init_learnedSense_isTheOneToKnowNow_andFollowsTheRecord() = runTest {
        progress.learn(TestEntries.manage.id)
        val viewModel = viewModel("word.run")

        assertThat(viewModel.word().learnedAny).isEqualTo(true)
        assertThat(viewModel.word().now.map { it.id }).containsExactly("sense.run.v.manage")

        progress.learn(TestEntries.moveFast.id)

        assertThat(viewModel.word().now.map { it.id }).containsExactly("sense.run.v.move-fast", "sense.run.v.manage")
    }

    @Test
    fun init_senseId_opensItsWordWithThatSenseNow() = runTest {
        progress.learn(TestEntries.moveFast.id)

        val word = viewModel("sense.run.v.manage").word()

        assertThat(word.id).isEqualTo("word.run")
        assertThat(word.now.map { it.id }).containsExactly("sense.run.v.move-fast", "sense.run.v.manage")
    }

    @Test
    fun init_unlearnedSenseId_keepsTheFirstStageSenseAndTitlesThemFirst() = runTest {
        val word = viewModel("sense.run.v.manage").word()

        assertThat(word.learnedAny).isEqualTo(false)
        assertThat(word.now.map { it.id }).containsExactly("sense.run.v.move-fast", "sense.run.v.manage")
    }

    @Test
    fun init_recordUnreadable_showsARetry() = runTest {
        val broken = object : ProgressObserver by progress {
            override fun observeSavedItems(): Flow<List<StudyItemId>> = flow { throw IOException("disk") }
        }

        val state = EntryViewModel("word.run", lexicon, broken, commands, speaker).state.value

        assertThat(state.error?.canRetry).isEqualTo(true)
    }

    @Test
    fun onSaveToggle_tappedTwiceBeforeTheRecordAnswers_endsNotSaved() = runTest {
        val slow = SlowCommands(commands)
        val viewModel = EntryViewModel("word.run", lexicon, progress, slow, speaker)

        viewModel.onAction(EntryAction.OnSaveToggle("sense.run.v.move-fast"))
        viewModel.onAction(EntryAction.OnSaveToggle("sense.run.v.move-fast"))
        assertThat(viewModel.word().now.single().saved).isEqualTo(false)
        slow.gate.complete(Unit)

        assertThat(commands.saved).containsExactly(TestEntries.moveFast.id to true, TestEntries.moveFast.id to false)
        assertThat(progress.savedItems.value).isEqualTo(emptyList())
        assertThat(viewModel.word().now.single().saved).isEqualTo(false)
    }

    /** Holds every save until [gate] completes, like a slow disk. */
    private class SlowCommands(private val inner: FakeProgressCommands) : ProgressCommands by inner {
        val gate = CompletableDeferred<Unit>()

        override suspend fun setSaved(itemId: StudyItemId, saved: Boolean): EmptyOutcome<DataError.Local> {
            gate.await()
            return inner.setSaved(itemId, saved)
        }
    }

    @Test
    fun init_patternAndSound_showTheirPages() = runTest {
        assertThat(viewModel("pattern.thank-you").state.value.entry).isNotNull().isInstanceOf(EntryUi.Pattern::class)
        assertThat(viewModel("sound.f").state.value.entry).isNotNull().isInstanceOf(EntryUi.Sound::class)
    }

    @Test
    fun init_idWithoutAPage_showsNotFoundWithoutRetry() = runTest {
        listOf("lesson.s1-01.1", "word.walk", "sense.walk.v.go", "").forEach { id ->
            val state = viewModel(id).state.value
            assertThat(state.entry, name = id).isNull()
            assertThat(state.error?.canRetry, name = id).isEqualTo(false)
        }
    }

    @Test
    fun onRetryClick_afterUnavailable_showsThePage() = runTest {
        lexicon.error = ContentError.UNAVAILABLE
        val viewModel = viewModel("word.run")
        assertThat(viewModel.state.value.error?.canRetry).isEqualTo(true)

        lexicon.error = null
        viewModel.onAction(EntryAction.OnRetryClick)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.word().text).isEqualTo("run")
    }

    @Test
    fun onSaveToggle_savesThenRemoves_andThePageFollows() = runTest {
        val viewModel = viewModel("word.run")

        viewModel.onAction(EntryAction.OnSaveToggle("sense.run.v.move-fast"))
        assertThat(viewModel.word().now.single().saved).isEqualTo(true)

        viewModel.onAction(EntryAction.OnSaveToggle("sense.run.v.move-fast"))
        assertThat(viewModel.word().now.single().saved).isEqualTo(false)
        assertThat(commands.saved).containsExactly(TestEntries.moveFast.id to true, TestEntries.moveFast.id to false)
    }

    @Test
    fun onSaveToggle_storageFails_showsAMessage() = runTest {
        commands.saveError = DataError.Local.DISK_FULL
        val viewModel = viewModel("pattern.thank-you")

        viewModel.events.test {
            viewModel.onAction(EntryAction.OnSaveToggle("pattern.thank-you"))

            assertThat(awaitItem()).isEqualTo(EntryEvent.ShowMessage(DataError.Local.DISK_FULL.toUiText()))
        }
        assertThat((viewModel.state.value.entry as EntryUi.Pattern).saved).isEqualTo(false)
    }

    @Test
    fun onPlayClick_speaksInTheChosenAccent() = runTest {
        val viewModel = viewModel("word.run")

        viewModel.onAction(EntryAction.OnPlayClick("run", Accent.BRITISH))
        viewModel.onAction(EntryAction.OnPlayClick("I run."))

        assertThat(speaker.spoken).containsExactly(
            FakeSpeaker.Utterance("run", SpeechLanguage.BRITISH_ENGLISH, SpeechRate.NORMAL),
            FakeSpeaker.Utterance("I run.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL)
        )
    }

    @Test
    fun voices_installedVoicesChange_showOnlyThePlayableAccents() = runTest {
        val viewModel = viewModel("word.run")
        assertThat(viewModel.state.value.voices).isEqualTo(VoicesUi(american = true, british = true))

        speaker.status.value = SpeakerStatus.Connected(
            mapOf(
                SpeechLanguage.AMERICAN_ENGLISH to VoiceState.INSTALLED,
                SpeechLanguage.BRITISH_ENGLISH to VoiceState.NOT_INSTALLED
            )
        )
        assertThat(viewModel.state.value.voices).isEqualTo(VoicesUi(american = true, british = false))

        speaker.status.value = SpeakerStatus.NoEngine
        assertThat(viewModel.state.value.voices.any).isEqualTo(false)
    }
}
