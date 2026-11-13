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

import android.content.Context
import android.text.Spanned
import android.text.style.LocaleSpan
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.designsystem.theme.GilTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** TalkBack picks an English voice only when the node text carries an en-US LocaleSpan. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnglishTextAccessibilityTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun enableTalkBack() {
        val manager = composeRule.activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setEnabled(true)
        shadowOf(manager).setTouchExplorationEnabled(true)
    }

    @Test
    fun englishText_accessibilityText_isOneEnglishLocaleSpan() {
        composeRule.setContent { GilTheme { EnglishText(text = SENTENCE) } }

        val text = accessibilityText(SENTENCE)

        val spans = text.getSpans(0, text.length, LocaleSpan::class.java)
        assertThat(spans.map { it.locales.toLanguageTags() }).containsExactly("en-US")
        assertThat(text.getSpanStart(spans[0]) to text.getSpanEnd(spans[0])).isEqualTo(0 to SENTENCE.length)
    }

    @Test
    fun appendEnglish_insideKorean_spansOnlyTheEnglishPart() {
        composeRule.setContent {
            GilTheme {
                Text(
                    buildAnnotatedString {
                        append(KOREAN_BEFORE)
                        appendEnglish(INLINE_ENGLISH)
                        append(KOREAN_AFTER)
                    }
                )
            }
        }

        val text = accessibilityText(KOREAN_BEFORE + INLINE_ENGLISH + KOREAN_AFTER)

        val span = text.getSpans(0, text.length, LocaleSpan::class.java).single()
        assertThat(span.locales.toLanguageTags()).isEqualTo("en-US")
        assertThat(text.subSequence(text.getSpanStart(span), text.getSpanEnd(span)).toString())
            .isEqualTo(INLINE_ENGLISH)
    }

    @Test
    fun ipaText_accessibilityText_isTheWordInEnglish() {
        composeRule.setContent { GilTheme { IpaText(ipa = IPA, spokenWord = IPA_WORD) } }

        val text = accessibilityText(IPA_WORD)

        val span = text.getSpans(0, text.length, LocaleSpan::class.java).single()
        assertThat(span.locales.toLanguageTags()).isEqualTo("en-US")
        assertThat(text.toString()).isEqualTo(IPA_WORD)
    }

    @Test
    fun blankSentenceText_accessibilityText_readsTheGapAsAWordBetweenEnglishParts() {
        composeRule.setContent { GilTheme { BlankSentenceText(text = BLANK_SENTENCE) } }

        val text = accessibilityText(BLANK_SENTENCE_SPOKEN)

        // The gap takes the app's locale, which is English under Robolectric too.
        val parts = text.getSpans(0, text.length, LocaleSpan::class.java)
            .sortedBy { text.getSpanStart(it) }
            .map {
                text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() to
                    it.locales.toLanguageTags()
            }
        assertThat(parts).containsExactly("I'm new " to "en-US", "blank" to "en-US", "." to "en-US")
    }

    @Test
    fun choiceRow_labelLocale_spansTheLabel() {
        composeRule.setContent {
            GilTheme {
                ChoiceRow(label = LANGUAGE_NAME, selected = false, onSelect = {}, labelLocale = LocaleList("ko-KR"))
            }
        }

        val text = accessibilityText(LANGUAGE_NAME, insideRow = true)

        val span = text.getSpans(0, text.length, LocaleSpan::class.java).single()
        assertThat(span.locales.toLanguageTags()).isEqualTo("ko-KR")
    }

    /**
     * The text TalkBack gets for the node showing [semanticsText]. Compose reports the unmerged
     * tree and the screen reader merges a row's texts itself, so a text inside a row is its own node.
     */
    private fun accessibilityText(semanticsText: String, insideRow: Boolean = false): Spanned {
        val node = composeRule.onNodeWithText(semanticsText, useUnmergedTree = insideRow).fetchSemanticsNode()
        val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
        val host =
            checkNotNull(content.findAccessibilityHost()) { "No accessibility node provider under the content view" }
        val info = checkNotNull(host.accessibilityNodeProvider.createAccessibilityNodeInfo(node.id))
        return info.text as Spanned
    }

    private fun View.findAccessibilityHost(): View? = if (accessibilityNodeProvider != null) {
        this
    } else {
        (this as? ViewGroup)?.let { group ->
            (0 until group.childCount).firstNotNullOfOrNull { group.getChildAt(it).findAccessibilityHost() }
        }
    }

    private companion object {
        const val SENTENCE = "Could I get a glass of water?"
        const val KOREAN_BEFORE = "물을 달라고 할 때는 "
        const val INLINE_ENGLISH = "Could I get"
        const val KOREAN_AFTER = "으로 시작해요."
        const val IPA = "ˈwɔːtər"
        const val IPA_WORD = "water"
        const val BLANK_SENTENCE = "I'm new ___."
        const val BLANK_SENTENCE_SPOKEN = "I'm new blank."
        const val LANGUAGE_NAME = "한국어"
    }
}
