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

import android.Manifest
import android.os.Build

/** The platform permission behind [this], or null when this API level grants it without asking. */
internal fun Permission.toManifestPermission(): String? = when (this) {
    Permission.NOTIFICATIONS ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null

    Permission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
}

/** Right after a request, "no rationale" can only mean the user refused for good. */
internal fun resolveStateAfterRequest(granted: Boolean, showRationale: Boolean): PermissionState = when {
    granted -> PermissionState.GRANTED
    showRationale -> PermissionState.DENIED
    else -> PermissionState.PERMANENTLY_DENIED
}

/** Without a request, "no rationale" is ambiguous, so it maps to the state that offers to ask. */
internal fun resolveQueriedState(granted: Boolean, showRationale: Boolean): PermissionState = when {
    granted -> PermissionState.GRANTED
    showRationale -> PermissionState.DENIED
    else -> PermissionState.NOT_DETERMINED
}
