package ir.pinbom.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.pressScale

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · buttons
 *  Three pill shapes and one circular icon button. Everything the app taps
 *  goes through these, so pressed states, loading states and disabled colours
 *  can never drift apart between screens.
 * ──────────────────────────────────────────────────────────────────────────── */

enum class PillButtonSize(val height: Dp, val horizontalPadding: Dp) {
    Regular(Dimens.button, 20.dp),
    Small(Dimens.buttonSmall, 16.dp),
}

/** Brand red pill: “ذخیره”, “انتشار”, “دنبال کردن”, form submits. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    size: PillButtonSize = PillButtonSize.Regular,
    fillWidth: Boolean = false,
    containerColor: Color = PinBomTheme.colors.primary,
    contentColor: Color = labelOn(containerColor),
) {
    val colors = PinBomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val resolvedContent = if (enabled && !loading) contentColor else colors.textTertiary

    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .pressScale(interaction, pressedScale = 0.96f),
        enabled = enabled && !loading,
        shape = PinBomShape.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.surfaceGrey,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = size.horizontalPadding),
        interactionSource = interaction,
    ) {
        Crossfade(
            targetState = loading,
            animationSpec = tween(Motion.short),
            label = "primaryLoading",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.textSecondary,
                )
            } else {
                ButtonLabel(text = text, icon = leadingIcon, color = resolvedContent)
            }
        }
    }
}

/** Grey pill: “اشتراک‌گذاری”, “ویرایش پروفایل”, “دنبال می‌کنید”. */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    size: PillButtonSize = PillButtonSize.Regular,
    fillWidth: Boolean = false,
    selected: Boolean = false,
) {
    val colors = PinBomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val container = if (selected) colors.chipSelectedBg else colors.surfaceGrey
    val content = if (selected) colors.chipSelectedText else colors.textPrimary

    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .pressScale(interaction, pressedScale = 0.96f),
        enabled = enabled && !loading,
        shape = PinBomShape.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = colors.surfaceGrey,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = size.horizontalPadding),
        interactionSource = interaction,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    ) {
        Crossfade(
            targetState = loading,
            animationSpec = tween(Motion.short),
            label = "secondaryLoading",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.textSecondary,
                )
            } else {
                ButtonLabel(text = text, icon = leadingIcon, color = content)
            }
        }
    }
}

/** Text-only pill: quiet actions, “بیشتر”, “انصراف”, “تلاش مجدد”. */
@Composable
fun TextPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false,
    size: PillButtonSize = PillButtonSize.Small,
) {
    val colors = PinBomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val content = when {
        !enabled -> colors.textTertiary
        destructive -> MaterialTheme.colorScheme.error
        else -> colors.textPrimary
    }

    Row(
        modifier = modifier
            .height(size.height)
            .clip(PinBomShape.pill)
            .pressScale(interaction, pressedScale = 0.96f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = content),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = size.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ButtonLabel(text = text, icon = leadingIcon, color = content)
    }
}

/**
 * Translucent / grey circular icon button. Used for the floating back and more
 * buttons over images, the search field, notification rows and the profile bar.
 */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = Dimens.iconButton,
    containerColor: Color? = null,
    contentColor: Color = PinBomTheme.colors.textPrimary,
    shape: Shape = CircleShape,
    iconSize: Dp = 20.dp,
    badge: Boolean = false,
    badgeColor: Color = PinBomTheme.colors.primary,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = PinBomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val resolvedContainer = containerColor ?: colors.surfaceGrey

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(resolvedContainer)
            .pressScale(interaction, pressedScale = 0.92f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else colors.textTertiary,
            modifier = Modifier.size(iconSize),
        )
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.16f, end = size * 0.16f)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
            )
        }
    }
}

/**
 * The label colour that belongs with a given container.
 *
 * The pill has always been white-on-brand-red, and that pair is unchanged. What
 * this fixes is the caller who passes a *light* container (`surface`, or the
 * light theme's `surfaceGrey`): the label used to stay white and disappear.
 * Rules of thumb, both measured: white on `primary` clears 4.7:1 in the light
 * theme and 3.5:1 in the dark one (button-sized semibold text), while
 * `inverseSurface` on a light container clears 13:1 or better.
 */
@Composable
private fun labelOn(container: Color): Color =
    if (container.luminance() > 0.45f) PinBomTheme.colors.inverseSurface else Color.White

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Dimens.sm))
        }
        Text(
            text = text,
            style = PinBomText.button,
            color = color,
            maxLines = 1,
        )
    }
}

/** Small square-ish button used inside sheets and dialogs. */
@Composable
fun PinBomTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    contentColor: Color = PinBomTheme.colors.textPrimary,
) {
    val colors = PinBomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(Dimens.buttonSmall)
            .clip(shape)
            .background(colors.surfaceGrey)
            .pressScale(interaction, pressedScale = 0.96f)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonLabel(text = text, icon = leadingIcon, color = contentColor)
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun ButtonGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(PinBomTheme.colors.background)
                .padding(Dimens.xl),
            verticalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                PrimaryPillButton(text = "ذخیره", onClick = {})
                PrimaryPillButton(text = "انتشار", onClick = {}, loading = true)
                PrimaryPillButton(text = "غیرفعال", onClick = {}, enabled = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                SecondaryPillButton(text = "اشتراک‌گذاری", onClick = {})
                SecondaryPillButton(text = "دنبال می‌کنید", onClick = {}, selected = true)
                SecondaryPillButton(text = "کوتاه", onClick = {}, size = PillButtonSize.Small)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                TextPillButton(text = "انصراف", onClick = {})
                TextPillButton(text = "تلاش مجدد", onClick = {})
                TextPillButton(text = "حذف", onClick = {}, destructive = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                IconCircleButton(
                    icon = Icons.Outlined.BookmarkBorder,
                    contentDescription = "ذخیره",
                    onClick = {},
                )
                IconCircleButton(
                    icon = Icons.Outlined.Share,
                    contentDescription = "اشتراک‌گذاری",
                    onClick = {},
                    containerColor = PinBomTheme.colors.surfaceGrey,
                    badge = true,
                )
                IconCircleButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "تنظیمات",
                    onClick = {},
                    enabled = false,
                )
                PinBomTonalButton(text = "خاص", onClick = {})
            }
            PrimaryPillButton(text = "تمام‌عرض", onClick = {}, fillWidth = true)
        }
    }
}

@Preview(name = "Buttons · light", backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PreviewButtonsLight() = ButtonGallery(dark = false)

@Preview(name = "Buttons · dark", backgroundColor = 0xFF121212, widthDp = 400)
@Composable
private fun PreviewButtonsDark() = ButtonGallery(dark = true)
