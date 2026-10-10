package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.pinbomClick

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · board row
 *  56dp cover thumbnail, title, “N پین” and a trailing slot (a “ذخیره” pill, a
 *  check when already saved, or a menu). Used by the save sheet, board lists
 *  and the profile.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun BoardRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinCountLabel: String? = null,
    selected: Boolean = false,
    covers: List<String> = emptyList(),
    seed: Long = 0L,
    baseUrl: String = "",
    isPrivate: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = PinBomTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pinbomClick(onClick = onClick, pressedScale = 0.99f, indication = ripple())
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
        } else {
            BoardCoverCollage(
                covers = covers,
                seed = seed,
                baseUrl = baseUrl,
                shape = PinBomShape.thumb,
                contentDescription = null,
                modifier = Modifier.size(Dimens.boardThumb),
            )
        }

        Spacer(Modifier.width(Dimens.md))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContentText(
                    text = title,
                    style = PinBomText.button,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPrivate) {
                    Spacer(Modifier.width(Dimens.xs))
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
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

        if (selected) {
            Spacer(Modifier.width(Dimens.sm))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(20.dp),
            )
        }

        if (trailing != null) {
            Spacer(Modifier.width(Dimens.sm))
            trailing()
        }
    }
}

/** Row with a grey “+” disc — “ساخت بورد جدید” in the save sheet. */
@Composable
fun BoardCreateRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PinBomTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pinbomClick(onClick = onClick, pressedScale = 0.99f, indication = ripple())
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.boardThumb)
                .background(colors.surfaceGrey, PinBomShape.thumb),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(Dimens.md))
        Text(
            text = title,
            style = PinBomText.button,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun BoardRowGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(PinBomTheme.colors.background)
                .padding(Dimens.screen),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            BoardRow(title = "دکوراسیون خانه", pinCountLabel = "۱۲ پین", seed = 1, onClick = {})
            BoardRow(title = "دستور پخت", pinCountLabel = "۴۸ پین", seed = 5, onClick = {}, selected = true)
            BoardRow(title = "بورد خصوصی من", pinCountLabel = "۳ پین", seed = 8, isPrivate = true, onClick = {})
            BoardCreateRow(title = "ساخت بورد جدید", onClick = {})
        }
    }
}

@Preview(name = "BoardRow · light", backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PreviewBoardRowLight() = BoardRowGallery(dark = false)

@Preview(name = "BoardRow · dark", backgroundColor = 0xFF121212, widthDp = 400)
@Composable
private fun PreviewBoardRowDark() = BoardRowGallery(dark = true)
