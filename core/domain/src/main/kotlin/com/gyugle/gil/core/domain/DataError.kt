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
package com.gyugle.gil.core.domain

/**
 * Storage failures shared by every feature.
 *
 * The app has no network access, so there is no network branch. Each constant has a producer:
 * `databaseCall` in core:database and `editCatching` in core:datastore. Add a constant only
 * together with the branch that returns it.
 */
sealed interface DataError : RootError {
    enum class Local : DataError {
        /** SQLite reported a full disk. */
        DISK_FULL,

        /** Any other storage failure. The cause is not kept, so nothing about it can reach the UI. */
        UNKNOWN
    }
}
