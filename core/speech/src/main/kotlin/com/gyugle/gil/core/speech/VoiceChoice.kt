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
package com.gyugle.gil.core.speech

import java.util.Locale
import java.util.MissingResourceException

/** The parts of `android.speech.tts.Voice` that decide which voice to use. */
internal data class VoiceCandidate(
    val name: String,
    val locale: Locale,
    /** `Voice.QUALITY_*`: higher is better. */
    val quality: Int,
    /** `Voice.LATENCY_*`: lower is better. */
    val latency: Int,
    val needsNetwork: Boolean,
    /** False when the engine lists the voice with `KEY_FEATURE_NOT_INSTALLED`. */
    val installed: Boolean
)

/** The voice to use for one language, or null when there is none to use. */
internal data class VoiceChoice(val state: VoiceState, val voiceName: String?)

/**
 * Picks the best installed offline voice for [language]: highest quality, then lowest latency,
 * then name, so the same engine always gives the same voice.
 *
 * Engines report locales in either ISO 639-1 (`en_US`) or ISO 639-2 (`eng_USA`) form, so both
 * sides are compared in their three-letter form.
 */
internal fun chooseVoice(candidates: Collection<VoiceCandidate>, language: SpeechLanguage): VoiceChoice {
    val target = Locale.forLanguageTag(language.languageTag)
    val forLanguage = candidates.filter { it.locale.sameLanguageAndCountry(target) }
    val best = forLanguage
        .filter { it.installed && !it.needsNetwork }
        .sortedWith(compareByDescending<VoiceCandidate> { it.quality }.thenBy { it.latency }.thenBy { it.name })
        .firstOrNull()
    return when {
        best != null -> VoiceChoice(VoiceState.INSTALLED, best.name)
        forLanguage.isNotEmpty() -> VoiceChoice(VoiceState.NOT_INSTALLED, null)
        else -> VoiceChoice(VoiceState.UNSUPPORTED, null)
    }
}

private fun Locale.sameLanguageAndCountry(other: Locale): Boolean =
    iso3Language() == other.iso3Language() && iso3Country() == other.iso3Country()

private fun Locale.iso3Language(): String = try {
    isO3Language
} catch (_: MissingResourceException) {
    language
}

private fun Locale.iso3Country(): String = try {
    isO3Country
} catch (_: MissingResourceException) {
    country
}
