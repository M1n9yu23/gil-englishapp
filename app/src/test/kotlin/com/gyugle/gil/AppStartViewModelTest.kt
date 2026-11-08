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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.navigation.DeepLinkTarget
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.home.api.TODAY_DEEP_LINK
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.lesson.api.LessonRoute
import com.gyugle.gil.feature.listening.api.LISTENING_DEEP_LINK
import com.gyugle.gil.feature.listening.api.ListeningPlayerRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingObserver
import com.gyugle.gil.feature.settings.api.FakeAppSettingsObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppStartViewModelTest {
    /** Emits nothing until a test says what the stored flag is, like the first file read. */
    private val completed = MutableSharedFlow<Boolean>(replay = 1)
    private val observer = object : OnboardingObserver {
        override fun observeCompleted(): Flow<Boolean> = completed
    }

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private val firstLesson = Lesson(
        id = LessonId("lesson.s0-01.1"),
        title = LocalizedText("매일 조금씩", "A little every day"),
        goal = LocalizedText("", ""),
        steps = emptyList(),
        introduced = emptyList(),
        next = null
    )
    private val course = FakeCourseObserver(firstLesson)

    private fun viewModel() = AppStartViewModel(observer, FakeAppSettingsObserver(), GilDeepLinkResolver(), course)

    @Test
    fun onboardingFinished_opensTheFirstLessonOverToday() {
        val viewModel = viewModel()
        completed.tryEmit(false)

        completed.tryEmit(true)

        assertThat(viewModel.deepLink.value)
            .isEqualTo(DeepLinkTarget(TodayRoute, routes = listOf(LessonRoute(firstLesson.id))))
    }

    @Test
    fun onboardingFinished_contentUnavailable_opensNothing() {
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        completed.tryEmit(false)

        completed.tryEmit(true)

        assertThat(viewModel.deepLink.value).isNull()
    }

    @Test
    fun startAfterTheFirstRun_opensNoLesson() {
        val viewModel = viewModel()

        completed.tryEmit(true)

        assertThat(viewModel.deepLink.value).isNull()
    }

    @Test
    fun host_beforeTheFlagIsRead_isLoading() {
        assertThat(viewModel().host.value).isEqualTo(AppHost.Loading)
    }

    @Test
    fun host_firstRun_isOnboarding() {
        val viewModel = viewModel()
        completed.tryEmit(false)

        assertThat(viewModel.host.value).isEqualTo(AppHost.Onboarding)
    }

    @Test
    fun host_onboardingFinished_switchesToMain() {
        val viewModel = viewModel()
        completed.tryEmit(false)

        completed.tryEmit(true)

        assertThat(viewModel.host.value).isEqualTo(AppHost.Main)
    }

    @Test
    fun todayLink_inMain_opensTheTodayTab() {
        val viewModel = viewModel()
        completed.tryEmit(true)

        viewModel.onIntentData(TODAY_DEEP_LINK)

        assertThat(viewModel.deepLink.value).isEqualTo(DeepLinkTarget(TodayRoute))
    }

    @Test
    fun listeningLink_inMain_opensThePlayerAboveToday() {
        val viewModel = viewModel()
        completed.tryEmit(true)

        viewModel.onIntentData(LISTENING_DEEP_LINK)

        assertThat(viewModel.deepLink.value).isEqualTo(DeepLinkTarget(TodayRoute, listOf(ListeningPlayerRoute)))
    }

    @Test
    fun unknownLink_opensTheTodayTabInstead() {
        val viewModel = viewModel()
        completed.tryEmit(true)

        viewModel.onIntentData("gil://lesson/../../settings")

        assertThat(viewModel.deepLink.value).isEqualTo(DeepLinkTarget(TodayRoute))
    }

    @Test
    fun noData_opensNothing() {
        val viewModel = viewModel()
        completed.tryEmit(true)

        viewModel.onIntentData(null)

        assertThat(viewModel.deepLink.value).isNull()
    }

    @Test
    fun link_handled_isCleared() {
        val viewModel = viewModel()
        completed.tryEmit(true)
        viewModel.onIntentData(TODAY_DEEP_LINK)

        viewModel.onDeepLinkHandled()

        assertThat(viewModel.deepLink.value).isNull()
    }

    @Test
    fun link_duringTheFirstRun_isDropped() {
        val viewModel = viewModel()
        completed.tryEmit(false)

        viewModel.onIntentData(TODAY_DEEP_LINK)

        assertThat(viewModel.deepLink.value).isNull()
    }

    // A reminder can start the app before the flag is read; the first run still drops it.
    @Test
    fun link_beforeTheFlagIsRead_isDroppedWhenItTurnsOutToBeTheFirstRun() {
        val viewModel = viewModel()
        viewModel.onIntentData(TODAY_DEEP_LINK)

        completed.tryEmit(false)

        assertThat(viewModel.deepLink.value).isNull()
    }

    @Test
    fun link_beforeTheFlagIsRead_isKeptForTheMainHost() {
        val viewModel = viewModel()
        viewModel.onIntentData(TODAY_DEEP_LINK)

        completed.tryEmit(true)

        assertThat(viewModel.deepLink.value).isEqualTo(DeepLinkTarget(TodayRoute))
    }
}
