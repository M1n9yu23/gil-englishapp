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
package com.gyugle.gil.core.navigation

import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene

/**
 * Entry metadata for a screen the learner works through and then closes, such as a lesson. A host
 * raises it from below instead of sliding it in from the side, so it reads as a task laid over the
 * app rather than a step deeper into a tab.
 */
fun taskScreen(): Map<String, Any> = metadata { put(TaskScreenKey, true) }

/** Whether the scene's top entry was marked with [taskScreen]. */
fun Scene<*>.isTaskScreen(): Boolean = entries.lastOrNull()?.let { TaskScreenKey in it.metadata } ?: false

private data object TaskScreenKey : NavMetadataKey<Boolean>
