package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.skeleton

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · skeletons
 *  Deterministic, varied heights so the first paint already feels like a
 *  masonry feed instead of a block of grey rectangles. The shimmer sweep keeps
 *  it alive; with reduced motion the blocks stay flat and calm.
 * ──────────────────────────────────────────────────────────────────────────── */

private val SKELETON_RATIOS = listOf(0.72f, 1.35f, 0.85f, 1.12f, 0.66f, 1.5f, 0.95f, 1.25f, 0.78f, 1.42f)

@Composable
fun PinGridSkeleton(
    modifier: Modifier = Modifier,
    columns: StaggeredGridCells = StaggeredGridCells.Fixed(2),
    itemCount: Int = 6,
    contentPadding: PaddingValues = PaddingValues(Dimens.grid),
    horizontalGap: Dp = Dimens.gridGap,
    verticalGap: Dp = Dimens.gridGap,
) {
    LazyVerticalStaggeredGrid(
        columns = columns,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
        verticalItemSpacing = verticalGap,
        userScrollEnabled = false,
    ) {
        items(count = itemCount, key = { "skeleton-$it" }) { index ->
            PinSkeletonTile(ratio = SKELETON_RATIOS[index % SKELETON_RATIOS.size])
        }
    }
}

/** A single placeholder tile: image block + two text lines + an author row. */
@Composable
fun PinSkeletonTile(
    modifier: Modifier = Modifier,
    ratio: Float = 1f,
    showAuthor: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp * ratio)
                .skeleton(PinBomShape.pin),
        )
        Spacer(Modifier.height(Dimens.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(end = 24.dp)
                .skeleton(PinBomShape.pill),
        )
        Spacer(Modifier.height(Dimens.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .skeleton(PinBomShape.pill),
        )
        if (showAuthor) {
            Spacer(Modifier.height(Dimens.sm))
            Box(Modifier.size(20.dp).skeleton(CircleShape))
        }
    }
}

/** Generic shimmering line — used by lists, notification rows and boards. */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .skeleton(PinBomShape.pill),
    )
}

/** Row-shaped skeleton: avatar + two lines + trailing block. */
@Composable
fun ListRowSkeleton(modifier: Modifier = Modifier, avatarSize: Dp = 48.dp) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(avatarSize).skeleton(CircleShape))
        Spacer(Modifier.size(Dimens.md))
        Column(Modifier.weight(1f)) {
            SkeletonLine(widthFraction = 0.55f)
            Spacer(Modifier.height(Dimens.sm))
            SkeletonLine(widthFraction = 0.85f, height = 10.dp)
        }
        Spacer(Modifier.size(Dimens.md))
        Box(Modifier.size(width = 48.dp, height = 48.dp).skeleton(PinBomShape.thumb))
    }
}

@Preview(name = "Grid skeleton · light", backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewSkeletonLight() {
    PinBomTheme(darkTheme = false) {
        Box(Modifier.background(PinBomTheme.colors.background)) {
            PinGridSkeleton(itemCount = 6)
        }
    }
}

@Preview(name = "Grid skeleton · dark", backgroundColor = 0xFF121212, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewSkeletonDark() {
    PinBomTheme(darkTheme = true) {
        Box(Modifier.background(PinBomTheme.colors.background)) {
            PinGridSkeleton(itemCount = 6)
        }
    }
}

@Preview(name = "Row skeleton", backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun PreviewRowSkeleton() {
    PinBomTheme(darkTheme = false) {
        Column(Modifier.background(PinBomTheme.colors.background)) {
            ListRowSkeleton()
            ListRowSkeleton()
        }
    }
}
