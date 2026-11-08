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
package com.gyugle.gil.feature.settings.impl.presentation.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.RowDivider
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.component.TimePickerDialog
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.core.presentation.permissions.Permission
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.presentation.permissions.rememberPermissionController
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.impl.presentation.R
import com.gyugle.gil.feature.settings.impl.presentation.component.label

private object RemindersTokens {
    /** The screen margin less a text button's 12dp inner padding. */
    val TextButtonStart = 8.dp
}

@Composable
fun RemindersRoot(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberPermissionController()
    val context = LocalContext.current

    // Below Android 13 nothing is asked, but the app's notifications can still be turned off.
    fun notificationState(): PermissionState {
        val granted = permissions.getPermissionState(Permission.NOTIFICATIONS)
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return if (granted == PermissionState.GRANTED && !enabled) PermissionState.PERMANENTLY_DENIED else granted
    }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RemindersEvent.RequestPermission -> {
                permissions.requestPermission(Permission.NOTIFICATIONS)
                viewModel.onAction(RemindersAction.OnNotificationsChecked(notificationState()))
            }

            RemindersEvent.OpenAppSettings -> permissions.openAppSettings()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(RemindersAction.OnNotificationsChecked(notificationState()))
    }
    RemindersScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun RemindersScreen(
    state: RemindersState,
    onAction: (RemindersAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.settings_reminders), onNavigateBack = onBack)
        if (state.times == null) LoadingState() else RemindersContent(state, onAction)
    }
    val editing = state.editing
    if (editing != null && state.times != null) {
        val initial = state.times.getOrNull(editing) ?: suggestionFor(state.times)
        TimePickerDialog(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            title = stringResource(R.string.settings_reminders_picker_title),
            confirmLabel = stringResource(R.string.settings_ok),
            dismissLabel = stringResource(R.string.settings_cancel),
            onPick = { hour, minute -> onAction(RemindersAction.OnTimePick(ReminderTime(hour, minute))) },
            onDismiss = { onAction(RemindersAction.OnPickerDismiss) }
        )
    }
}

@Composable
private fun RemindersContent(state: RemindersState, onAction: (RemindersAction) -> Unit) {
    val times = state.times.orEmpty()
    val spacing = GilTheme.spacing
    val margin = Modifier.padding(horizontal = spacing.screenMargin)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        BodyText(stringResource(R.string.settings_reminders_body), margin)
        Column {
            if (times.isEmpty()) {
                ListRow(headline = stringResource(R.string.settings_reminders_none))
            }
            times.forEachIndexed { index, time ->
                if (index > 0) RowDivider()
                TimeRow(time, onAction, index)
            }
            if (state.canAddTime) {
                TextActionButton(
                    text = stringResource(R.string.settings_reminders_add),
                    onClick = { onAction(RemindersAction.OnAddClick) },
                    modifier = Modifier.padding(start = RemindersTokens.TextButtonStart)
                )
            }
        }
        state.error?.let { error ->
            Text(
                text = error.asString(),
                modifier = margin.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (state.isBlocked) BlockedNotice(state.notifications, onAction, modifier = margin)
    }
}

@Composable
private fun TimeRow(time: ReminderTime, onAction: (RemindersAction) -> Unit, index: Int) {
    val text = time.label()
    ListRow(
        headline = text,
        onClick = { onAction(RemindersAction.OnTimeClick(index)) },
        clickLabel = stringResource(R.string.settings_reminders_change),
        trailing = {
            IconButton(onClick = { onAction(RemindersAction.OnRemoveClick(index)) }) {
                Icon(
                    painter = GilIcons.Close,
                    contentDescription = stringResource(R.string.settings_reminders_remove, text)
                )
            }
        }
    )
}

/** What to do so that the saved times show, shown only when they would not. */
@Composable
private fun BlockedNotice(
    notifications: PermissionState?,
    onAction: (RemindersAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = GilTheme.spacing
    val inSettings = notifications == PermissionState.PERMANENTLY_DENIED
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.itemGap)
            .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)
    ) {
        Text(
            text = stringResource(R.string.settings_reminders_blocked_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        val body = if (inSettings) {
            R.string.settings_reminders_blocked_settings_body
        } else {
            R.string.settings_reminders_blocked_body
        }
        BodyText(stringResource(body))
        SecondaryActionButton(
            text = stringResource(
                if (inSettings) R.string.settings_reminders_open_settings else R.string.settings_reminders_allow
            ),
            onClick = { onAction(RemindersAction.OnAllowClick) },
            modifier = Modifier.padding(top = spacing.inlineGap)
        )
    }
}

@Composable
private fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Evening first, the way to work second: the same suggestions as the first run. */
private fun suggestionFor(times: List<ReminderTime>): ReminderTime =
    listOf(ReminderTime(20, 0), ReminderTime(8, 0)).firstOrNull { it !in times } ?: ReminderTime(12, 0)

@PreviewLightDark
@Composable
private fun RemindersScreenPreview() {
    GilTheme {
        Surface {
            RemindersScreen(
                state = RemindersState(times = listOf(ReminderTime(20, 0)), notifications = PermissionState.DENIED),
                onAction = {},
                onBack = {}
            )
        }
    }
}
