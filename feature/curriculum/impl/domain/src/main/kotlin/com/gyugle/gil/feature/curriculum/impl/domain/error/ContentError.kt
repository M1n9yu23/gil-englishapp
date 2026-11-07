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
package com.gyugle.gil.feature.curriculum.impl.domain.error

import com.gyugle.gil.core.domain.RootError

enum class ContentError : RootError {
    /** The content database could not be copied, opened or read. Trying again may work. */
    UNAVAILABLE,

    /** No row has the requested id, for example a unit an app update removed. */
    NOT_FOUND
}
