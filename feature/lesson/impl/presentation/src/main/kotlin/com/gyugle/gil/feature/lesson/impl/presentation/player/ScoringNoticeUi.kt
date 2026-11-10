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
package com.gyugle.gil.feature.lesson.impl.presentation.player

/** Why shadowing is only compared by ear on this phone. */
enum class ScoringNoticeUi {
    /** The recognizer supports English, but its offline model is not installed. */
    MODEL_NOT_INSTALLED,

    /** Below Android 13, or no on-device recognizer for English. */
    UNSUPPORTED
}
