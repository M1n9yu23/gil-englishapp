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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.R as CoreR
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.PressableSurface
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.presentation.ContentErrorUi
import com.gyugle.gil.feature.curriculum.impl.presentation.R
import com.gyugle.gil.feature.curriculum.impl.presentation.component.PlaceholderBar

private object PathTokens {
    /** The line runs down the start edge; its marks sit 6dp inside the screen margin. */
    val LineCenter = 26.dp
    val ContentStart = 48.dp
    val LineStroke = 3.dp
    val MarkRadius = 4.dp
    val RingStroke = 2.dp
    val UnitRowMinHeight = 56.dp
    val BulletWidth = 12.dp
    const val PLACEHOLDER_STAGES = 3
}

@Composable
fun PathRoot(
    onUnitClick: (UnitId) -> Unit,
    onTestClick: (StageId) -> Unit,
    viewModel: PathViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PathScreen(state = state, onAction = viewModel::onAction, onUnitClick = onUnitClick, onTestClick = onTestClick)
}

@Composable
fun PathScreen(
    state: PathState,
    onAction: (PathAction) -> Unit,
    onUnitClick: (UnitId) -> Unit,
    onTestClick: (StageId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.curriculum_path_title))
        Crossfade(
            targetState = state.content(),
            animationSpec = tween(GilTheme.motion.fade),
            label = "path"
        ) { content ->
            when (content) {
                PathContent.Loading -> PathLoading()

                is PathContent.Failed -> ErrorState(
                    title = content.error.title.asString(),
                    body = content.error.body.asString(),
                    retryLabel = stringResource(R.string.curriculum_error_retry),
                    onRetry = { onAction(PathAction.OnRetryClick) }
                )

                is PathContent.Stages -> StageList(content, onUnitClick, onTestClick)
            }
        }
    }
}

/** One of the three things the screen shows; the crossfade runs when this value changes. */
private sealed interface PathContent {
    data object Loading : PathContent
    data class Failed(val error: ContentErrorUi) : PathContent

    @Immutable
    data class Stages(val stages: List<StageUi>) : PathContent
}

private fun PathState.content(): PathContent = when {
    error != null -> PathContent.Failed(error)
    isLoading -> PathContent.Loading
    else -> PathContent.Stages(stages)
}

@Composable
private fun StageList(content: PathContent.Stages, onUnitClick: (UnitId) -> Unit, onTestClick: (StageId) -> Unit) {
    val stages = content.stages
    val spacing = GilTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = spacing.itemGap, bottom = spacing.sectionGap)
    ) {
        itemsIndexed(stages, key = { _, stage -> stage.id }) { index, stage ->
            StageItem(
                stage = stage,
                isFirst = index == 0,
                isLast = index == stages.lastIndex,
                onUnitClick = onUnitClick,
                onTestClick = { onTestClick(StageId(stage.id)) }
            )
        }
    }
}

/**
 * The stage list's shape before it loads: the pale line with open rings, and a bar where each
 * title and unit will be. Read as one "loading" node.
 */
@Composable
private fun PathLoading() {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    val label = stringResource(CoreR.string.common_loading)
    val titleLine = with(LocalDensity.current) { MaterialTheme.typography.titleMedium.lineHeight.toDp() }
    val ahead = colors.primaryContainer
    val paper = colors.surface
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = spacing.itemGap)
            .clearAndSetSemantics { contentDescription = label }
    ) {
        repeat(PathTokens.PLACEHOLDER_STAGES) { index ->
            val isLast = index == PathTokens.PLACEHOLDER_STAGES - 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isLast) 0.dp else spacing.sectionGap)
                    .drawWithContent {
                        drawContent()
                        val markY = titleLine.toPx() / 2
                        if (index > 0) drawLineSegment(0f, markY, ahead)
                        if (!isLast) drawLineSegment(markY, size.height + spacing.sectionGap.toPx(), ahead)
                        drawStageMark(markY, StagePlace.AHEAD, travelled = ahead, seal = ahead, paper = paper)
                    },
                verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
            ) {
                val inset = Modifier.padding(start = PathTokens.ContentStart, end = spacing.screenMargin)
                PlaceholderBar(widthFraction = 0.4f, lineHeight = titleLine, modifier = inset)
                PlaceholderBar(widthFraction = 0.6f, lineHeight = titleLine, modifier = inset)
            }
        }
    }
}

/**
 * A stage with its units, then its test when it offers one, and the stretch of the path line
 * beside it. The line is drawn over the content so a pressed row does not cover it.
 */
