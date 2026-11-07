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
package com.gyugle.gil.feature.dictionary.impl.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.navigation.BottomSheetSceneStrategy
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.feature.dictionary.api.EntryRoute
import com.gyugle.gil.feature.dictionary.api.SentenceRoute
import com.gyugle.gil.feature.dictionary.api.WordsRoute
import com.gyugle.gil.feature.dictionary.impl.presentation.entry.EntryRoot
import com.gyugle.gil.feature.dictionary.impl.presentation.sentence.SentenceRoot
import com.gyugle.gil.feature.dictionary.impl.presentation.words.WordsRoot

/**
 * The Words tab, entry pages and the sentence sheet. Pages link to each other, so opening one goes
 * back to it when it is already in the stack instead of stacking a second copy.
 */
fun EntryProviderScope<NavKey>.dictionaryEntries(navigator: Navigator) {
    entry<WordsRoute> {
        WordsRoot(onOpenEntry = { id -> navigator.goToOrBackTo(EntryRoute(id)) })
    }
    entry<EntryRoute> { route ->
        EntryRoot(
            entryId = route.entryId,
            onBack = navigator::goBack,
            onOpenEntry = { id -> navigator.goToOrBackTo(EntryRoute(id)) },
            onOpenSentence = { sentenceId, entryId -> navigator.goTo(SentenceRoute(sentenceId, source = entryId)) }
        )
    }
    entry<SentenceRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { route ->
        SentenceRoot(
            sentenceId = route.sentenceId,
            source = route.source,
            onBack = navigator::goBack,
            // The word of the page below the sheet is already open; closing the sheet shows it.
            onOpenEntry = { id ->
                if (id ==
                    route.source
                ) {
                    navigator.goBack()
                } else {
                    navigator.goToOrBackTo(EntryRoute(id))
                }
            }
        )
    }
}
