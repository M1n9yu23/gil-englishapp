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
import com.gyugle.gil.core.navigation.DeepLinkResolver
import com.gyugle.gil.core.navigation.DeepLinkTarget
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
 * to open, so a link that arrives during the first run is dropped.
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    onboardingObserver: OnboardingObserver,
    private val deepLinkResolver: DeepLinkResolver
) : ViewModel() {

    // Eagerly, so the splash screen's condition sees the value without a collector.
    val host: StateFlow<AppHost> = onboardingObserver.observeCompleted()
        .map { completed -> if (completed) AppHost.Main else AppHost.Onboarding }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppHost.Loading)

    private val _deepLink = MutableStateFlow<DeepLinkTarget?>(null)

    /** The entry to open in the main host; call [onDeepLinkHandled] once it is open. */
    val deepLink: StateFlow<DeepLinkTarget?> = _deepLink.asStateFlow()

    init {
        viewModelScope.launch {
            if (host.first { it != AppHost.Loading } == AppHost.Onboarding) _deepLink.value = null
        }
    }

    /**
     * The data URI of an intent that started or reached the activity. A URI the app does not know
     * opens the Today tab, as if the app were opened from the launcher.
     */
    fun onIntentData(uri: String?) {
        if (uri == null || host.value == AppHost.Onboarding) return
        _deepLink.value = deepLinkResolver.resolve(uri) ?: DeepLinkTarget(TodayRoute)
    }

    fun onDeepLinkHandled() {
        _deepLink.value = null
    }
}
