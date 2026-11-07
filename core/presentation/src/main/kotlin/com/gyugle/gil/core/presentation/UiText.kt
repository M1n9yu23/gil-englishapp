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
package com.gyugle.gil.core.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/**
 * Text for the screen that a ViewModel can hold without a `Context`.
 *
 * Data classes with a `List` for the arguments, so two equal messages compare equal and a
 * `StateFlow` does not re-emit the same error.
 */
@Immutable
sealed interface UiText {
    @Immutable
    data class DynamicString(val value: String) : UiText

    @Immutable
    data class StringResource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Prefer this inside composition: it follows a change of app language. */
    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id, *args.toTypedArray())
    }

    /** For places with no composition, such as a snackbar handler or a notification. */
    fun resolve(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(id, *args.toTypedArray())
    }
}
