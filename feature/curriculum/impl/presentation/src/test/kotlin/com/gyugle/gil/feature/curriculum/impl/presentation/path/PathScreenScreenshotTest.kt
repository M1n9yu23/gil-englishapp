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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.impl.presentation.captureScreen
import com.gyugle.gil.feature.curriculum.impl.presentation.toContentErrorUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class PathScreenScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: PathState) = composeRule.captureScreen {
        PathScreen(state = state, onAction = {}, onUnitClick = {}, onTestClick = {})
    }

    private val stages = PathState(isLoading = false, stages = PathPreviewData.stages)

    @Test fun stages_light_showsLineWithCurrentStage() = capture(stages)

    @Test
    @Config(qualifiers = "+night")
    fun stages_night_usesNightPaper() = capture(stages)

    @Test
    @Config(fontScale = 2f)
    fun stages_fontScale2_keepsMarksOnTitles() = capture(stages)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi")
    fun stages_english_showsEnglishTitles() = capture(stages)

    // The shipped course before any unit is written: every stage is still empty.
    @Test
    fun stages_noUnitsYet_saysEachStageIsComing() = capture(
        PathState(
            isLoading = false,
            stages = PathPreviewData.stages.mapIndexed { index, stage ->
                stage.copy(
                    units = emptyList(),
                    canDo = emptyList(),
                    testQuestions = null,
                    place = if (index == 0) StagePlace.CURRENT else StagePlace.AHEAD
                )
            }
        )
    )

    @Test fun loading_light_showsPlaceholders() = capture(PathState())

    @Test
    fun error_light_offersRetry() =
        capture(PathState(isLoading = false, error = ContentError.UNAVAILABLE.toContentErrorUi()))
}
