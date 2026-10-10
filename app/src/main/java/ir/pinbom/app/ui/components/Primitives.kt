package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.res.stringResource
import ir.pinbom.app.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Localizer

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · primitives
 *  The small vocabulary every screen shares: section headers, hairlines, count
 *  and time text (Persian or English, following the app language), badges and the
 *  scroll-driven top bar slot.
 * ──────────────────────────────────────────────────────────────────────────── */

/** Section title with an optional trailing action (“مشاهده همه”). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = PinBomTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = PinBomText.title,
                color = colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PinBomText.caption,
                    color = colors.textSecondary,
                )
            }
        }
        if (action != null) action()
    }
}

/** 1dp hairline. Never a divider item with its own height — just draw it. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = PinBomTheme.colors.divider,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

/** 8dp red dot — unread notifications, unread tabs. */
@Composable
fun BadgeDot(
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    color: Color = PinBomTheme.colors.primary,
    borderColor: Color? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (borderColor != null) {
                    Modifier.background(borderColor)
                } else {
                    Modifier
                },
            ),
    )
}

/** Compact count: ۱٫۲ هزار / ۱٬۲۰۰ in Persian, 1.2K / 1,200 in English. */
@Composable
fun CountText(
    count: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = PinBomTheme.colors.textSecondary,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(Dimens.xxs))
        }
        Text(
            text = Localizer.compact(count),
            style = PinBomText.caption,
            color = color,
        )
    }
}

/** Relative time: «همین حالا» / “Just now”, «۳ دقیقه پیش» / “3 min ago”. */
@Composable
fun RelativeTimeText(
    iso: String?,
    modifier: Modifier = Modifier,
    color: Color = PinBomTheme.colors.textTertiary,
) {
    Text(
        text = Localizer.relativeTime(iso),
        style = PinBomText.caption,
        color = color,
        modifier = modifier,
    )
}

/** “۱۲ پین” / “12 pins” label. */
@Composable
fun PinCountText(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = PinBomTheme.colors.textSecondary,
    pattern: String = stringResource(R.string.count_pins),
) {
    Text(
        text = pattern.format(Localizer.number(count)),
        style = PinBomText.caption,
        color = color,
        modifier = modifier,
    )
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun PrimitivesGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(PinBomTheme.colors.background)
                .fillMaxWidth(),
        ) {
            SectionHeader(
                title = "ایده‌های مرتبط",
                subtitle = "بر اساس علاقه‌های شما",
                action = { TextPillButton(text = "مشاهده همه", onClick = {}) },
            )
            Hairline()
            Row(
                Modifier.padding(Dimens.lg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountText(count = 1200, icon = Icons.Outlined.Star)
                CountText(count = 1_500_000)
                CountText(count = 87)
                BadgeDot()
                PinCountText(count = 12)
                RelativeTimeText(iso = "2026-10-07T09:30:00Z")
            }
        }
    }
}

@Preview(name = "Primitives · light", backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PreviewPrimitivesLight() = PrimitivesGallery(dark = false)

@Preview(name = "Primitives · dark", backgroundColor = 0xFF121212, widthDp = 400)
@Composable
private fun PreviewPrimitivesDark() = PrimitivesGallery(dark = true)
