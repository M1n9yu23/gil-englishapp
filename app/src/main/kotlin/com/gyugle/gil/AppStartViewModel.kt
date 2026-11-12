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
package com.gyugle.gil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.navigation.DeepLinkResolver
import com.gyugle.gil.core.navigation.DeepLinkTarget
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.lesson.api.LessonRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingObserver
import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import com.gyugle.gil.feature.settings.api.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Picks the host, and holds an outside entry until the main host takes it. Onboarding has no tabs
 * to open, so a link that arrives during the first run is dropped. Finishing the first run opens
 * the course's first lesson over the Today tab.
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    onboardingObserver: OnboardingObserver,
    appSettings: AppSettingsObserver,
    private val deepLinkResolver: DeepLinkResolver,
    private val courseObserver: CourseObserver
) : ViewModel() {

    // Eagerly, so the splash screen's condition sees the value without a collector.
    val host: StateFlow<AppHost> = onboardingObserver.observeCompleted()
        .map { completed -> if (completed) AppHost.Main else AppHost.Onboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppHost.Loading)

    /** Null until read, so the splash screen stays until the first frame has the right colours. */
    val theme: StateFlow<ThemeMode?> = appSettings.observeSettings()
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _deepLink = MutableStateFlow<DeepLinkTarget?>(null)

    /** The entry to open in the main host; call [onDeepLinkHandled] once it is open. */
    val deepLink: StateFlow<DeepLinkTarget?> = _deepLink.asStateFlow()

    /** The link being looked up; a newer one replaces it. */
    private var resolving: Job? = null

    init {
        viewModelScope.launch {
            var previous = AppHost.Loading
            host.collect { current ->
                if (previous == AppHost.Onboarding && current == AppHost.Main) openFirstLesson()
                previous = current
            }
        }
    }

    /** Without the content the first lesson cannot open; the Today tab shows instead. */
    private suspend fun openFirstLesson() {
        courseObserver.getFirstLessonId().onSuccess { lessonId ->
            _deepLink.value = DeepLinkTarget(TodayRoute, routes = listOf(LessonRoute(lessonId)))
        }
    }

    /**
     * The data URI of an intent that started or reached the activity. A URI the app does not know
     * opens the Today tab, as if the app were opened from the launcher.
     */
    fun onIntentData(uri: String?) {
        if (uri == null || host.value == AppHost.Onboarding) return
        resolving?.cancel()
        resolving = viewModelScope.launch {
            val target = deepLinkResolver.resolve(uri) ?: DeepLinkTarget(TodayRoute)
            // A link can arrive before the flag is read; the first run then drops it.
            if (host.first { it != AppHost.Loading } == AppHost.Main) _deepLink.value = target
        }
    }

    fun onDeepLinkHandled() {
        _deepLink.value = null
    }
}
