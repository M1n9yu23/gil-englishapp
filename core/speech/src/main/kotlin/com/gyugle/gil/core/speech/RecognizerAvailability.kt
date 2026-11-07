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
package com.gyugle.gil.core.speech

enum class RecognizerAvailability {
    /** The model for the language is installed and recognition can run offline. */
    READY,

    /** The recognizer supports the language, but its model is not installed or is still downloading. */
    NOT_DOWNLOADED,

    /** Below Android 13, no on-device recognizer, or the language is not supported. */
    UNAVAILABLE
}
