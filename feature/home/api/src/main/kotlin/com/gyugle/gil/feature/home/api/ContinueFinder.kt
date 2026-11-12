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
package com.gyugle.gil.feature.home.api

import androidx.navigation3.runtime.NavKey

/**
 * The data URI that opens what "Continue" on the Today tab opens, such as from a launcher shortcut.
 * Put it on an explicit intent to the app's launch activity.
 */
const val CONTINUE_DEEP_LINK = "gil://continue"

/** Finds what "Continue" on the Today tab opens now, so an entry from outside can open the same. */
fun interface ContinueFinder {
    /**
     * The route to push above the Today tab: today's reviews or the next lesson. Null when the plan
     * has nothing left, or when the record or the course could not be read.
     */
    suspend fun find(): NavKey?
}
