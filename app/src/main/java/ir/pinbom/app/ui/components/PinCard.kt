package ir.pinbom.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.pinbom.app.R
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.pressScale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · PinCard
 *  No Card, no border, no elevation — the image *is* the card. Below it: a two
 *  line title, a 20dp avatar with the author and a quiet “more” affordance.
 *  Long-press pops a floating quick-action bubble over a dimmed screen.
 * ──────────────────────────────────────────────────────────────────────────── */

/** One entry of the long-press quick-action bubble. */
data class PinQuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

@Composable
fun PinCard(
    pin: Pin,
    baseUrl: String,
    modifier: Modifier = Modifier,
    imageAspect: Float = pinAspectRatio(pin),
    shape: Shape = PinBomShape.pin,
    showAuthor: Boolean = true,
    showQuickActions: Boolean = true,
    showSavedBadge: Boolean = true,
    onSaveClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onHideClick: (() -> Unit)? = null,
    onAuthorClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = PinBomTheme.colors
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    var quickActionsOpen by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Rect.Zero) }

    val saveLabel = stringResource(R.string.action_save)
    val shareLabel = stringResource(R.string.action_share)
    val hideLabel = stringResource(R.string.action_hide)

    val quickActions = remember(onSaveClick, onShareClick, onHideClick, saveLabel, shareLabel, hideLabel) {
        buildList {
            if (onSaveClick != null) {
                add(PinQuickAction(saveLabel, Icons.Outlined.BookmarkAdd, onSaveClick))
            }
            if (onShareClick != null) {
                add(PinQuickAction(shareLabel, Icons.Outlined.Share, onShareClick))
            }
            if (onHideClick != null) {
                add(PinQuickAction(hideLabel, Icons.Outlined.VisibilityOff, onHideClick))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { anchor = it.boundsInWindow() }
            .pressScale(source = interaction, pressedScale = 0.98f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                hapticFeedbackEnabled = false,
                onLongClick = {
                    if (showQuickActions && quickActions.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        quickActionsOpen = true
                    }
                },
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            PinImage(
                url = if (pin.image_url.isBlank()) "" else imageUrl(baseUrl, pin.image_url),
                seed = pin.id,
                contentDescription = stringResource(R.string.cd_pin_image, pin.title),
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                aspectRatio = imageAspect,
            )

            if (showSavedBadge && pin.saved) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Dimens.sm)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.cd_saved_badge),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.sm))

        ContentText(
            text = pin.title,
            style = PinBomText.pinTitle,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.xs),
        )

        if (showAuthor) {
            Spacer(Modifier.height(Dimens.xs))
            Row(
                modifier = Modifier.padding(horizontal = Dimens.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    name = pin.author?.display_name,
                    size = 20.dp,
                    modifier = if (onAuthorClick != null) {
                        Modifier.clickable(onClick = onAuthorClick)
                    } else {
                        Modifier
                    },
                )
                Spacer(Modifier.width(Dimens.xs))
                ContentText(
                    text = pin.author?.display_name.orEmpty(),
                    style = PinBomText.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (onMoreClick != null) {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(Dimens.iconButtonSmall),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    if (quickActionsOpen && quickActions.isNotEmpty()) {
        PinQuickActions(
            anchor = anchor,
            title = pin.title,
            actions = quickActions,
            onDismiss = { quickActionsOpen = false },
        )
    }
}

/**
 * The floating bubble shown after a long press: a dimmed screen, a compact
 * card with the pin title and 2–3 actions, popped in with a spring.
 */
@Composable
fun PinQuickActions(
    anchor: Rect,
    actions: List<PinQuickAction>,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    val colors = PinBomTheme.colors
    val density = LocalDensity.current
    val container = LocalWindowInfo.current.containerSize

    val bubbleWidth = 232.dp
    val rowHeight = 48.dp
    val bubbleHeight = rowHeight * actions.size + Dimens.sm * 2 + if (title != null) 34.dp else 0.dp
    val margin = Dimens.md

    val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
    val bubbleHeightPx = with(density) { bubbleHeight.toPx() }
    val marginPx = with(density) { margin.toPx() }

    val x = (anchor.center.x - bubbleWidthPx / 2f)
        .coerceIn(marginPx, (container.width - bubbleWidthPx - marginPx).coerceAtLeast(marginPx))
    val y = (anchor.center.y - bubbleHeightPx / 2f)
        .coerceIn(marginPx, (container.height - bubbleHeightPx - marginPx).coerceAtLeast(marginPx))

    val scale = remember { Animatable(0.88f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scale.animateTo(1f, Motion.popSpring) }
            launch { fade.animateTo(1f, tween(Motion.short, easing = Motion.standard)) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(bubbleWidth)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = fade.value
                    }
                    .shadow(
                        elevation = 12.dp,
                        shape = PinBomShape.boardCover,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    )
                    .clip(PinBomShape.boardCover)
                    .background(colors.surface)
                    .padding(vertical = Dimens.sm),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = PinBomText.caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            start = Dimens.lg,
                            end = Dimens.lg,
                            top = Dimens.xs,
                            bottom = Dimens.xs,
                        ),
                    )
                }
                actions.forEach { action ->
                    QuickActionRow(action = action)
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(action: PinQuickAction) {
    val colors = PinBomTheme.colors
    val tint = if (action.destructive) MaterialTheme.colorScheme.error else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.touch)
            .clickable(onClick = action.onClick)
            .padding(horizontal = Dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.md))
        Text(text = action.label, style = PinBomText.button, color = tint)
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Preview(name = "PinCard · grid · light", backgroundColor = 0xFFFFFFFF, widthDp = 220)
@Composable
private fun PreviewPinCardLight() {
    PinBomTheme(darkTheme = false) {
        Column(Modifier.padding(Dimens.sm)) {
            PinCard(
                pin = PreviewData.pins[0],
                baseUrl = "",
                onSaveClick = {},
                onShareClick = {},
                onHideClick = {},
                onMoreClick = {},
                onAuthorClick = {},
                onClick = {},
            )
            Spacer(Modifier.height(Dimens.md))
            PinCard(
                pin = PreviewData.pins[1],
                baseUrl = "",
                imageAspect = 1f,
                onMoreClick = {},
                onClick = {},
            )
        }
    }
}

@Preview(name = "PinCard · grid · dark", backgroundColor = 0xFF121212, widthDp = 220)
@Composable
private fun PreviewPinCardDark() {
    PinBomTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.sm)) {
            PinCard(
                pin = PreviewData.pins[4],
                baseUrl = "",
                onSaveClick = {},
                onShareClick = {},
                onHideClick = {},
                onMoreClick = {},
                onClick = {},
            )
            Spacer(Modifier.height(Dimens.md))
            PinCard(
                pin = PreviewData.pins[2],
                baseUrl = "",
                imageAspect = 0.6f,
                showSavedBadge = true,
                onMoreClick = {},
                onClick = {},
            )
        }
    }
}

@Preview(name = "Quick actions", backgroundColor = 0xFFEFEFEF, widthDp = 300, heightDp = 420)
@Composable
private fun PreviewQuickActions() {
    PinBomTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize()) {
            PinQuickActions(
                anchor = Rect(offset = Offset(120f, 200f), size = Size(300f, 400f)),
                title = "ایده چیدمان اتاق نشیمن مینیمال",
                actions = listOf(
                    PinQuickAction("ذخیره", Icons.Outlined.BookmarkAdd, {}),
                    PinQuickAction("اشتراک‌گذاری", Icons.Outlined.Share, {}),
                    PinQuickAction("پنهان کردن", Icons.Outlined.VisibilityOff, {}),
                ),
                onDismiss = {},
            )
        }
    }
}
