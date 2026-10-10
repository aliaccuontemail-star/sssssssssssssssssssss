package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.pinbom.app.R
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.pinbomClick

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · board cover collage
 *  One tall cover on the start side, two stacked tiles on the end side.
 *  Because it is a Row, RTL mirrors it for free. Missing pins become pastel
 *  tiles so a brand-new board still looks deliberate, never broken.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun BoardCoverCollage(
    covers: List<String>,
    modifier: Modifier = Modifier,
    seed: Long = 0L,
    baseUrl: String = "",
    shape: Shape = PinBomShape.boardCover,
    gap: Dp = 2.dp,
    contentDescription: String? = stringResource(R.string.cd_board_cover),
) {
    val urls = covers.take(3)

    Box(
        modifier = modifier
            .clip(shape)
            .background(PinBomTheme.colors.surfaceGrey),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Big tile (start side; mirrors to the right in RTL).
            PinImage(
                url = urls.getOrNull(0).orEmpty(),
                seed = seed,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.62f),
                shape = RectangleShape,
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.width(gap))

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                PinImage(
                    url = urls.getOrNull(1).orEmpty(),
                    seed = seed + 1,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RectangleShape,
                    contentScale = ContentScale.Crop,
                )
                PinImage(
                    url = urls.getOrNull(2).orEmpty(),
                    seed = seed + 2,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RectangleShape,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/** Card wrapper: collage + title + “N پین”, used in board grids. */
@Composable
fun BoardCoverCard(
    title: String,
    pinCountLabel: String?,
    covers: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    seed: Long = 0L,
    baseUrl: String = "",
    aspectRatio: Float = 0.92f,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = PinBomTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pinbomClick(onClick = onClick, pressedScale = 0.98f),
    ) {
        BoardCoverCollage(
            covers = covers,
            seed = seed,
            baseUrl = baseUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
        )
        Spacer(Modifier.height(Dimens.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = PinBomText.pinTitle,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) trailing()
        }
        if (pinCountLabel != null) {
            Text(
                text = pinCountLabel,
                style = PinBomText.caption,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun CollageGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(PinBomTheme.colors.background)
                .padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg),
        ) {
            BoardCoverCollage(
                covers = emptyList(),
                seed = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                BoardCoverCard(
                    title = "دکوراسیون خانه",
                    pinCountLabel = "۱۲ پین",
                    covers = emptyList(),
                    seed = 9,
                    onClick = {},
                    modifier = Modifier.width(150.dp),
                )
                BoardCoverCard(
                    title = "دستور پخت",
                    pinCountLabel = "۴۸ پین",
                    covers = emptyList(),
                    seed = 12,
                    onClick = {},
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Preview(name = "Board collage · light", backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PreviewCollageLight() = CollageGallery(dark = false)

@Preview(name = "Board collage · dark", backgroundColor = 0xFF121212, widthDp = 400)
@Composable
private fun PreviewCollageDark() = CollageGallery(dark = true)
