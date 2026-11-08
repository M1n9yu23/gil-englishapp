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
package com.gyugle.gil.feature.listening.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.DialogueLine
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.api.UnitLessons
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningChoices
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistEntry
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.UnitChoice
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ListeningLibraryTest {
    private val stage = StageTag(StageId("stage.a1"), 2, LocalizedText("첫걸음", "First steps"))
    private val hello = SenseId("sense.hello.interj.greeting")
    private val name = SenseId("sense.name.n.what-called")
    private val niceToMeet = PatternId("pattern.nice-to-meet-you")
    private val letterA = SoundId("sound.letter-a")

    private val helloExample = Sentence(10, "Hello, Mina.", "안녕하세요, 미나 씨.")
    private val dialogueLines = listOf(
        DialogueLine("Mina", Sentence(20, "Hi, I’m Mina.", "안녕, 나는 미나야.")),
        DialogueLine("Tom", Sentence(21, "Nice to meet you.", "만나서 반가워."))
    )

    private val first = lesson("lesson.s1-01.1", introduced = listOf(letterA))
    private val second = lesson(
        "lesson.a1-01.1",
        introduced = listOf(hello, niceToMeet),
        steps = listOf(LessonStep.Dialogue(dialogueLines, question = null, options = emptyList(), item = null))
    )
    private val third = lesson("lesson.a1-02.1", introduced = listOf(name))

    private val unitS1 = UnitLessons(UnitId("unit.s1-01"), LocalizedText("글자", "Letters"), listOf(first.id))
    private val unitA1 = UnitLessons(UnitId("unit.a1-01"), LocalizedText("인사", "Greetings"), listOf(second.id))
    private val unitA2 = UnitLessons(UnitId("unit.a1-02"), LocalizedText("이름", "Names"), listOf(third.id))

    private val course = FakeCourseObserver(first, second, third).apply { units = listOf(unitS1, unitA1, unitA2) }
    private val lexicon = FakeLexiconObserver(
        word("hello", hello, "안녕하세요", helloExample),
        word("name", name, "이름"),
        PatternEntry(niceToMeet, "Nice to meet you.", "만나서 반가워요", null, stage, emptyList())
    )
    private val progress = FakeProgressObserver()
    private val clock = FakeStudyClock()
    private val library = ListeningLibrary(course, lexicon, progress, clock)

    @Test
    fun choices_nothingFinished_isEmpty() = runTest {
        val choices = (library.choices() as Outcome.Success).data

        assertThat(choices).isEqualTo(ListeningChoices(0, 0, 0, emptyList()))
        assertThat(choices.isEmpty).isEqualTo(true)
    }

    @Test
    fun choices_finishedLessons_countsTheirItemsWithoutSoundsAndListsUnitsWithItems() = runTest {
        progress.completedLessons.value = setOf(first.id, second.id)
        progress.cards.value = listOf(card(hello, dueDay = clock.today()), card(letterA, dueDay = clock.today()))

        assertThat(library.choices()).isEqualTo(
            Outcome.Success(
                ListeningChoices(
                    dueToday = 1,
                    recent = 2,
                    dialogues = 1,
                    // The letters unit has only a sound, so there is nothing to hear in it.
                    units = listOf(UnitChoice(unitA1.id, unitA1.title, 2))
                )
            )
        )
    }

    @Test
    fun entries_dueToday_onlyCardsDueByToday() = runTest {
        progress.cards.value = listOf(
            card(name, dueDay = StudyDay(clock.today().epochDay + 1)),
            card(hello, dueDay = clock.today())
        )

        assertThat(library.entries(PlaylistSource.DueToday)).isEqualTo(
            Outcome.Success(listOf(PlaylistEntry.Item(hello, "hello", "안녕하세요", listOf(helloExample))))
        )
    }

    @Test
    fun entries_recent_finishedItemsInCourseOrder() = runTest {
        progress.completedLessons.value = setOf(third.id, second.id)

        val ids = (library.entries(PlaylistSource.Recent) as Outcome.Success).data
            .map { (it as PlaylistEntry.Item).id }

        assertThat(ids).isEqualTo(listOf(hello, niceToMeet, name))
    }

    @Test
    fun entries_recentWithMoreThanTheLimit_keepsTheLastOnes() = runTest {
        val many = (1..MAX_TRACKS + 5).map { PatternId("pattern.p$it") }
        course.lessons.clear()
        course.lessons += lesson("lesson.a1-03.1", introduced = many)
        course.units =
            listOf(UnitLessons(UnitId("unit.a1-03"), LocalizedText("많이", "Many"), listOf(course.lessons[0].id)))
        lexicon.entries += many.map { PatternEntry(it, it.value, "뜻", null, stage, emptyList()) }
        progress.completedLessons.value = setOf(course.lessons[0].id)

        val ids = (library.entries(PlaylistSource.Recent) as Outcome.Success).data
            .map { (it as PlaylistEntry.Item).id }

        assertThat(ids).isEqualTo(many.takeLast(MAX_TRACKS))
    }

    @Test
    fun entries_unit_onlyFinishedLessonsOfThatUnit() = runTest {
        progress.completedLessons.value = setOf(second.id, third.id)

        val ids = (library.entries(PlaylistSource.CourseUnit(unitA2.id)) as Outcome.Success).data
            .map { (it as PlaylistEntry.Item).id }

        assertThat(ids).isEqualTo(listOf(name))
    }

    @Test
    fun entries_dialogues_dialoguesOfFinishedLessons() = runTest {
        progress.completedLessons.value = setOf(second.id)

        assertThat(library.entries(PlaylistSource.Dialogues)).isEqualTo(
            Outcome.Success(listOf(PlaylistEntry.Dialogue(second.id, 0, dialogueLines)))
        )
    }

    @Test
    fun entries_itemMissingFromDictionary_leavesItOut() = runTest {
        lexicon.entries.removeAll { it.id == WordId("word.hello") }
        progress.completedLessons.value = setOf(second.id)

        val ids = (library.entries(PlaylistSource.Recent) as Outcome.Success).data
            .map { (it as PlaylistEntry.Item).id }

        assertThat(ids).isEqualTo(listOf(niceToMeet))
    }

    @Test
    fun entries_dictionaryUnreadable_fails() = runTest {
        lexicon.error = ContentError.UNAVAILABLE
        progress.completedLessons.value = setOf(second.id)

        assertThat(library.entries(PlaylistSource.Recent)).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    @Test
    fun choices_courseUnreadable_fails() = runTest {
        course.error = ContentError.UNAVAILABLE

        assertThat(library.choices()).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    private fun lesson(id: String, introduced: List<StudyItemId>, steps: List<LessonStep> = emptyList()) = Lesson(
        id = LessonId(id),
        title = LocalizedText(id, id),
        goal = LocalizedText("", ""),
        steps = steps,
        introduced = introduced.map { LessonItem(it, it.value, null, null) },
        next = null
    )

    private fun word(text: String, id: SenseId, ko: String, vararg examples: Sentence) = WordEntry(
        id = WordId("word.$text"),
        text = text,
        ipa = text,
        hangul = null,
        senses = listOf(WordSense(id, PartOfSpeech.NOUN, ko, null, null, stage, examples.toList())),
        collocations = emptyList(),
        family = emptyList(),
        notes = emptyList()
    )

    private fun card(id: StudyItemId, dueDay: StudyDay) = CardState(
        id,
        stabilityDays = 1.0,
        dueDay = dueDay,
        lastReviewDay = StudyDay(0),
        reviewCount = 1,
        lapseCount = 0
    )
}
