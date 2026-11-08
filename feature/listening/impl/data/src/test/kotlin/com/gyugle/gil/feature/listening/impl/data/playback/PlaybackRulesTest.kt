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
package com.gyugle.gil.feature.listening.impl.data.playback

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import org.junit.jupiter.api.Test

class PlaybackRulesTest {
    private val own = "com.gyugle.gil"

    @Test
    fun controllerAccess_theAppItself_mayLoadPlaylists() {
        assertThat(controllerAccess(own, own, trusted = true, notificationController = false))
            .isEqualTo(ControllerAccess.FULL)
    }

    @Test
    fun controllerAccess_notificationController_onlyControls() {
        assertThat(controllerAccess(own, own, trusted = true, notificationController = true))
            .isEqualTo(ControllerAccess.CONTROL)
    }

    @Test
    fun controllerAccess_trustedSystemController_onlyControls() {
        assertThat(controllerAccess("com.android.systemui", own, trusted = true, notificationController = false))
            .isEqualTo(ControllerAccess.CONTROL)
    }

    @Test
    fun controllerAccess_untrustedApp_isRejected() {
        assertThat(controllerAccess("com.example.other", own, trusted = false, notificationController = false))
            .isEqualTo(ControllerAccess.REJECTED)
    }

    @Test
    fun controllerAccess_ownPackageNameButUntrusted_isRejected() {
        // A package name alone proves nothing; trust is the system's check of the caller.
        assertThat(controllerAccess(own, own, trusted = false, notificationController = false))
            .isEqualTo(ControllerAccess.REJECTED)
    }

    @Test
    fun studyTimeTracker_recordsOnlyPlayingTime() {
        val time = TestTimeSource()
        val recorded = mutableListOf<Duration>()
        val tracker = StudyTimeTracker(time) { recorded += it }

        tracker.onPlayingChanged(true)
        time += 30.seconds
        tracker.onPlayingChanged(false)
        time += 60.seconds
        tracker.onPlayingChanged(false)
        tracker.onPlayingChanged(true)
        time += 10.seconds
        tracker.onPlayingChanged(false)

        assertThat(recorded).containsExactly(30.seconds, 10.seconds)
    }

    @Test
    fun studyTimeTracker_checkpoint_recordsSoFarAndKeepsCounting() {
        val time = TestTimeSource()
        val recorded = mutableListOf<Duration>()
        val tracker = StudyTimeTracker(time) { recorded += it }

        tracker.checkpoint()
        tracker.onPlayingChanged(true)
        time += 20.seconds
        tracker.checkpoint()
        time += 5.seconds
        tracker.onPlayingChanged(false)

        assertThat(recorded).containsExactly(20.seconds, 5.seconds)
    }
}
