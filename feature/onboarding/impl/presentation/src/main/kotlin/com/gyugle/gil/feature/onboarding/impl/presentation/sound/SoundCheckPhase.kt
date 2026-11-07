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

enum class SoundCheckPhase {
    /** Waiting for the speech engine. */
    Checking,

    /** The American English voice is there; the word plays and the learner says whether they heard it. */
    Ready,

    /** The voice is there but the learner heard nothing: volume, silent mode, or broken voice data. */
    NotHeard,

    /** The engine has no offline American English voice. */
    NoVoice,

    /** No speech engine at all. Lessons fall back to text. */
    NoEngine
}
