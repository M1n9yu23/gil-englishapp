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
import com.gyugle.gil.feature.home.api.TODAY_DEEP_LINK
import com.gyugle.gil.feature.home.api.TodayRoute
import javax.inject.Inject

/** Every outside entry that opens a screen, matched as a whole string: no part of it is parsed yet. */
class GilDeepLinkResolver @Inject constructor() : DeepLinkResolver {
    override fun resolve(uri: String): DeepLinkTarget? = when (uri) {
        TODAY_DEEP_LINK -> DeepLinkTarget(TodayRoute)
        else -> null
    }
}
