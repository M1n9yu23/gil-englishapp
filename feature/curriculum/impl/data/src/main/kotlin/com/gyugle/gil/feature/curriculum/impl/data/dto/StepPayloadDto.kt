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
package com.gyugle.gil.feature.curriculum.impl.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The JSON in `lesson_step.payload`, written by tools/content-compiler (`StepPayload`). The two must
 * have the same shape; the database tests decode every step of the shipped content to check it.
 */
@Serializable
sealed interface StepPayloadDto {
    @Serializable
    @SerialName("explain")
    data class Explain(val paragraphs: List<String>, val examples: List<SentenceRefDto>) : StepPayloadDto

    @Serializable
    @SerialName("listen")
    data class Listen(val sentence: SentenceRefDto) : StepPayloadDto

    @Serializable
    @SerialName("chooseMeaning")
    data class ChooseMeaning(val prompt: SentenceRefDto, val options: List<OptionDto>, val item: String? = null) :
        StepPayloadDto

    @Serializable
    @SerialName("listenChoose")
    data class ListenChoose(val audio: SentenceRefDto, val options: List<OptionDto>, val item: String? = null) :
        StepPayloadDto

    @Serializable
    @SerialName("chooseEnglish")
    data class ChooseEnglish(val ko: String, val options: List<OptionDto>, val item: String? = null) : StepPayloadDto

    @Serializable
    @SerialName("arrange")
    data class Arrange(
        val ko: String,
        val answer: SentenceRefDto,
        val tiles: List<String>,
        val accepted: List<List<String>>,
        val extra: List<String>,
        val item: String? = null
    ) : StepPayloadDto

    @Serializable
    @SerialName("fillBlank")
    data class FillBlank(
        val text: String,
        val ko: String,
        val answers: List<String>,
        val options: List<OptionDto>,
        val sentence: SentenceRefDto,
        val item: String? = null
    ) : StepPayloadDto

    @Serializable
    @SerialName("spell")
    data class Spell(val item: String, val wordId: String, val answer: String, val ko: String, val ipa: String) :
        StepPayloadDto

    @Serializable
    @SerialName("dictation")
    data class Dictation(val sentence: SentenceRefDto, val item: String? = null) : StepPayloadDto

    @Serializable
    @SerialName("minimalPair")
    data class MinimalPair(
        val words: List<PairWordDto>,
        val answer: Int,
        val why: String? = null,
        val item: String? = null
    ) : StepPayloadDto

    @Serializable
    @SerialName("shadow")
    data class Shadow(val sentence: SentenceRefDto) : StepPayloadDto

    @Serializable
    @SerialName("readPassage")
    data class ReadPassage(val sentences: List<SentenceRefDto>, val questions: List<QuestionDto>) : StepPayloadDto

    @Serializable
    @SerialName("dialogue")
    data class Dialogue(
        val lines: List<LineDto>,
        val question: String? = null,
        val options: List<OptionDto>,
        val item: String? = null
    ) : StepPayloadDto
}

@Serializable
data class SentenceRefDto(val id: Long, val text: String, val ko: String? = null)

@Serializable
data class OptionDto(val text: String, val english: Boolean, val correct: Boolean, val why: String? = null)

@Serializable
data class PairWordDto(val text: String, val ipa: String)

@Serializable
data class QuestionDto(val ko: String, val options: List<OptionDto>)

@Serializable
data class LineDto(val speaker: String, val sentence: SentenceRefDto)

/** Strict about unknown keys, so a compiler change the app has not followed fails the tests. */
@OptIn(ExperimentalSerializationApi::class)
val StepPayloadJson = Json {
    classDiscriminator = "type"
    explicitNulls = false
}
