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
package com.gyugle.gil.core.presentation

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DeviceConfigurationTest {

    @ParameterizedTest(name = "{0}x{1} -> {2}")
    @CsvSource(
        "411, 891, MOBILE_PORTRAIT",
        "411, 440, MOBILE_PORTRAIT",
        "568, 320, MOBILE_PORTRAIT",
        "891, 411, MOBILE_LANDSCAPE",
        "732, 360, MOBILE_LANDSCAPE",
        "1600, 400, MOBILE_LANDSCAPE",
        "600, 800, TABLET_PORTRAIT",
        "673, 841, TABLET_PORTRAIT",
        "800, 1280, TABLET_PORTRAIT",
        "841, 673, TABLET_LANDSCAPE",
        "1280, 800, TABLET_LANDSCAPE",
        "1366, 1024, DESKTOP",
        "1024, 1366, DESKTOP",
        "1920, 1080, DESKTOP"
    )
    fun fromWindowSizeClass_windowSize_mapsToExpectedConfiguration(
        widthDp: Int,
        heightDp: Int,
        expected: DeviceConfiguration
    ) {
        for (breakpoints in listOf(WindowSizeClass.BREAKPOINTS_V1, WindowSizeClass.BREAKPOINTS_V2)) {
            val sizeClass = breakpoints.computeWindowSizeClass(widthDp, heightDp)

            assertThat(DeviceConfiguration.fromWindowSizeClass(sizeClass)).isEqualTo(expected)
        }
    }

    @Test
    fun isMobile_phoneShapes_true() {
        assertThat(DeviceConfiguration.MOBILE_PORTRAIT.isMobile).isTrue()
        assertThat(DeviceConfiguration.MOBILE_LANDSCAPE.isMobile).isTrue()
        assertThat(DeviceConfiguration.TABLET_PORTRAIT.isMobile).isFalse()
    }

    @Test
    fun isWideScreen_landscapeTabletAndDesktop_true() {
        assertThat(DeviceConfiguration.TABLET_LANDSCAPE.isWideScreen).isTrue()
        assertThat(DeviceConfiguration.DESKTOP.isWideScreen).isTrue()
        assertThat(DeviceConfiguration.TABLET_PORTRAIT.isWideScreen).isFalse()
        assertThat(DeviceConfiguration.MOBILE_LANDSCAPE.isWideScreen).isFalse()
    }
}
