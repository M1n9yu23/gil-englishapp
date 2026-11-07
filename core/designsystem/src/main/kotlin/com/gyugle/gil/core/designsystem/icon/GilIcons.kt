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
 * at the framework attribute, because this module has no AppCompat theme.
 */
object GilIcons {
    val ArrowBack: Painter
        @Composable get() = painterResource(R.drawable.ic_arrow_back)

    val Check: Painter
        @Composable get() = painterResource(R.drawable.ic_check)

    val ChevronRight: Painter
        @Composable get() = painterResource(R.drawable.ic_chevron_right)

    val Close: Painter
        @Composable get() = painterResource(R.drawable.ic_close)
}
