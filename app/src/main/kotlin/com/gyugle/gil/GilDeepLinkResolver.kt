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
package com.gyugle.gil

import com.gyugle.gil.core.navigation.DeepLinkResolver
import com.gyugle.gil.core.navigation.DeepLinkTarget
import com.gyugle.gil.feature.curriculum.api.EntryId
import com.gyugle.gil.feature.dictionary.api.EntryRoute
import com.gyugle.gil.feature.dictionary.api.SentenceLink
import com.gyugle.gil.feature.dictionary.api.SentenceRoute
import com.gyugle.gil.feature.dictionary.api.WordsRoute
import com.gyugle.gil.feature.home.api.CONTINUE_DEEP_LINK
import com.gyugle.gil.feature.home.api.ContinueFinder
import com.gyugle.gil.feature.home.api.TODAY_DEEP_LINK
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.listening.api.LISTENING_DEEP_LINK
import com.gyugle.gil.feature.listening.api.ListeningPlayerRoute
import com.gyugle.gil.feature.review.api.ALL_DUE_CARDS
import com.gyugle.gil.feature.review.api.REVIEW_DEEP_LINK
import com.gyugle.gil.feature.review.api.ReviewSessionRoute
import javax.inject.Inject

/**
 * Every outside entry that opens a screen. Fixed links are matched as whole strings; the one link
 * with ids in it is read by its own parser, which accepts nothing but its exact shape.
 */
class GilDeepLinkResolver @Inject constructor(private val continueFinder: ContinueFinder) : DeepLinkResolver {
    override suspend fun resolve(uri: String): DeepLinkTarget? = when (uri) {
        TODAY_DEEP_LINK -> DeepLinkTarget(TodayRoute)

        // Reviews, lessons and the player are all opened from the Today tab, so back goes there.
        CONTINUE_DEEP_LINK -> DeepLinkTarget(TodayRoute, listOfNotNull(continueFinder.find()))

        REVIEW_DEEP_LINK -> DeepLinkTarget(TodayRoute, listOf(ReviewSessionRoute(ALL_DUE_CARDS)))

        LISTENING_DEEP_LINK -> DeepLinkTarget(TodayRoute, listOf(ListeningPlayerRoute))

        else -> SentenceLink.parse(uri)?.let(::sentence)
    }

    /** Over the entry's page, as when the sentence is opened from there. */
    private fun sentence(link: SentenceLink): DeepLinkTarget? {
        val entryId = EntryId.parse(link.entryId) ?: return null
        return DeepLinkTarget(
            WordsRoute,
            listOf(EntryRoute(entryId.value), SentenceRoute(link.sentenceId, source = entryId.value))
        )
    }
}
