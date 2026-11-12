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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

private const val TIMEOUT_MS = 10_000L

/** Today's main button: the first lesson on a fresh install, the next step after that. */
private val TODAY_NEXT = Pattern.compile("Start the first lesson|Continue")

/** The lesson's progress line once its first step is on screen. */
private val FIRST_STEP = Pattern.compile("Step 1 of \\d+")

/** The first screen after the splash: onboarding's skip button, or a tab label. */
private val ONBOARDING_OR_TABS = Pattern.compile("Skip|Stats")

/** The application id of the APK under test, passed in by the Gradle build. */
val targetAppId: String
    get() = checkNotNull(InstrumentationRegistry.getArguments().getString("targetAppId")) {
        "The targetAppId instrumentation argument is missing"
    }

/** The journeys find buttons by their English labels, whatever language the device is in. */
fun useEnglishLabels() {
    setAppLanguage("en-US")
}

/** Back to following the device language. */
fun clearAppLanguage() {
    setAppLanguage("")
}

private fun setAppLanguage(tags: String) {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .executeShellCommand("cmd locale set-app-locales $targetAppId --locales $tags")
}

/**
 * A cold start up to Today's plan. On a fresh install it skips onboarding, which keeps the default
 * goal and opens the first lesson, and leaves that lesson.
 */
fun MacrobenchmarkScope.startToToday() {
    pressHome()
    startActivityAndWait()
    val first = checkNotNull(device.wait(Until.findObject(By.text(ONBOARDING_OR_TABS)), TIMEOUT_MS)) {
        "Neither onboarding nor the tabs appeared"
    }
    if (first.text == "Skip") {
        first.click()
        leaveLesson()
    }
    waitForToday()
}

/** Opens each tab from Today, scrolls it and comes back. */
fun MacrobenchmarkScope.visitTabs() {
    for (tab in listOf("Path", "Words", "Stats", "Today")) {
        checkNotNull(device.wait(Until.findObject(By.text(tab)), TIMEOUT_MS)) { "No $tab tab" }.click()
        device.waitForIdle()
        device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        device.waitForIdle()
    }
    waitForToday()
}

/** Opens the next lesson from Today, then leaves it from the first step. */
fun MacrobenchmarkScope.openLessonAndLeave() {
    checkNotNull(device.wait(Until.findObject(By.text(TODAY_NEXT)), TIMEOUT_MS)) { "No lesson to open" }.click()
    leaveLesson()
    waitForToday()
}

private fun MacrobenchmarkScope.leaveLesson() {
    // The close button shows while the lesson loads too, and Back then leaves without asking.
    checkNotNull(device.wait(Until.findObject(By.desc(FIRST_STEP)), TIMEOUT_MS)) { "The lesson did not open" }
    device.pressBack()
    checkNotNull(device.wait(Until.findObject(By.text("Leave")), TIMEOUT_MS)) { "No leave dialog" }.click()
}

/** The tab bar and Today's main button; a lesson has a Continue button but no tabs. */
private fun MacrobenchmarkScope.waitForToday() {
    checkNotNull(device.wait(Until.findObject(By.text("Stats")), TIMEOUT_MS)) { "The tabs did not appear" }
    checkNotNull(device.wait(Until.findObject(By.text(TODAY_NEXT)), TIMEOUT_MS)) { "Today's plan did not appear" }
}
