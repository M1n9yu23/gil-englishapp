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

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningChoices
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistEntry
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.UnitChoice
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * A playlist plays at most this many tracks. Every track is written to audio before playing starts,
 * so this keeps the wait short; about 10 minutes of listening.
 */
const val MAX_TRACKS = 25

/**
 * Makes playlists from what the learner has met: finished lessons and the review schedule. Sounds
 * are left out, since they have no meaning to say in Korean.
 *
 * Reads fail with the [ContentError] of the content they could not read. A failure to read the
 * learner's record is thrown, as [ProgressObserver]'s flows throw it.
 */
class ListeningLibrary @Inject constructor(
    private val course: CourseObserver,
    private val lexicon: LexiconObserver,
    private val progress: ProgressObserver,
    private val clock: StudyClock
) {

    suspend fun choices(): Outcome<ListeningChoices, ContentError> {
        val units = when (val read = finishedUnits()) {
            is Outcome.Failure -> return read
            is Outcome.Success -> read.data
        }
        val lessons = units.flatMap { it.lessons }
        return Outcome.Success(
            ListeningChoices(
                dueToday = dueItems().size,
                recent = recentItems(lessons).size,
                dialogues = dialogues(lessons).size,
                units = units.mapNotNull { unit ->
                    val count = itemsOf(unit.lessons).size
                    if (count == 0) null else UnitChoice(unit.id, unit.title, count)
                }
            )
        )
    }

    /** What [source] plays, in order. Items the dictionary no longer has are left out. */
    suspend fun entries(source: PlaylistSource): Outcome<List<PlaylistEntry>, ContentError> {
        val units = when (val read = finishedUnits()) {
            is Outcome.Failure -> return read
            is Outcome.Success -> read.data
        }
        val lessons = units.flatMap { it.lessons }
        val items = when (source) {
            PlaylistSource.DueToday -> dueItems()
            PlaylistSource.Recent -> recentItems(lessons)
            is PlaylistSource.CourseUnit -> itemsOf(units.firstOrNull { it.id == source.id }?.lessons.orEmpty())
            PlaylistSource.Dialogues -> return Outcome.Success(dialogues(lessons))
        }
        return itemEntries(items)
    }

    private suspend fun dueItems(): List<StudyItemId> =
        progress.observeDueCards(clock.today()).first().map { it.itemId }.filter(::isHeard).take(MAX_TRACKS)

    private fun recentItems(lessons: List<Lesson>): List<StudyItemId> = itemsOf(lessons, fromEnd = true)

    private fun itemsOf(lessons: List<Lesson>, fromEnd: Boolean = false): List<StudyItemId> {
        val items = lessons.flatMap { lesson -> lesson.introduced.map { it.id } }.filter(::isHeard).distinct()
        return if (fromEnd) items.takeLast(MAX_TRACKS) else items.take(MAX_TRACKS)
    }

    private fun dialogues(lessons: List<Lesson>): List<PlaylistEntry.Dialogue> = lessons.flatMap { lesson ->
        lesson.steps.filterIsInstance<LessonStep.Dialogue>()
            .filter { it.lines.isNotEmpty() }
            .mapIndexed { index, step -> PlaylistEntry.Dialogue(lesson.id, index, step.lines) }
    }.take(MAX_TRACKS)

    /** Every unit with a finished lesson, in course order, with only its finished lessons. */
    private suspend fun finishedUnits(): Outcome<List<FinishedUnit>, ContentError> {
        val finished = progress.observeCompletedLessons().first()
        val units = when (val read = course.getUnitLessons()) {
            is Outcome.Failure -> return read
            is Outcome.Success -> read.data
        }
        return Outcome.Success(
            units.mapNotNull { unit ->
                val lessons = unit.lessons.filter { it in finished }.map { id ->
                    when (val lesson = course.getLesson(id)) {
                        is Outcome.Failure -> return lesson
                        is Outcome.Success -> lesson.data
                    }
                }
                if (lessons.isEmpty()) null else FinishedUnit(unit.id, unit.title, lessons)
            }
        )
    }

    private suspend fun itemEntries(ids: List<StudyItemId>): Outcome<List<PlaylistEntry>, ContentError> {
        val entries = ids.mapNotNull { id ->
            when (val entry = itemEntry(id)) {
                is Outcome.Success -> entry.data
                is Outcome.Failure -> if (entry.error == ContentError.NOT_FOUND) null else return entry
            }
        }
        return Outcome.Success(entries)
    }

    private suspend fun itemEntry(id: StudyItemId): Outcome<PlaylistEntry.Item, ContentError> = when (id) {
        is SenseId -> when (val word = lexicon.getWordOf(id)) {
            is Outcome.Failure -> word

            is Outcome.Success -> when (val entry = lexicon.getEntry(word.data)) {
                is Outcome.Failure -> entry

                is Outcome.Success -> {
                    val entryWord = entry.data as? WordEntry ?: return Outcome.Failure(ContentError.NOT_FOUND)
                    entryWord.senses.firstOrNull { it.id == id }
                        ?.let { Outcome.Success(PlaylistEntry.Item(id, entryWord.text, it.meaningKo, it.examples)) }
                        ?: Outcome.Failure(ContentError.NOT_FOUND)
                }
            }
        }

        is PatternId -> when (val entry = lexicon.getEntry(id)) {
            is Outcome.Failure -> entry

            is Outcome.Success -> {
                val pattern = entry.data as? PatternEntry ?: return Outcome.Failure(ContentError.NOT_FOUND)
                Outcome.Success(PlaylistEntry.Item(id, pattern.text, pattern.meaningKo, pattern.examples))
            }
        }

        is SoundId -> Outcome.Failure(ContentError.NOT_FOUND)
    }

    private fun isHeard(id: StudyItemId): Boolean = id !is SoundId

    private class FinishedUnit(val id: UnitId, val title: LocalizedText, val lessons: List<Lesson>)
}