@Composable
private fun StageItem(
    stage: StageUi,
    isFirst: Boolean,
    isLast: Boolean,
    onUnitClick: (UnitId) -> Unit,
    onTestClick: () -> Unit
) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    // Before the first layout, half a title line; then the middle of the title's first line as laid out.
    val fallbackMarkY = with(LocalDensity.current) { MaterialTheme.typography.titleMedium.lineHeight.toPx() / 2 }
    var titleFirstLineMiddle by remember { mutableFloatStateOf(Float.NaN) }
    val travelled = colors.primary
    val ahead = colors.primaryContainer
    val seal = colors.tertiary
    val paper = colors.surface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else spacing.sectionGap)
            .drawWithContent {
                drawContent()
                val markY = titleFirstLineMiddle.takeUnless { it.isNaN() } ?: fallbackMarkY
                if (!isFirst) drawLineSegment(0f, markY, if (stage.place == StagePlace.AHEAD) ahead else travelled)
                if (!isLast) {
                    // Runs through this item's bottom padding to the next stage's mark.
                    val end = size.height + spacing.sectionGap.toPx()
                    drawLineSegment(markY, end, if (stage.place == StagePlace.PASSED) travelled else ahead)
                }
                drawStageMark(markY, stage.place, travelled = travelled, seal = seal, paper = paper)
            }
    ) {
        StageHeader(stage, onTitleLayout = { layout ->
            titleFirstLineMiddle =
                (layout.getLineTop(0) + layout.getLineBottom(0)) / 2
        })
        if (stage.units.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = spacing.inlineGap)) {
                stage.units.forEachIndexed { index, unit ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = PathTokens.ContentStart))
                    UnitRow(unit, onClick = { onUnitClick(unit.id) })
                }
                stage.testQuestions?.let { questions ->
                    HorizontalDivider(modifier = Modifier.padding(start = PathTokens.ContentStart))
                    TestRow(questions, onClick = onTestClick)
                }
            }
        }
    }
}

@Composable
private fun StageHeader(stage: StageUi, onTitleLayout: (TextLayoutResult) -> Unit) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    // The title must stay the first thing in the item: the mark is drawn at its first line.
    Column(
        modifier = Modifier.padding(start = PathTokens.ContentStart, end = spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)
    ) {
        // The title and its status are one heading for screen readers.
        Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
            Text(
                text = stage.title.asString(),
                onTextLayout = onTitleLayout,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            if (stage.place == StagePlace.CURRENT) {
                Text(
                    text = stringResource(R.string.curriculum_path_current_stage),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.tertiary
                )
            }
            if (stage.units.isEmpty()) {
                Text(
                    text = stringResource(R.string.curriculum_path_stage_not_ready),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        if (stage.canDo.isNotEmpty()) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    text = stringResource(R.string.curriculum_path_can_do_heading),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                stage.canDo.forEach { line ->
                    // The dot sits apart so a wrapped line starts under the text, not under the dot.
                    Row {
                        Text(
                            text = "·",
                            // Screen readers would say "middle dot" before every line.
                            modifier = Modifier.width(PathTokens.BulletWidth).clearAndSetSemantics {},
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = line.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitRow(unit: UnitRowUi, onClick: () -> Unit) {
    val spacing = GilTheme.spacing
    PressableSurface(onClick = onClick, clickLabel = stringResource(R.string.curriculum_path_open_unit)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PathTokens.UnitRowMinHeight)
                .padding(
                    start = PathTokens.ContentStart,
                    end = spacing.screenMargin,
                    top = spacing.itemGap,
                    bottom = spacing.itemGap
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unit.title.asString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.curriculum_unit_lesson_count,
                        unit.lessonCount,
                        unit.lessonCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = GilIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The way to skip a stage the learner already knows, after its units and set apart by ink colour. */
@Composable
private fun TestRow(questions: Int, onClick: () -> Unit) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    PressableSurface(onClick = onClick, clickLabel = stringResource(R.string.curriculum_path_start_test)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PathTokens.UnitRowMinHeight)
                .padding(
                    start = PathTokens.ContentStart,
                    end = spacing.screenMargin,
                    top = spacing.itemGap,
                    bottom = spacing.itemGap
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.curriculum_path_test),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primary
                )
                Text(
                    text = pluralStringResource(R.plurals.curriculum_path_test_detail, questions, questions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
            Icon(painter = GilIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

private fun DrawScope.lineX(): Float {
    val x = PathTokens.LineCenter.toPx()
    return if (layoutDirection == LayoutDirection.Rtl) size.width - x else x
}

private fun DrawScope.drawLineSegment(startY: Float, endY: Float, color: Color) {
    val x = lineX()
    drawLine(color, Offset(x, startY), Offset(x, endY), PathTokens.LineStroke.toPx(), StrokeCap.Butt)
}

/** Passed stages get an ink dot, the current one the seal dot, and stages ahead an open ring. */
private fun DrawScope.drawStageMark(y: Float, place: StagePlace, travelled: Color, seal: Color, paper: Color) {
    val center = Offset(lineX(), y)
    val radius = PathTokens.MarkRadius.toPx()
    when (place) {
        StagePlace.PASSED -> drawCircle(travelled, radius, center)

        StagePlace.CURRENT -> drawCircle(seal, radius, center)

        StagePlace.AHEAD -> {
            drawCircle(paper, radius, center)
            drawCircle(
                travelled,
                radius - PathTokens.RingStroke.toPx() / 2,
                center,
                style = Stroke(PathTokens.RingStroke.toPx())
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PathScreenPreview() {
    GilTheme {
        Surface {
            PathScreen(
                state = PathState(isLoading = false, stages = PathPreviewData.stages),
                onAction = {},
                onUnitClick = {},
                onTestClick = {}
            )
        }
    }
}
