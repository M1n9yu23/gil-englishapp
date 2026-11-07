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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
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
class AnswerOptionScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun answered() = composeRule.captureOnPaper {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AnswerOption(text = "고맙다", english = false, mark = AnswerMark.CORRECT, onClick = {})
            AnswerOption(text = "미안하다", english = false, mark = AnswerMark.PICKED_WRONG, onClick = {})
            AnswerOption(text = "ship", english = true, ipa = "ʃɪp", mark = AnswerMark.OTHER, onClick = {})
            AnswerOption(text = "sheep", english = true, ipa = "ʃiːp", mark = AnswerMark.IDLE, onClick = {})
        }
    }

    @Test fun answerOption_light_matchesDesign() = answered()

    @Test
    @Config(qualifiers = "+night")
    fun answerOption_night_usesNightPaper() = answered()

    @Test
    @Config(fontScale = 2f)
    fun answerOption_fontScale2_wrapsText() = answered()
}
