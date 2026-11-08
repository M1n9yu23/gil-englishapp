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
package com.gyugle.gil.feature.settings.impl.data.repository

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.getSystemService
import com.gyugle.gil.feature.settings.impl.domain.model.AppLanguage
import com.gyugle.gil.feature.settings.impl.domain.repository.AppLanguageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The per-app language that Android 13 keeps for each app. The app lists its languages in the
 * generated locale config, which is also what the phone's settings offer.
 */
internal class SystemAppLanguageRepository @Inject constructor(@param:ApplicationContext private val context: Context) :
    AppLanguageRepository {

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    override val canChoose: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun current(): AppLanguage {
        if (!canChoose) return AppLanguage.SYSTEM
        val locales = context.getSystemService<LocaleManager>()?.applicationLocales ?: return AppLanguage.SYSTEM
        return AppLanguage.fromTags(locales.toLanguageTags())
    }

    override fun set(language: AppLanguage) {
        if (!canChoose) return
        context.getSystemService<LocaleManager>()?.applicationLocales =
            language.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
    }
}
