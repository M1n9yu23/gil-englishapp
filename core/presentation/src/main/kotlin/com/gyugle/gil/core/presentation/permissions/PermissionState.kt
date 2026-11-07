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
package com.gyugle.gil.core.presentation.permissions

enum class PermissionState {
    GRANTED,

    /** Refused, and the system will show the dialog again. */
    DENIED,

    /** Refused for good. Only the app's system settings can change it. */
    PERMANENTLY_DENIED,

    /**
     * Not asked yet, as far as a query can tell. Android reports "never asked" and "refused for
     * good" the same way, so a query maps both here; the next request returns the real answer.
     */
    NOT_DETERMINED
}
