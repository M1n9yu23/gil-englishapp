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
package com.gyugle.gil.feature.dictionary.impl.presentation.words

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.EmptyState
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ContentErrorView
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ItemRow
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ItemRowUi
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ListLoading

private object WordsTokens {
    // A tab pads its label by 16dp; this puts the first label on the 20dp screen margin.
    val TabEdgePadding = 4.dp
}

@Composable
internal fun WordsRoot(onOpenEntry: (String) -> Unit, viewModel: WordsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WordsScreen(state = state, onAction = viewModel::onAction, onOpenEntry = onOpenEntry)
}

@Composable
internal fun WordsScreen(
    state: WordsState,
    onAction: (WordsAction) -> Unit,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.dictionary_words_title))
        // Tabs take the width of their labels, so a long label at a large font scrolls instead of
        // breaking mid-word.
        PrimaryScrollableTabRow(
            selectedTabIndex = state.tab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = WordsTokens.TabEdgePadding
        ) {
            WordsTab.entries.forEach { tab ->
                Tab(
                    selected = tab == state.tab,
                    onClick = { onAction(WordsAction.OnTabSelect(tab)) },
                    text = { Text(stringResource(tab.label())) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Crossfade(targetState = state.tab, animationSpec = tween(GilTheme.motion.fade), label = "words tab") { tab ->
            val retry = { onAction(WordsAction.OnRetryClick) }
            when (tab) {
                WordsTab.SAVED -> WordList(
                    list = state.saved,
                    onOpenEntry = onOpenEntry,
                    onRetry = retry,
                    empty = {
                        EmptyState(
                            title = stringResource(R.string.dictionary_saved_empty_title),
                            body = stringResource(R.string.dictionary_saved_empty_body),
                            actionLabel = stringResource(R.string.dictionary_saved_empty_action),
                            onAction = { onAction(WordsAction.OnTabSelect(WordsTab.SEARCH)) }
                        )
                    }
                )

                WordsTab.SEARCH -> SearchPane(state.query, state.search, onAction, onOpenEntry)

                WordsTab.MISTAKES -> WordList(
                    list = state.mistakes,
                    onOpenEntry = onOpenEntry,
                    onRetry = retry,
                    empty = {
                        EmptyState(
                            title = stringResource(R.string.dictionary_mistakes_empty_title),
                            body = stringResource(R.string.dictionary_mistakes_empty_body)
                        )
                    }
                )
            }
        }
    }
}

private fun WordsTab.label(): Int = when (this) {
    WordsTab.SAVED -> R.string.dictionary_tab_saved
    WordsTab.SEARCH -> R.string.dictionary_tab_search
    WordsTab.MISTAKES -> R.string.dictionary_tab_mistakes
}

@Composable
private fun WordList(
    list: WordListUi,
    onOpenEntry: (String) -> Unit,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit
) {
    when (list) {
        WordListUi.Loading -> ListLoading()

        is WordListUi.Failed -> ContentErrorView(error = list.error, onRetry = onRetry)

        is WordListUi.Loaded -> if (list.rows.isEmpty()) {
            empty()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) { rows(list.rows, onOpenEntry) }
        }
    }
}

private fun LazyListScope.rows(rows: List<ItemRowUi>, onOpenEntry: (String) -> Unit) {
    itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
        if (index > 0) HorizontalDivider()
        ItemRow(item = row, onClick = { onOpenEntry(row.id) })
    }
}

@Composable
private fun SearchPane(
    query: String,
    search: SearchUi,
    onAction: (WordsAction) -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val spacing = GilTheme.spacing
    val focusManager = LocalFocusManager.current
    // The results end above the keyboard, so the last ones can still be scrolled into view.
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        OutlinedTextField(
            value = query,
            onValueChange = { onAction(WordsAction.OnQueryChange(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
            label = { Text(stringResource(R.string.dictionary_search_label)) },
            leadingIcon = { Icon(painter = GilIcons.Search, contentDescription = null) },
            trailingIcon = if (query.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { onAction(WordsAction.OnQueryClear) }) {
                        Icon(
                            painter = GilIcons.Close,
                            contentDescription = stringResource(R.string.dictionary_search_clear)
                        )
                    }
                }
            },
            // English or Korean may be typed, so neither language is forced on the keyboard.
            textStyle = MaterialTheme.typography.bodyLarge.copy(localeList = LocaleList.current),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
        when (search) {
            SearchUi.Idle -> Text(
                text = stringResource(R.string.dictionary_search_hint),
                modifier = Modifier.padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            is SearchUi.NoResults -> EmptyState(
                title = stringResource(R.string.dictionary_search_empty_title, search.query),
                body = stringResource(R.string.dictionary_search_empty_body)
            )

            is SearchUi.Failed -> ContentErrorView(error = search.error, onRetry = {
                onAction(WordsAction.OnRetryClick)
            })

            is SearchUi.Results -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.sectionGap)
            ) {
                rows(search.learned, onOpenEntry)
                if (search.notLearned.isNotEmpty()) {
                    item(key = "not learned") {
                        Text(
                            text = stringResource(R.string.dictionary_search_not_learned),
                            modifier = Modifier
                                .padding(
                                    start = spacing.screenMargin,
                                    end = spacing.screenMargin,
                                    top = if (search.learned.isEmpty()) spacing.inlineGap else spacing.sectionGap,
                                    bottom = spacing.inlineGap
                                )
                                .semantics { heading() },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    rows(search.notLearned, onOpenEntry)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WordsScreenPreview() {
    GilTheme {
        Surface {
            WordsScreen(
                state = WordsState(saved = WordListUi.Loaded(WordsPreviewData.saved)),
                onAction = {},
                onOpenEntry = {}
            )
        }
    }
}
