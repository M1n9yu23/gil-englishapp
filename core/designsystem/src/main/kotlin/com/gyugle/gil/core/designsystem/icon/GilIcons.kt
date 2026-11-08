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
package com.gyugle.gil.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.gyugle.gil.core.designsystem.R

/**
 * Material Symbols Outlined, weight 400, 24dp. Path data is copied unchanged; only the tint points
 * at the framework attribute, because this module has no AppCompat theme. A `Filled` icon is the
 * same symbol with fill 1, for the selected state, and for media controls, which read better solid.
 */
object GilIcons {
    val Analytics: Painter
        @Composable get() = painterResource(R.drawable.ic_analytics)

    val AnalyticsFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_analytics_filled)

    val ArrowBack: Painter
        @Composable get() = painterResource(R.drawable.ic_arrow_back)

    val Bookmark: Painter
        @Composable get() = painterResource(R.drawable.ic_bookmark)

    val BookmarkFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_bookmark_filled)

    val Check: Painter
        @Composable get() = painterResource(R.drawable.ic_check)

    val ChevronLeft: Painter
        @Composable get() = painterResource(R.drawable.ic_chevron_left)

    val ChevronRight: Painter
        @Composable get() = painterResource(R.drawable.ic_chevron_right)

    val Close: Painter
        @Composable get() = painterResource(R.drawable.ic_close)

    val Dictionary: Painter
        @Composable get() = painterResource(R.drawable.ic_dictionary)

    val DictionaryFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_dictionary_filled)

    val ExpandMore: Painter
        @Composable get() = painterResource(R.drawable.ic_expand_more)

    val MoreVert: Painter
        @Composable get() = painterResource(R.drawable.ic_more_vert)

    val PauseFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_pause_filled)

    val PlayFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_play_filled)

    val Route: Painter
        @Composable get() = painterResource(R.drawable.ic_route)

    val RouteFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_route_filled)

    val Search: Painter
        @Composable get() = painterResource(R.drawable.ic_search)

    val Settings: Painter
        @Composable get() = painterResource(R.drawable.ic_settings)

    val SkipNextFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_skip_next_filled)

    val SkipPreviousFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_skip_previous_filled)

    val Today: Painter
        @Composable get() = painterResource(R.drawable.ic_today)

    val TodayFilled: Painter
        @Composable get() = painterResource(R.drawable.ic_today_filled)

    val VolumeUp: Painter
        @Composable get() = painterResource(R.drawable.ic_volume_up)
}
