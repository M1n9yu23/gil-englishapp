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
package com.gyugle.gil.feature.settings.impl.presentation.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.FakeGoalRepository
import com.gyugle.gil.feature.progress.api.FakeProgressBackup
import com.gyugle.gil.feature.reminder.api.FakeReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.ThemeMode
import com.gyugle.gil.feature.settings.impl.domain.model.AppLanguage
import com.gyugle.gil.feature.settings.impl.domain.repository.FakeAppLanguageRepository
import com.gyugle.gil.feature.settings.impl.domain.repository.FakeSettingsRepository
import com.gyugle.gil.feature.settings.impl.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsHomeViewModelTest {
    private val goal = FakeGoalRepository(DailyGoal.THIRTY)
    private val reminders = FakeReminderCommands().apply { times.value = listOf(ReminderTime(20, 0)) }
    private val settings = FakeSettingsRepository()
    private val progress = FakeProgressBackup(record = RECORD)
    private var language = FakeAppLanguageRepository()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsHomeViewModel(goal, reminders, goal, settings, language, progress)

    @Test
    fun init_showsTheGoalRemindersAndSettings() {
        assertThat(viewModel().state.value.values)
            .isEqualTo(SettingsValues(DailyGoal.THIRTY, listOf(ReminderTime(20, 0)), AppSettings()))
    }

    @Test
    fun goal_pickedInTheDialog_isSavedAndClosesIt() {
        val viewModel = viewModel()
        viewModel.onAction(SettingsHomeAction.OnGoalClick)
        assertThat(viewModel.state.value.dialog).isEqualTo(SettingsDialog.Goal)

        viewModel.onAction(SettingsHomeAction.OnGoalPick(DailyGoal.TEN))

        assertThat(goal.goal.value).isEqualTo(DailyGoal.TEN)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.values?.goal).isEqualTo(DailyGoal.TEN)
    }

    @Test
    fun theme_andHangulAid_areSaved() {
        val viewModel = viewModel()

        viewModel.onAction(SettingsHomeAction.OnThemePick(ThemeMode.DARK))
        viewModel.onAction(SettingsHomeAction.OnHangulAidChange(false))

        assertThat(settings.settings.value).isEqualTo(AppSettings(theme = ThemeMode.DARK, showHangulAid = false))
    }

    @Test
    fun saveFails_saysSo() = runTest {
        settings.error = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsHomeAction.OnThemePick(ThemeMode.DARK))
            assertThat(awaitItem()).isEqualTo(SettingsHomeEvent.ShowMessage(DataError.Local.DISK_FULL.toUiText()))
        }
    }

    @Test
    fun clear_asksTwiceBeforeErasing() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsHomeAction.OnClearClick)
            assertThat(viewModel.state.value.dialog).isEqualTo(SettingsDialog.Clear)
            viewModel.onAction(SettingsHomeAction.OnClearNextClick)
            assertThat(viewModel.state.value.dialog).isEqualTo(SettingsDialog.ClearAgain)
            assertThat(progress.record).isEqualTo(RECORD)

            viewModel.onAction(SettingsHomeAction.OnClearConfirmClick)

            assertThat(progress.record).isEqualTo(FakeProgressBackup.EMPTY)
            assertThat(viewModel.state.value.dialog).isNull()
            assertThat(
                awaitItem()
            ).isEqualTo(SettingsHomeEvent.ShowMessage(UiText.StringResource(R.string.settings_cleared)))
        }
    }

    @Test
    fun clear_dismissedAtTheSecondQuestion_keepsTheRecord() {
        val viewModel = viewModel()
        viewModel.onAction(SettingsHomeAction.OnClearClick)
        viewModel.onAction(SettingsHomeAction.OnClearNextClick)

        viewModel.onAction(SettingsHomeAction.OnDialogDismiss)

        assertThat(progress.record).isEqualTo(RECORD)
        assertThat(viewModel.state.value.dialog).isNull()
    }

    @Test
    fun clear_fails_saysSoAndKeepsTheRecord() = runTest {
        progress.clearError = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsHomeAction.OnClearConfirmClick)
            assertThat(awaitItem()).isEqualTo(SettingsHomeEvent.ShowMessage(DataError.Local.UNKNOWN.toUiText()))
        }
        assertThat(progress.record).isEqualTo(RECORD)
        assertThat(viewModel.state.value.isClearing).isEqualTo(false)
    }

    @Test
    fun language_belowAndroid13_hasNoRow() {
        language = FakeAppLanguageRepository(canChoose = false)

        assertThat(viewModel().state.value.language).isNull()
    }

    @Test
    fun language_nothingChosen_followsThePhone() {
        assertThat(viewModel().state.value.language).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun language_pickedInTheDialog_isSetAndClosesIt() {
        val viewModel = viewModel()
        viewModel.onAction(SettingsHomeAction.OnLanguageClick)
        assertThat(viewModel.state.value.dialog).isEqualTo(SettingsDialog.Language)

        viewModel.onAction(SettingsHomeAction.OnLanguagePick(AppLanguage.ENGLISH))

        assertThat(language.language).isEqualTo(AppLanguage.ENGLISH)
        assertThat(viewModel.state.value.dialog).isNull()
        assertThat(viewModel.state.value.language).isEqualTo(AppLanguage.ENGLISH)
    }

    @Test
    fun language_changedInThePhoneSettings_isShownOnReturn() {
        val viewModel = viewModel()
        language.language = AppLanguage.KOREAN
        assertThat(viewModel.state.value.language).isEqualTo(AppLanguage.SYSTEM)

        viewModel.onAction(SettingsHomeAction.OnResumed)

        assertThat(viewModel.state.value.language).isEqualTo(AppLanguage.KOREAN)
    }

    private companion object {
        const val RECORD = """{"version":1,"cards":["..."]}"""
    }
}
