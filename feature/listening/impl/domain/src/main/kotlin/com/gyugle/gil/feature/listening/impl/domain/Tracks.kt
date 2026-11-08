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

import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistEntry
import com.gyugle.gil.feature.listening.impl.domain.model.SpokenLanguage
import com.gyugle.gil.feature.listening.impl.domain.model.Track
import com.gyugle.gil.feature.listening.impl.domain.model.TrackPart
import kotlin.time.Duration.Companion.seconds

/** The short silence between the other parts, and at the end of a track before the next one. */
internal val GAP = 1.seconds

/** How often an item's example is heard. */
internal const val EXAMPLE_REPEATS = 2

private val SPACE_BEFORE_PUNCTUATION = Regex("""\s+([.,?!])""")
private val SPACES = Regex("""\s{2,}""")

/**
 * [text] as it is said. A pattern marks its open slot with `~`, as in "Call me ~.", which a speech
 * engine would read out as a word; the slot is left silent instead.
 */
internal fun spoken(text: String): String =
    text.replace("~", " ").replace(SPACE_BEFORE_PUNCTUATION, "$1").replace(SPACES, " ").trim()

/**
 * How [entry] is heard. Without [withKorean], for a learner whose phone has no Korean voice, the
 * Korean parts are left out and the pauses stay.
 *
 * An item: its English, a [pause] to recall the meaning, the Korean meaning, then its first example
 * [EXAMPLE_REPEATS] times. An item without an example repeats its English instead.
 *
 * A dialogue: each line in English, a [pause], its Korean translation where the content has one;
 * then the whole dialogue once more in English only, to hear it flow.
 */
fun trackOf(entry: PlaylistEntry, pause: PauseLength, withKorean: Boolean): Track = when (entry) {
    is PlaylistEntry.Item -> itemTrack(entry, pause, withKorean)
    is PlaylistEntry.Dialogue -> dialogueTrack(entry, pause, withKorean)
}

private fun itemTrack(item: PlaylistEntry.Item, pause: PauseLength, withKorean: Boolean): Track {
    val repeated = item.examples.firstOrNull()?.text ?: item.english
    val parts = buildList {
        add(TrackPart.Speech(spoken(item.english), SpokenLanguage.ENGLISH))
        add(TrackPart.Pause(pause.duration))
        if (withKorean) {
            add(TrackPart.Speech(spoken(item.korean), SpokenLanguage.KOREAN))
            add(TrackPart.Pause(GAP))
        }
        repeat(EXAMPLE_REPEATS) {
            add(TrackPart.Speech(spoken(repeated), SpokenLanguage.ENGLISH))
            add(TrackPart.Pause(GAP))
        }
    }
    return Track(id = item.id.value, title = item.english, subtitle = item.korean, parts = parts)
}

private fun dialogueTrack(dialogue: PlaylistEntry.Dialogue, pause: PauseLength, withKorean: Boolean): Track {
    val parts = buildList {
        dialogue.lines.forEach { line ->
            add(TrackPart.Speech(line.sentence.text, SpokenLanguage.ENGLISH))
            add(TrackPart.Pause(pause.duration))
            val korean = line.sentence.ko
            if (withKorean && korean != null) {
                add(TrackPart.Speech(korean, SpokenLanguage.KOREAN))
                add(TrackPart.Pause(GAP))
            }
        }
        dialogue.lines.forEach { line ->
            add(TrackPart.Speech(line.sentence.text, SpokenLanguage.ENGLISH))
            add(TrackPart.Pause(GAP))
        }
    }
    val first = dialogue.lines.first().sentence
    return Track(
        id = "${dialogue.lessonId.value}#${dialogue.index}",
        title = first.text,
        subtitle = first.ko,
        parts = parts
    )
}
