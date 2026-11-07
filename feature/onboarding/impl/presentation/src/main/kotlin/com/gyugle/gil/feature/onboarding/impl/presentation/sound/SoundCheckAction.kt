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
package com.gyugle.gil.feature.onboarding.impl.presentation.sound

sealed interface SoundCheckAction {
    data object OnPlayClick : SoundCheckAction

    data object OnHeardClick : SoundCheckAction

    data object OnNotHeardClick : SoundCheckAction

    data object OnInstallClick : SoundCheckAction

    /** The phone has no screen for installing voice data. */
    data object OnInstallUnavailable : SoundCheckAction

    /** Ends the first run without sound: lessons show listening questions as text. */
    data object OnContinueWithoutSoundClick : SoundCheckAction

    /** Back on screen, perhaps after installing voice data. */
    data object OnResumed : SoundCheckAction
}
