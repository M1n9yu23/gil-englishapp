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
package com.gyugle.gil.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the app's baseline and startup profiles. Run `./gradlew :app:generateBaselineProfile` with
 * a device connected; the profiles land in app/src/main/generated/baselineProfiles.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Before
    fun setUp() = useEnglishLabels()

    @After
    fun tearDown() = clearAppLanguage()

    /** The cold start also decides which classes go first in the dex files. */
    @Test
    fun startup() = rule.collect(packageName = targetAppId, includeInStartupProfile = true) {
        startToToday()
    }

    /** What most study days go through after the start. */
    @Test
    fun studyDay() = rule.collect(packageName = targetAppId) {
        startToToday()
        visitTabs()
        openLessonAndLeave()
    }
}
