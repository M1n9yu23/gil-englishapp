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
package com.gyugle.gil.feature.settings.impl.presentation.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.settings.impl.presentation.R

/** The sections of the policy, in the order of PRIVACY.md, which this page keeps in step with. */
private enum class PolicySection(@param:StringRes val title: Int, @param:StringRes val body: Int) {
    Stored(R.string.settings_privacy_stored_title, R.string.settings_privacy_stored_body),
    Microphone(R.string.settings_privacy_mic_title, R.string.settings_privacy_mic_body),
    Speech(R.string.settings_privacy_speech_title, R.string.settings_privacy_speech_body),
    Backup(R.string.settings_privacy_backup_title, R.string.settings_privacy_backup_body),
    Report(R.string.settings_privacy_report_title, R.string.settings_privacy_report_body),
    Changes(R.string.settings_privacy_changes_title, R.string.settings_privacy_changes_body)
}

/** The privacy policy inside the app, so it can be read without a connection. Nothing to load. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.settings_privacy), onNavigateBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenMargin)
                .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            Text(
                text = stringResource(R.string.settings_privacy_updated),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_privacy_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )
            PolicySection.entries.forEach { section ->
                Text(
                    text = stringResource(section.title),
                    modifier = Modifier
                        .padding(top = spacing.itemGap)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(section.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PrivacyScreenPreview() {
    GilTheme {
        Surface {
            PrivacyScreen(onBack = {})
        }
    }
}
