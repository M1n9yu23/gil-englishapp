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
package com.gyugle.gil.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SettingsRoute : NavKey

/** Every setting in one list, opened from the Today tab. */
@Serializable
@SerialName("settings.home")
data object SettingsHomeRoute : SettingsRoute

@Serializable
@SerialName("settings.reminders")
data object SettingsRemindersRoute : SettingsRoute

/** The accent, the speed and whether the phone has the voices. */
@Serializable
@SerialName("settings.sound")
data object SettingsSoundRoute : SettingsRoute

/** Saving the record to a file and reading it back. */
@Serializable
@SerialName("settings.backup")
data object SettingsBackupRoute : SettingsRoute

@Serializable
@SerialName("settings.licenses")
data object SettingsLicensesRoute : SettingsRoute

/** The full license of one library, [libraryId] as the license list names it. */
@Serializable
@SerialName("settings.license")
data class SettingsLicenseRoute(val libraryId: String) : SettingsRoute

@Serializable
@SerialName("settings.privacy")
data object SettingsPrivacyRoute : SettingsRoute
