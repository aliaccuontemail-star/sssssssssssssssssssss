package ir.pinbom.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.pressScale

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · chips
 *  36dp pills. Unselected = grey fill with primary text; selected = inverted
 *  (black on light, white on dark) with a spring colour crossfade, so switching
 *  a filter feels like a physical toggle rather than a repaint.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun PinBomChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = PinBomTheme.colors
    val interaction = remember { MutableInteractionSource() }

    val container by animateColorAsState(
        targetValue = if (selected) colors.chipSelectedBg else colors.surfaceGrey,
        animationSpec = Motion.colorShort,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> colors.textTertiary
            selected -> colors.chipSelectedText
            else -> colors.textPrimary
        },
        animationSpec = Motion.colorShort,
        label = "chipContent",
    )

    Row(
        modifier = modifier
            .height(Dimens.chip)
            .clip(PinBomShape.pill)
            .background(container)
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = content),
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = contentDescription,
                tint = content,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Dimens.xs))
        }
        Text(
            text = text,
            style = PinBomText.button,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(Dimens.xs))
            trailingIcon()
        }
    }
}

/** Horizontally scrolling chip row (feed filters, search tabs, suggestions). */
@Composable
fun PinBomChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Dimens.screen),
    spacing: Dp = Dimens.sm,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items(
            count = labels.size,
            key = { index -> "$index-${labels[index]}" },
            contentType = { "chip" },
        ) { index ->
            PinBomChip(
                text = labels[index],
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.widthIn(max = 180.dp),
            )
        }
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun ChipsGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(PinBomTheme.colors.background)
                .padding(vertical = Dimens.lg)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            PinBomChipRow(
                labels = listOf("برای تو", "دنبال‌شده‌ها", "ترند", "دکوراسیون", "غذا"),
                selectedIndex = 0,
                onSelect = {},
            )
            Box(Modifier.padding(horizontal = Dimens.screen)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    PinBomChip(text = "پین‌ها", selected = true, onClick = {})
                    PinBomChip(text = "بوردها", selected = false, onClick = {})
                    PinBomChip(text = "کاربران", selected = false, onClick = {})
                    PinBomChip(text = "غیرفعال", selected = false, onClick = {}, enabled = false)
                }
            }
        }
    }
}

@Preview(name = "Chips · light", backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PreviewChipsLight() = ChipsGallery(dark = false)

@Preview(name = "Chips · dark", backgroundColor = 0xFF121212, widthDp = 400)
@Composable
private fun PreviewChipsDark() = ChipsGallery(dark = true)

/** Chip with a live label, kept here so the string resource is exercised. */
@Preview(name = "Chip · tag", backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewTagChip() {
    PinBomTheme(darkTheme = false) {
        Box(Modifier.padding(Dimens.lg)) {
            PinBomChip(
                text = "گیاهان آپارتمانی",
                selected = false,
                onClick = {},
                contentDescription = stringResource(R.string.cd_filter_chip, "گیاهان"),
            )
        }
    }
}
