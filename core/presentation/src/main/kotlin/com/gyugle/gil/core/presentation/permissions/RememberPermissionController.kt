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

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Call it in a Root composable, not in a Screen: previews have no activity and this throws there.
 * Keyed on the activity so a configuration change never leaves it holding a destroyed one.
 */
@Composable
fun rememberPermissionController(): PermissionController {
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("rememberPermissionController needs a ComponentActivity host")
    return remember(activity) { PermissionController(activity) }
}
