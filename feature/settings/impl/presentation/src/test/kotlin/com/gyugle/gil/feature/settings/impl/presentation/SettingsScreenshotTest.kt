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
package com.gyugle.gil.feature.settings.impl.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview
import com.gyugle.gil.feature.settings.impl.presentation.backup.BackupScreen
import com.gyugle.gil.feature.settings.impl.presentation.backup.BackupState
import com.gyugle.gil.feature.settings.impl.presentation.backup.BackupStep
import com.gyugle.gil.feature.settings.impl.presentation.home.PreviewNavigation
import com.gyugle.gil.feature.settings.impl.presentation.home.SettingsDialog
import com.gyugle.gil.feature.settings.impl.presentation.home.SettingsHomeScreen
import com.gyugle.gil.feature.settings.impl.presentation.home.SettingsHomeState
import com.gyugle.gil.feature.settings.impl.presentation.home.SettingsValues
import com.gyugle.gil.feature.settings.impl.presentation.licenses.LicenseScreen
import com.gyugle.gil.feature.settings.impl.presentation.licenses.LicensesScreen
import com.gyugle.gil.feature.settings.impl.presentation.licenses.LicensesState
import com.gyugle.gil.feature.settings.impl.presentation.licenses.PreviewLibraries
import com.gyugle.gil.feature.settings.impl.presentation.privacy.PrivacyScreen
import com.gyugle.gil.feature.settings.impl.presentation.reminders.RemindersScreen
import com.gyugle.gil.feature.settings.impl.presentation.reminders.RemindersState
import com.gyugle.gil.feature.settings.impl.presentation.sound.SoundScreen
import com.gyugle.gil.feature.settings.impl.presentation.sound.SoundState
import com.gyugle.gil.feature.settings.impl.presentation.sound.VoicesUi
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The tall captures show the whole page, which a phone scrolls through. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class SettingsScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private val values = SettingsValues(
        goal = DailyGoal.TWENTY,
        reminders = listOf(ReminderTime(8, 0), ReminderTime(20, 0)),
        settings = AppSettings()
    )
    private val preview = BackupPreview(Instant.parse("2026-11-01T09:00:00Z"), 128, 23, StudyDay(20758))

    private fun capture(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { Surface { content() } } }
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    private fun home(state: SettingsHomeState = SettingsHomeState(values)) = capture {
        SettingsHomeScreen(state = state, onAction = {}, navigation = PreviewNavigation, versionName = "0.2.0")
    }

    @Test fun home_loading() = home(SettingsHomeState())

    @Test
    @Config(qualifiers = "ko-w360dp-h1100dp-xhdpi")
    fun home_wholePage() = home()

    @Test
    @Config(qualifiers = "ko-w360dp-h1100dp-night-xhdpi")
    fun home_wholePageNight() = home()

    @Test
    @Config(qualifiers = "en-w360dp-h1100dp-xhdpi")
    fun home_wholePageEnglish() = home(SettingsHomeState(values.copy(reminders = emptyList())))

    @Test
    @Config(qualifiers = "ko-w360dp-h2200dp-xhdpi", fontScale = 2f)
    fun home_wholePageFontScale2() = home()

    @Test
    @Config(qualifiers = "en-w320dp-h2400dp-xhdpi", fontScale = 2f)
    fun home_narrowEnglishFontScale2() = home()

    @Test
    fun home_goalDialog() {
        composeRule.setContent {
            GilTheme {
                Surface {
                    SettingsHomeScreen(
                        state = SettingsHomeState(values, dialog = SettingsDialog.Goal),
                        onAction = {},
                        navigation = PreviewNavigation,
                        versionName = "0.2.0"
                    )
                }
            }
        }
        captureScreenRoboImage()
    }

    @Test
    fun home_clearAgainDialog() {
        composeRule.setContent {
            GilTheme {
                Surface {
                    SettingsHomeScreen(
                        state = SettingsHomeState(values, dialog = SettingsDialog.ClearAgain),
                        onAction = {},
                        navigation = PreviewNavigation,
                        versionName = "0.2.0"
                    )
                }
            }
        }
        captureScreenRoboImage()
    }

    @Test
    fun reminders_twoTimes() = capture {
        RemindersScreen(
            state = RemindersState(times = values.reminders, notifications = PermissionState.GRANTED),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    fun reminders_blocked() = capture {
        RemindersScreen(
            state = RemindersState(
                times = listOf(ReminderTime(20, 0)),
                notifications = PermissionState.PERMANENTLY_DENIED
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi")
    fun reminders_noneEnglish() = capture {
        RemindersScreen(state = RemindersState(times = emptyList()), onAction = {}, onBack = {})
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h1000dp-xhdpi")
    fun sound_britishMissing() = capture {
        SoundScreen(
            state = SoundState(
                accent = Accent.AMERICAN,
                voices = VoicesUi.Known(VoiceState.INSTALLED, VoiceState.NOT_INSTALLED)
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h1000dp-night-xhdpi")
    fun sound_britishChosenAndMissingNight() = capture {
        SoundScreen(
            state = SoundState(
                accent = Accent.BRITISH,
                voices = VoicesUi.Known(VoiceState.INSTALLED, VoiceState.NOT_INSTALLED)
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    @Config(qualifiers = "en-w360dp-h1000dp-xhdpi")
    fun sound_noEngineEnglish() = capture {
        SoundScreen(state = SoundState(accent = Accent.AMERICAN, voices = VoicesUi.NoEngine), onAction = {
        }, onBack = {})
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h1000dp-xhdpi")
    fun backup_choices() = capture { BackupScreen(state = BackupState.Idle, onAction = {}, onBack = {}) }

    @Test
    fun backup_exporting() = capture { BackupScreen(state = BackupState.Exporting, onAction = {}, onBack = {}) }

    @Test
    fun backup_summary() = capture { BackupScreen(state = BackupState.Preview(preview), onAction = {}, onBack = {}) }

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi")
    fun backup_summaryEnglish() = capture {
        BackupScreen(state = BackupState.Preview(preview), onAction = {}, onBack = {})
    }

    @Test
    fun backup_restoring() = capture {
        BackupScreen(state = BackupState.Restoring(preview), onAction = {}, onBack = {})
    }

    @Test
    fun backup_restored() = capture { BackupScreen(state = BackupState.Restored, onAction = {}, onBack = {}) }

    @Test
    fun backup_restoreFailed() = capture {
        BackupScreen(
            state = BackupState.Failed(
                BackupStep.RESTORE,
                UiText.StringResource(R.string.settings_error_disk_full),
                unchanged = true
            ),
            onAction = {},
            onBack = {}
        )
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h1600dp-xhdpi", fontScale = 2f)
    fun backup_summaryFontScale2() = capture {
        BackupScreen(state = BackupState.Preview(preview), onAction = {}, onBack = {})
    }

    @Test
    fun licenses_list() = capture {
        LicensesScreen(LicensesState.Loaded(PreviewLibraries), onBack = {}, onOpenLibrary = {}, onRetry = {})
    }

    @Test
    fun license_page() = capture {
        LicenseScreen(LicensesState.Loaded(PreviewLibraries), PreviewLibraries.first().id, onBack = {}, onRetry = {})
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h1800dp-xhdpi")
    fun privacy_wholePage() = capture { PrivacyScreen(onBack = {}) }

    @Test
    @Config(qualifiers = "en-w360dp-h2000dp-xhdpi")
    fun privacy_wholePageEnglish() = capture { PrivacyScreen(onBack = {}) }

    private companion object {
        const val SETTLED_MILLIS = 400L
    }
}
