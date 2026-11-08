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

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.feature.onboarding.api.StudyMethodRoute
import com.gyugle.gil.feature.settings.api.SettingsBackupRoute
import com.gyugle.gil.feature.settings.api.SettingsHomeRoute
import com.gyugle.gil.feature.settings.api.SettingsLicenseRoute
import com.gyugle.gil.feature.settings.api.SettingsLicensesRoute
import com.gyugle.gil.feature.settings.api.SettingsPrivacyRoute
import com.gyugle.gil.feature.settings.api.SettingsRemindersRoute
import com.gyugle.gil.feature.settings.api.SettingsSoundRoute
import com.gyugle.gil.feature.settings.impl.presentation.backup.BackupRoot
import com.gyugle.gil.feature.settings.impl.presentation.home.SettingsHomeNavigation
import com.gyugle.gil.feature.settings.impl.presentation.home.SettingsHomeRoot
import com.gyugle.gil.feature.settings.impl.presentation.licenses.LicenseRoot
import com.gyugle.gil.feature.settings.impl.presentation.licenses.LicensesRoot
import com.gyugle.gil.feature.settings.impl.presentation.privacy.PrivacyScreen
import com.gyugle.gil.feature.settings.impl.presentation.reminders.RemindersRoot
import com.gyugle.gil.feature.settings.impl.presentation.sound.SoundRoot

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsHomeRoute> {
        SettingsHomeRoot(
            navigation = SettingsHomeNavigation(
                onBack = navigator::goBack,
                onOpenReminders = { navigator.goTo(SettingsRemindersRoute) },
                onOpenSound = { navigator.goTo(SettingsSoundRoute) },
                onOpenBackup = { navigator.goTo(SettingsBackupRoute) },
                onOpenMethod = { navigator.goTo(StudyMethodRoute) },
                onOpenLicenses = { navigator.goTo(SettingsLicensesRoute) },
                onOpenPrivacy = { navigator.goTo(SettingsPrivacyRoute) }
            )
        )
    }
    entry<SettingsRemindersRoute> { RemindersRoot(onBack = navigator::goBack) }
    entry<SettingsSoundRoute> { SoundRoot(onBack = navigator::goBack) }
    entry<SettingsBackupRoute> { BackupRoot(onBack = navigator::goBack) }
    entry<SettingsLicensesRoute> {
        LicensesRoot(onBack = navigator::goBack, onOpenLibrary = { id -> navigator.goTo(SettingsLicenseRoute(id)) })
    }
    entry<SettingsLicenseRoute> { route -> LicenseRoot(libraryId = route.libraryId, onBack = navigator::goBack) }
    entry<SettingsPrivacyRoute> { PrivacyScreen(onBack = navigator::goBack) }
}
