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
package com.gyugle.gil.feature.settings.impl.domain.repository

import com.gyugle.gil.feature.settings.impl.domain.model.AppLanguage

/**
 * The app's language. The system keeps it, not the app, so it is not part of a backup and can
 * also be changed in the phone's settings while the app is in the background.
 */
interface AppLanguageRepository {
    /** False below Android 13, where the app always follows the phone's language. */
    val canChoose: Boolean

    /** [AppLanguage.SYSTEM] when nothing was chosen or [canChoose] is false. */
    fun current(): AppLanguage

    /** The open screens are recreated in the new language. Does nothing when [canChoose] is false. */
    fun set(language: AppLanguage)
}
