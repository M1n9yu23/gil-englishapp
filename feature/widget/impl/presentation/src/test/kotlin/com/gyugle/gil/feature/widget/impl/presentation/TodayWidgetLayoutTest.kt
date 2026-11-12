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
package com.gyugle.gil.feature.widget.impl.presentation

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasAnyDescendant
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import com.gyugle.gil.feature.dictionary.api.SentenceLink
import com.gyugle.gil.feature.home.api.TODAY_DEEP_LINK
import com.gyugle.gil.feature.review.api.REVIEW_DEEP_LINK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TodayWidgetLayoutTest {
    private val context = RuntimeEnvironment.getApplication()
    private val launcher = ComponentName(context, "com.gyugle.gil.MainActivity")
    private val sentence = SentenceUi(
        text = "How are you, Mina?",
        ko = "미나, 어떻게 지내요?",
        link = SentenceLink(21, "pattern.how-are-you").uri
    )

    @Before
    fun setUp() {
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(launcher)
            addIntentFilterForActivity(
                launcher,
                IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            )
        }
    }

    private fun widget(
        state: TodayWidgetState,
        size: androidx.compose.ui.unit.DpSize = FULL_SIZE,
        test: GlanceAppWidgetUnitTest.() -> Unit
    ) = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(size)
        provideComposable { TodayWidgetLayout(state) }
        test()
    }

    /** What tapping opens: the launch activity with [link]. */
    private fun opens(link: String) = hasStartActivityClickAction(checkNotNull(linkIntent(context, link)))

    @Test
    fun cardsDue_showTheCount_andOpenTheReview() = widget(TodayWidgetState.Ready(dueCount = 12, sentence = sentence)) {
        onNode(hasTextEqualTo("Today’s review")).assertExists()
        onNode(opens(REVIEW_DEEP_LINK) and hasAnyDescendant(hasTextEqualTo("12 cards"))).assertExists()
    }

    @Test
    fun oneCardDue_isOneCard() = widget(TodayWidgetState.Ready(dueCount = 1, sentence = sentence)) {
        onNode(hasTextEqualTo("1 card")).assertExists()
    }

    @Test
    fun nothingDue_saysSo_andOpensToday() = widget(TodayWidgetState.Ready(dueCount = 0, sentence = sentence)) {
        onNode(hasTextEqualTo("Nothing to review today")).assertExists()
        onNode(hasText("card")).assertDoesNotExist()
        // The row opens Today like the rest of the widget does.
        onAllNodes(opens(REVIEW_DEEP_LINK)).assertCountEquals(0)
        onAllNodes(opens(TODAY_DEEP_LINK) and hasAnyDescendant(hasTextEqualTo("Nothing to review today")))
            .assertCountEquals(2)
    }

    @Test
    fun fullSize_showsTheSentenceAndItsTranslation_andOpensItsBreakdown() =
        widget(TodayWidgetState.Ready(dueCount = 3, sentence = sentence)) {
            onNode(
                opens(sentence.link) and
                    hasAnyDescendant(hasTextEqualTo("How are you, Mina?")) and
                    hasAnyDescendant(hasTextEqualTo("미나, 어떻게 지내요?"))
            ).assertExists()
        }

    @Test
    fun sentenceWithoutTranslation_showsTheEnglishOnly() =
        widget(TodayWidgetState.Ready(dueCount = 3, sentence = sentence.copy(ko = null))) {
            onNode(hasTextEqualTo("How are you, Mina?")).assertExists()
            onNode(hasTextEqualTo("미나, 어떻게 지내요?")).assertDoesNotExist()
        }

    @Test
    fun oneRowTall_showsTheReviewsOnly() =
        widget(TodayWidgetState.Ready(dueCount = 3, sentence = sentence), size = ROW_SIZE) {
            onNode(hasTextEqualTo("3 cards")).assertExists()
            onNode(hasTextEqualTo("How are you, Mina?")).assertDoesNotExist()
        }

    @Test
    fun noSentenceYet_saysWhenOneComes() = widget(TodayWidgetState.Ready(dueCount = 0, sentence = null)) {
        onNode(
            hasTextEqualTo("From the day after your first lesson, a sentence you’ve learned shows up here.")
        ).assertExists()
    }

    @Test
    fun unavailable_saysSo_andOpensToday() = widget(TodayWidgetState.Unavailable) {
        onNode(opens(TODAY_DEEP_LINK) and hasAnyDescendant(hasText("Couldn’t read your study record"))).assertExists()
        onNode(hasText("review")).assertDoesNotExist()
    }
}
