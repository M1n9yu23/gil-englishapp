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
package com.gyugle.gil.feature.onboarding.impl.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.onboarding.impl.presentation.goal.GoalStepScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.goal.GoalStepState
import com.gyugle.gil.feature.onboarding.impl.presentation.method.MethodScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.reminder.ReminderStage
import com.gyugle.gil.feature.onboarding.impl.presentation.reminder.ReminderStepScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.reminder.ReminderStepState
import com.gyugle.gil.feature.onboarding.impl.presentation.sound.SoundCheckPhase
import com.gyugle.gil.feature.onboarding.impl.presentation.sound.SoundCheckScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.sound.SoundCheckState
import com.gyugle.gil.feature.onboarding.impl.presentation.welcome.WelcomeScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.welcome.WelcomeState
import com.gyugle.gil.feature.reminder.api.ReminderTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class OnboardingScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private val twoTimes = ReminderStepState(times = listOf(ReminderTime(8, 0), ReminderTime(20, 0)))

    private fun welcome(state: WelcomeState = WelcomeState()) =
        composeRule.captureScreen { WelcomeScreen(state = state, onAction = {}, onStart = {}) }

    private fun method(page: Int) = composeRule.captureScreen { MethodScreen(page = page, onNext = {}, onBack = {}) }

    private fun goal(state: GoalStepState = GoalStepState()) =
        composeRule.captureScreen { GoalStepScreen(state = state, onAction = {}, onBack = {}) }

    private fun reminder(state: ReminderStepState) =
        composeRule.captureScreen { ReminderStepScreen(state = state, onAction = {}, onBack = {}) }

    private fun sound(phase: SoundCheckPhase) = composeRule.captureScreen {
        SoundCheckScreen(state = SoundCheckState(phase = phase), onAction = {}, onBack = {})
    }

    @Test fun welcome_light_offersStartAndSkip() = welcome()

    @Test
    @Config(qualifiers = "+night")
    fun welcome_night_usesNightPaper() = welcome()

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun welcome_englishFontScale2_keepsButtonsWhole() = welcome()

    @Test
    fun welcome_saveFailed_showsWhy() = welcome(WelcomeState(error = DataError.Local.DISK_FULL.toUiText()))

    @Test fun method_firstPage_showsStepOneOfSeven() = method(page = 0)

    @Test fun method_lastPage_mistakesAreFine() = method(page = 3)

    @Test
    @Config(qualifiers = "+night")
    fun method_night_usesNightPaper() = method(page = 1)

    @Test fun goal_light_twentyMinutesSelected() = goal()

    @Test
    @Config(qualifiers = "+night")
    fun goal_night_usesNightPaper() = goal(GoalStepState(selected = DailyGoal.THIRTY))

    @Test
    @Config(qualifiers = "ko-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun goal_fontScale2_scrollsAboveTheButton() = goal()

    @Test fun reminder_oneTime_offersToAddAnother() = reminder(ReminderStepState())

    @Test fun reminder_twoTimes_noMoreToAdd() = reminder(twoTimes)

    @Test
    @Config(qualifiers = "+night")
    fun reminder_night_usesNightPaper() = reminder(twoTimes)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun reminder_englishFontScale2_keepsRowsWhole() = reminder(twoTimes)

    @Test fun reminder_noTimes_cannotTurnOn() = reminder(ReminderStepState(times = emptyList()))

    @Test fun reminder_rationale_explainsThePermission() = reminder(twoTimes.copy(stage = ReminderStage.Rationale))

    @Test
    fun reminder_denied_goesOnWithout() = reminder(twoTimes.copy(stage = ReminderStage.Denied(permanently = false)))

    @Test
    fun reminder_blocked_offersSettings() = reminder(twoTimes.copy(stage = ReminderStage.Denied(permanently = true)))

    @Test fun sound_checking_waitsForTheVoice() = sound(SoundCheckPhase.Checking)

    @Test fun sound_ready_showsTheWord() = sound(SoundCheckPhase.Ready)

    @Test
    @Config(qualifiers = "+night")
    fun sound_night_usesNightPaper() = sound(SoundCheckPhase.Ready)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun sound_englishFontScale2_keepsButtonsWhole() = sound(SoundCheckPhase.Ready)

    @Test fun sound_notHeard_offersHelp() = sound(SoundCheckPhase.NotHeard)

    @Test fun sound_noVoice_offersTheInstall() = sound(SoundCheckPhase.NoVoice)

    @Test fun sound_noEngine_goesOnWithText() = sound(SoundCheckPhase.NoEngine)
}
