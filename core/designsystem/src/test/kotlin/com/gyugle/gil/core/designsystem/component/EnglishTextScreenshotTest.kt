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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.buildAnnotatedString
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
class EnglishTextScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun texts() = composeRule.captureOnPaper {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EnglishText(text = "Could I get a glass of water, please?", size = EnglishTextSize.Focus)
            EnglishText(text = "I usually walk to work, but today I took the bus.")
            IpaText(ipa = "ˈwɔːtər", spokenWord = "water")
            Text(
                text = buildAnnotatedString {
                    append("물을 달라고 할 때는 ")
                    appendEnglish("Could I get")
                    append("으로 시작해요. 끝에 ")
                    appendEnglish("please")
                    append("를 붙이면 더 공손해요.")
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    @Test fun englishText_light_matchesDesign() = texts()

    @Test
    @Config(qualifiers = "+night")
    fun englishText_night_usesNightPaper() = texts()

    @Test
    @Config(fontScale = 2f)
    fun englishText_fontScale2_wrapsWithoutHyphens() = texts()
}
