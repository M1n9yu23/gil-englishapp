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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.ChoiceDialog
import com.gyugle.gil.core.designsystem.component.ChoiceRow
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.RowDivider
import com.gyugle.gil.core.designsystem.component.SwitchRow
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.ThemeMode
import com.gyugle.gil.feature.settings.impl.domain.model.AppLanguage
import com.gyugle.gil.feature.settings.impl.presentation.R
import com.gyugle.gil.feature.settings.impl.presentation.component.LinkRow
import com.gyugle.gil.feature.settings.impl.presentation.component.SettingsSection
import com.gyugle.gil.feature.settings.impl.presentation.component.ValueRow
import com.gyugle.gil.feature.settings.impl.presentation.component.label
import com.gyugle.gil.feature.settings.impl.presentation.component.summary

/** Where the rows of the list lead. */
data class SettingsHomeNavigation(
    val onBack: () -> Unit,
    val onOpenReminders: () -> Unit,
    val onOpenSound: () -> Unit,
    val onOpenBackup: () -> Unit,
    val onOpenMethod: () -> Unit,
    val onOpenLicenses: () -> Unit,
    val onOpenPrivacy: () -> Unit
)

@Composable
fun SettingsHomeRoot(navigation: SettingsHomeNavigation, viewModel: SettingsHomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(SettingsHomeAction.OnResumed) }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsHomeEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.resolve(context))
        }
    }
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    SettingsHomeScreen(
        state = state,
        onAction = viewModel::onAction,
        navigation = navigation,
        versionName = versionName,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun SettingsHomeScreen(
    state: SettingsHomeState,
    onAction: (SettingsHomeAction) -> Unit,
    navigation: SettingsHomeNavigation,
    versionName: String,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GilTopBar(title = stringResource(R.string.settings_title), onNavigateBack = navigation.onBack)
            val values = state.values
            if (values ==
                null
            ) {
                LoadingState()
            } else {
                SettingsList(values, state.language, state.isClearing, onAction, navigation, versionName)
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
    state.dialog?.let { dialog -> SettingsDialogs(dialog, state, onAction) }
}

@Composable
private fun SettingsList(
    values: SettingsValues,
    language: AppLanguage?,
    isClearing: Boolean,
    onAction: (SettingsHomeAction) -> Unit,
    navigation: SettingsHomeNavigation,
    versionName: String
) {
    val spacing = GilTheme.spacing
    val settings = values.settings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        SettingsSection(stringResource(R.string.settings_section_study)) {
            ValueRow(stringResource(R.string.settings_goal), values.goal.label()) {
                onAction(SettingsHomeAction.OnGoalClick)
            }
            RowDivider()
            LinkRow(
                headline = stringResource(R.string.settings_reminders),
                supporting = values.reminders.summary(),
                onClick = navigation.onOpenReminders
            )
        }
        SettingsSection(stringResource(R.string.settings_section_sound_display)) {
            LinkRow(
                headline = stringResource(R.string.settings_sound),
                supporting = stringResource(
                    R.string.settings_sound_summary,
                    settings.accent.label(),
                    settings.speed.label()
                ),
                onClick = navigation.onOpenSound
            )
            RowDivider()
            SwitchRow(
                headline = stringResource(R.string.settings_hangul_aid),
                supporting = stringResource(R.string.settings_hangul_aid_detail),
                checked = settings.showHangulAid,
                onCheckedChange = { onAction(SettingsHomeAction.OnHangulAidChange(it)) }
            )
            RowDivider()
            ValueRow(stringResource(R.string.settings_theme), settings.theme.label()) {
                onAction(SettingsHomeAction.OnThemeClick)
            }
            if (language != null) {
                RowDivider()
                ValueRow(stringResource(R.string.settings_language), language.label()) {
                    onAction(SettingsHomeAction.OnLanguageClick)
                }
            }
        }
        SettingsSection(stringResource(R.string.settings_section_record)) {
            LinkRow(
                headline = stringResource(R.string.settings_backup),
                supporting = stringResource(R.string.settings_backup_summary),
                onClick = navigation.onOpenBackup
            )
            RowDivider()
            ListRow(
                headline = stringResource(R.string.settings_clear),
                onClick = if (isClearing) null else ({ onAction(SettingsHomeAction.OnClearClick) })
            )
        }
        SettingsSection(stringResource(R.string.settings_section_about)) {
            LinkRow(stringResource(R.string.settings_method), onClick = navigation.onOpenMethod)
            RowDivider()
            LinkRow(stringResource(R.string.settings_licenses), onClick = navigation.onOpenLicenses)
            RowDivider()
            LinkRow(stringResource(R.string.settings_privacy), onClick = navigation.onOpenPrivacy)
            RowDivider()
            ListRow(headline = stringResource(R.string.settings_version), supporting = versionName)
        }
    }
}

@Composable
private fun SettingsDialogs(dialog: SettingsDialog, state: SettingsHomeState, onAction: (SettingsHomeAction) -> Unit) {
    val dismiss = { onAction(SettingsHomeAction.OnDialogDismiss) }
    val cancel = stringResource(R.string.settings_cancel)
    when (dialog) {
        SettingsDialog.Goal -> ChoiceDialog(stringResource(R.string.settings_goal), cancel, dismiss) {
            DailyGoal.entries.forEach { goal ->
                ChoiceRow(
                    label = goal.label(),
                    selected = goal == state.values?.goal,
                    onSelect = { onAction(SettingsHomeAction.OnGoalPick(goal)) },
                    horizontalPadding = 0.dp
                )
            }
        }

        SettingsDialog.Theme -> ChoiceDialog(stringResource(R.string.settings_theme), cancel, dismiss) {
            ThemeMode.entries.forEach { theme ->
                ChoiceRow(
                    label = theme.label(),
                    selected = theme == state.values?.settings?.theme,
                    onSelect = { onAction(SettingsHomeAction.OnThemePick(theme)) },
                    horizontalPadding = 0.dp
                )
            }
        }

        SettingsDialog.Language -> ChoiceDialog(stringResource(R.string.settings_language), cancel, dismiss) {
            AppLanguage.entries.forEach { language ->
                ChoiceRow(
                    label = language.label(),
                    selected = language == state.language,
                    onSelect = { onAction(SettingsHomeAction.OnLanguagePick(language)) },
                    horizontalPadding = 0.dp
                )
            }
        }

        SettingsDialog.Clear -> ConfirmDialog(
            title = stringResource(R.string.settings_clear_title),
            body = stringResource(R.string.settings_clear_body),
            confirmLabel = stringResource(R.string.settings_clear_next),
            onConfirm = { onAction(SettingsHomeAction.OnClearNextClick) },
            onDismiss = dismiss
        )

        SettingsDialog.ClearAgain -> ConfirmDialog(
            title = stringResource(R.string.settings_clear_again_title),
            body = stringResource(R.string.settings_clear_again_body),
            confirmLabel = stringResource(R.string.settings_clear_confirm),
            onConfirm = { onAction(SettingsHomeAction.OnClearConfirmClick) },
            onDismiss = dismiss
        )
    }
}

/** Cancel comes last, where the thumb rests, so the erasing choice is never the easy one. */
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextActionButton(text = stringResource(R.string.settings_cancel), onClick = onDismiss) },
        dismissButton = { TextActionButton(text = confirmLabel, onClick = onConfirm) }
    )
}

internal val PreviewNavigation = SettingsHomeNavigation({}, {}, {}, {}, {}, {}, {})

@PreviewLightDark
@Composable
private fun SettingsHomeScreenPreview() {
    GilTheme {
        Surface {
            SettingsHomeScreen(
                state = SettingsHomeState(
                    SettingsValues(DailyGoal.TWENTY, listOf(ReminderTime(8, 0), ReminderTime(20, 0)), AppSettings())
                ),
                onAction = {},
                navigation = PreviewNavigation,
                versionName = "0.2.0"
            )
        }
    }
}
