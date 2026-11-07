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
package com.gyugle.gil.feature.dictionary.impl.presentation.sentence

import com.gyugle.gil.feature.dictionary.impl.presentation.R

/** Sentences for previews and screenshot tests, shaped like the first lessons. */
internal object SentencePreviewData {
    val sentence = SentenceUi(
        text = "I drink coffee at home in the morning.",
        ko = "나는 아침에 집에서 커피를 마셔요.",
        words = listOf(
            SentenceWordUi(0, 1, "word.i", "I", R.string.dictionary_pos_pronoun, "나, 저"),
            SentenceWordUi(2, 7, "word.drink", "drink", R.string.dictionary_pos_verb, "마시다"),
            SentenceWordUi(8, 14, "word.coffee", "coffee", R.string.dictionary_pos_noun, "커피"),
            SentenceWordUi(18, 22, "word.home", "home", null, null),
            SentenceWordUi(30, 37, "word.morning", "morning", R.string.dictionary_pos_noun, "아침")
        ),
        chunks = listOf(
            ChunkUi("I", R.string.dictionary_role_who, "나는"),
            ChunkUi("drink", R.string.dictionary_role_do, "마신다"),
            ChunkUi("coffee", R.string.dictionary_role_what, "커피를"),
            ChunkUi("at home", R.string.dictionary_role_where, "집에서"),
            ChunkUi("in the morning", R.string.dictionary_role_when, "아침에")
        ),
        literalKo = null,
        grammar = GrammarUi(
            title = "말의 순서",
            paragraphs = listOf("영어는 누가, 한다, 무엇을 순서로 말해요. 장소와 때는 보통 끝에 와요.")
        )
    )

    /** A dictionary example: no chunks, gloss or grammar. */
    val plain = SentenceUi(
        text = "Zero, one, two.",
        ko = "영, 일, 이.",
        words = listOf(
            SentenceWordUi(0, 4, "word.zero", "zero", R.string.dictionary_pos_number, "0, 영"),
            SentenceWordUi(6, 9, "word.one", "one", R.string.dictionary_pos_number, "1, 하나"),
            SentenceWordUi(11, 14, "word.two", "two", R.string.dictionary_pos_number, "2, 둘")
        ),
        chunks = emptyList(),
        literalKo = null,
        grammar = null
    )
}
