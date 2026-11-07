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
package com.gyugle.gil.feature.review.impl.presentation.session

/** What the learner is asked to do, which picks the instruction above the card. */
enum class CardKindUi {
    CHOOSE_MEANING,
    CHOOSE_ENGLISH,
    LISTEN_CHOOSE,
    CHOOSE_LETTER_BY_EAR,
    CHOOSE_LETTER_BY_NAME,
    FIND_SOUND,
    SPELL_BY_EAR,
    SPELL_BY_MEANING,
    FILL_BLANK
}
