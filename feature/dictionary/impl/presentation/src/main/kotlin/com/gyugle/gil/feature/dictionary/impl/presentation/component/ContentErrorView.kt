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
package com.gyugle.gil.feature.dictionary.impl.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.feature.dictionary.impl.presentation.ContentErrorUi
import com.gyugle.gil.feature.dictionary.impl.presentation.R

/**
 * A content error with a retry, or, for an entry that is gone, the way back. Without [onBack], as
 * on a tab root, it always offers the retry.
 */
@Composable
internal fun ContentErrorView(
    error: ContentErrorUi,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val back = onBack.takeUnless { error.canRetry }
    ErrorState(
        title = error.title.asString(),
        body = error.body.asString(),
        retryLabel = stringResource(
            if (back ==
                null
            ) {
                R.string.dictionary_error_retry
            } else {
                R.string.dictionary_error_back
            }
        ),
        onRetry = back ?: onRetry,
        modifier = modifier
    )
}
