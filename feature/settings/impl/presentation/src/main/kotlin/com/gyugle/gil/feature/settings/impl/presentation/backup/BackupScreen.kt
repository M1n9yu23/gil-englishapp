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
package com.gyugle.gil.feature.settings.impl.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.designsystem.theme.tabularFigures
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview
import com.gyugle.gil.feature.settings.impl.presentation.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private object BackupTokens {
    val FigureRowMinHeight = 48.dp
}

/** Backup files are JSON; some file apps call them plain text or bytes, so those can be picked too. */
private val ImportTypes = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
fun BackupRoot(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val createFile =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            viewModel.onAction(BackupAction.OnExportPicked(uri?.toString()))
        }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onAction(BackupAction.OnImportPicked(uri?.toString()))
    }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is BackupEvent.PickExportFile -> createFile.launch(event.fileName)
            BackupEvent.PickImportFile -> openFile.launch(ImportTypes)
            is BackupEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.resolve(context))
        }
    }
    BackupScreen(state = state, onAction = viewModel::onAction, onBack = onBack, snackbarHostState = snackbarHostState)
}

@Composable
fun BackupScreen(
    state: BackupState,
    onAction: (BackupAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GilTopBar(title = stringResource(R.string.settings_backup), onNavigateBack = onBack)
            val fade = GilTheme.motion.fade
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) },
                // Exporting and reading keep the same page, so only a new page fades.
                contentKey = { it.page() },
                label = "backup"
            ) { shown ->
                when (shown) {
                    BackupState.Idle, BackupState.Exporting, BackupState.Reading -> Choices(shown, onAction)

                    is BackupState.Preview -> Summary(shown.preview, isRestoring = false, onAction)

                    is BackupState.Restoring -> Summary(shown.preview, isRestoring = true, onAction)

                    BackupState.Restored -> Result(
                        title = stringResource(R.string.settings_restored_title),
                        body = stringResource(R.string.settings_restored_body),
                        onDone = { onAction(BackupAction.OnDoneClick) }
                    )

                    is BackupState.Failed -> Result(
                        title = stringResource(shown.step.title()),
                        body = shown.reason.asString() +
                            if (shown.unchanged) " " + stringResource(R.string.settings_error_unchanged) else "",
                        onDone = { onAction(BackupAction.OnDoneClick) }
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

private enum class Page { CHOICES, SUMMARY, RESULT }

private fun BackupState.page(): Page = when (this) {
    BackupState.Idle, BackupState.Exporting, BackupState.Reading -> Page.CHOICES
    is BackupState.Preview, is BackupState.Restoring -> Page.SUMMARY
    BackupState.Restored, is BackupState.Failed -> Page.RESULT
}

private fun BackupStep.title(): Int = when (this) {
    BackupStep.EXPORT -> R.string.settings_export_failed_title
    BackupStep.READ -> R.string.settings_read_failed_title
    BackupStep.RESTORE -> R.string.settings_restore_failed_title
}

@Composable
private fun Choices(state: BackupState, onAction: (BackupAction) -> Unit) {
    val busy = state != BackupState.Idle
    Page {
        BodyText(stringResource(R.string.settings_backup_body))
        Choice(
            title = stringResource(R.string.settings_export_title),
            body = stringResource(R.string.settings_export_body),
            button = stringResource(R.string.settings_export_button),
            onClick = { onAction(BackupAction.OnExportClick) },
            enabled = !busy,
            status = if (state == BackupState.Exporting) stringResource(R.string.settings_exporting) else null
        )
        Choice(
            title = stringResource(R.string.settings_import_title),
            body = stringResource(R.string.settings_import_body),
            button = stringResource(R.string.settings_import_button),
            onClick = { onAction(BackupAction.OnImportClick) },
            enabled = !busy,
            status = if (state == BackupState.Reading) stringResource(R.string.settings_reading) else null
        )
    }
}

@Composable
private fun Choice(
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
    enabled: Boolean,
    status: String?
) {
    val spacing = GilTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
        Title(title)
        BodyText(body)
        SecondaryActionButton(
            text = button,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.inlineGap),
            enabled = enabled
        )
        if (status != null) {
            Text(
                text = status,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Summary(preview: BackupPreview, isRestoring: Boolean, onAction: (BackupAction) -> Unit) {
    val spacing = GilTheme.spacing
    Page(
        actions = {
            PrimaryActionButton(
                text = stringResource(R.string.settings_restore_button),
                onClick = { onAction(BackupAction.OnRestoreClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRestoring
            )
            TextActionButton(
                text = stringResource(R.string.settings_cancel),
                onClick = { onAction(BackupAction.OnDoneClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRestoring
            )
        }
    ) {
        Title(stringResource(R.string.settings_preview_title), large = true)
        Column {
            FigureRow(stringResource(R.string.settings_preview_created), dateText(preview.createdAt))
            FigureDivider()
            FigureRow(stringResource(R.string.settings_preview_items), preview.itemCount.toString())
            FigureDivider()
            FigureRow(stringResource(R.string.settings_preview_lessons), preview.completedLessonCount.toString())
            FigureDivider()
            FigureRow(
                stringResource(R.string.settings_preview_last_day),
                preview.lastStudyDay?.let { dateText(it) } ?: stringResource(R.string.settings_preview_never)
            )
        }
        BodyText(stringResource(R.string.settings_preview_warning))
        if (isRestoring) {
            Text(
                text = stringResource(R.string.settings_restoring),
                modifier = Modifier
                    .padding(top = spacing.inlineGap)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Result(title: String, body: String, onDone: () -> Unit) {
    Page(
        actions = {
            PrimaryActionButton(
                text = stringResource(R.string.settings_ok),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Title(title, large = true)
        BodyText(body, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

/** A scrolling page with its [actions] kept at the bottom, so a large font never pushes them off. */
@Composable
private fun Page(actions: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenMargin)
                .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
            content = content
        )
        if (actions != null) {
            Column(
                modifier = Modifier.padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
                verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
                content = actions
            )
        }
    }
}

@Composable
private fun FigureRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BackupTokens.FigureRowMinHeight)
            .padding(vertical = GilTheme.spacing.inlineGap)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.tabularFigures(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

/** The page already keeps the screen margin, so the line runs from margin to margin. */
@Composable
private fun FigureDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Title(text: String, large: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
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

@Composable
private fun dateText(instant: kotlin.time.Instant): String =
    dateText(java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(ZoneId.systemDefault()).toLocalDate())

@Composable
private fun dateText(day: StudyDay): String = dateText(LocalDate.ofEpochDay(day.epochDay))

@Composable
private fun dateText(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
    }
}

@PreviewLightDark
@Composable
private fun BackupScreenPreview() {
    GilTheme {
        Surface {
            BackupScreen(state = BackupState.Idle, onAction = {}, onBack = {})
        }
    }
}
