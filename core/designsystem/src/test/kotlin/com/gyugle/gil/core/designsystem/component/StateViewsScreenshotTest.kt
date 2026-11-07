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
package com.gyugle.gil.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.core.designsystem.captureOnPaper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h640dp-xhdpi")
class StateViewsScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun empty() = composeRule.captureOnPaper {
        EmptyState(
            title = "아직 저장한 단어가 없어요",
            body = "과를 공부하다가 단어를 눌러 단어장에 넣어 보세요.",
            actionLabel = "오늘 공부 시작하기",
            onAction = {}
        )
    }

    private fun error() = composeRule.captureOnPaper {
        ErrorState(
            title = "학습 기록을 불러오지 못했어요",
            body = "휴대폰 저장 공간이 부족하면 이럴 수 있어요.",
            retryLabel = "다시 불러오기",
            onRetry = {}
        )
    }

    private fun loading() = composeRule.captureOnPaper { LoadingState() }

    @Test fun emptyState_light_showsPathAndAction() = empty()

    @Test
    @Config(qualifiers = "+night")
    fun emptyState_night_usesNightPaper() = empty()

    @Test
    @Config(fontScale = 2f)
    fun emptyState_fontScale2_scrollsInsteadOfClipping() = empty()

    @Test fun errorState_light_showsRetry() = error()

    @Test
    @Config(qualifiers = "+night")
    fun errorState_night_usesNightPaper() = error()

    @Test
    @Config(fontScale = 2f)
    fun errorState_fontScale2_wrapsText() = error()

    @Test fun loadingState_light_showsPlaceholders() = loading()

    @Test
    @Config(qualifiers = "+night")
    fun loadingState_night_usesNightPaper() = loading()
}
