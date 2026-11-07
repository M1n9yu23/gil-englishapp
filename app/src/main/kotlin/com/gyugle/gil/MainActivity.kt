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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.theme.GilTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so the splash theme is swapped for the app theme in time.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Until the first read of the onboarding flag, so neither host flashes before the other.
        splashScreen.setKeepOnScreenCondition { viewModel.host.value == AppHost.Loading }
        enableEdgeToEdge()
        // A recreated activity already opened its link; its back stacks were restored instead. From
        // Recents the old intent comes back with its link, which was opened back then.
        val fromRecents = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromRecents) viewModel.onIntentData(intent.dataString)
        setContent {
            GilTheme {
                val host by viewModel.host.collectAsStateWithLifecycle()
                val deepLink by viewModel.deepLink.collectAsStateWithLifecycle()
                val fade = tween<Float>(GilTheme.motion.fade)
                AnimatedContent(
                    targetState = host,
                    // The splash screen already hides the first host appearing; fading it in again lags.
                    transitionSpec = {
                        if (initialState == AppHost.Loading) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            fadeIn(fade) togetherWith fadeOut(fade)
                        }
                    },
                    label = "host"
                ) { shown ->
                    when (shown) {
                        // The splash screen covers this frame.
                        AppHost.Loading -> Unit

                        AppHost.Onboarding -> OnboardingHost()

                        AppHost.Main -> MainHost(deepLink = deepLink, onDeepLinkOpen = viewModel::onDeepLinkHandled)
                    }
                }
            }
        }
    }

    /** A reminder tapped while the app is open arrives here, not through [getIntent]. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onIntentData(intent.dataString)
    }
}
