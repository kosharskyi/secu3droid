package org.secu3.android.ui.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Material 3 tab styling shared by feature screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecuTabRow(
    titles: List<String>,
    selectedIndex: Int,
    selectionPosition: () -> Float = { selectedIndex.toFloat() },
    onSelected: (Int) -> Unit,
    scrollable: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    val selected = selectedIndex.coerceIn(0, (titles.size - 1).coerceAtLeast(0))
    val content: @Composable () -> Unit = {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    FractionalTabPill(index, title, titles.size, selectionPosition, "$testTag-pill-$index")
                },
            )
        }
    }
    if (scrollable) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selected,
            modifier = modifier.fillMaxWidth().testTag(testTag),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 12.dp,
            divider = {},
            indicator = {},
            tabs = content,
        )
    } else {
        PrimaryTabRow(
            selectedTabIndex = selected,
            modifier = modifier.fillMaxWidth().testTag(testTag),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            divider = {},
            indicator = {},
            tabs = content,
        )
    }
}

@Composable
private fun FractionalTabPill(
    index: Int,
    title: String,
    itemCount: Int,
    selectionPosition: () -> Float,
    testTag: String,
) {
    val currentSelectionPosition = rememberUpdatedState(selectionPosition)
    val selection by remember(index, itemCount) {
        derivedStateOf { tabPillSelectionFraction(index, currentSelectionPosition.value(), itemCount) }
    }
    val pillColor = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, selection)
    val labelColor = lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onPrimaryContainer, selection)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = pillColor,
        contentColor = labelColor,
        modifier = Modifier.testTag(testTag),
    ) {
        Text(title, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), maxLines = 2)
    }
}

/** Continuous selected-pill blend, driven by PagerState.currentPage + currentPageOffsetFraction. */
internal fun tabPillSelectionFraction(index: Int, selectionPosition: Float, itemCount: Int): Float {
    if (itemCount <= 0 || index !in 0 until itemCount) return 0f
    return (1f - abs(selectionPosition.coerceIn(0f, (itemCount - 1).toFloat()) - index)).coerceIn(0f, 1f)
}
