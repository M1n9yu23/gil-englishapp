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
package com.gyugle.gil.feature.curriculum.impl.data.repository

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabase
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabaseProvider
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import timber.log.Timber

/**
 * Runs [block] on the content database. Any failure to copy, open or read it is reported as
 * [ContentError.UNAVAILABLE]: the screen offers a retry either way, and the cause goes to the log.
 */
internal suspend fun <T> ContentDatabaseProvider.read(
    block: suspend (ContentDatabase) -> Outcome<T, ContentError>
): Outcome<T, ContentError> = try {
    block(get())
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    Timber.e(e, "Could not read the content database")
    Outcome.Failure(ContentError.UNAVAILABLE)
}
