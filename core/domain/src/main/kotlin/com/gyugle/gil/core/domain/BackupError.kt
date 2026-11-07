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
 * Why a backup section could not be restored.
 *
 * A backup file comes from outside the app, so every owner that restores a section reads it as
 * untrusted input and reports one of these instead of throwing.
 */
sealed interface BackupError : RootError {
    enum class Format : BackupError {
        /** Not valid JSON, a missing or wrong field, a value out of range or a duplicate entry. */
        MALFORMED,

        /** Written by a newer version of the app in a format this one cannot read. */
        NEWER_VERSION
    }

    /** The section was valid but could not be written. */
    data class Storage(val error: DataError.Local) : BackupError
}
