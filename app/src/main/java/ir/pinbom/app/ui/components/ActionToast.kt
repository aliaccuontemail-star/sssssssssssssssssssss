package ir.pinbom.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import kotlinx.coroutines.delay

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · action toast
 *  The app’s only floating surface (together with bottom sheets). It slides up
 *  from above the navigation bar, can carry a small cover thumbnail and one
 *  action (“تغییر”, “بازگرداندن”), and dismisses itself after three seconds.
 *  It is intentionally “inverse” (dark on light) so it stays readable on top of
 *  photography.
 * ──────────────────────────────────────────────────────────────────────────── */

/** One toast message. [id] keeps repeated messages distinguishable. */
data class PinBomToast(
    val message: String,
    val id: Long = System.nanoTime(),
    val coverUrl: String? = null,
    val coverSeed: Long = 0L,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** Hoisted toast state: any screen can fire a toast without owning a Scaffold. */
@Stable
class PinBomToastState {
    var current by mutableStateOf<PinBomToast?>(null)
        private set

    fun show(
        message: String,
        coverUrl: String? = null,
        coverSeed: Long = 0L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        current = PinBomToast(
            message = message,
            coverUrl = coverUrl,
            coverSeed = coverSeed,
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberPinBomToastState(): PinBomToastState = remember { PinBomToastState() }

/**
 * Drop this next to the screen content inside a [Box]; it renders nothing until
 * a toast is requested, then animates in and out by itself.
 */
@Composable
fun ActionToastHost(
    state: PinBomToastState,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    bottomPadding: Dp = Dimens.xl,
    durationMillis: Long = 3_000L,
) {
    val toast = state.current

    // Keep the last toast around so it can animate out gracefully.
    var displayed by remember { mutableStateOf<PinBomToast?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) displayed = toast
    }

    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(durationMillis)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
            initialOffsetY = { height -> height },
        ) + fadeIn(tween(Motion.short)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = Motion.short),
            targetOffsetY = { height -> height },
        ) + fadeOut(tween(Motion.instant)),
        modifier = modifier,
    ) {
        displayed?.let { current ->
            ActionToast(
                toast = current,
                baseUrl = baseUrl,
                onDismiss = { state.dismiss() },
                modifier = Modifier.padding(bottom = bottomPadding),
            )
        }
    }
}

/** The pill itself — exported so screens can position it themselves. */
@Composable
fun ActionToast(
    toast: PinBomToast,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
) {
    val colors = PinBomTheme.colors
    /* Same token as the offline banner: the toast is the page's inverse. */
    val container = colors.inverseSurface
    val contentColor = colors.onInverseSurface

    Row(
        modifier = modifier
            .widthIn(min = 200.dp, max = 420.dp)
            .shadow(
                elevation = 10.dp,
                shape = PinBomShape.pin,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .clip(PinBomShape.pin)
            .background(container)
            .padding(horizontal = Dimens.md, vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!toast.coverUrl.isNullOrBlank()) {
            PinImage(
                url = if (toast.coverUrl.startsWith("http")) {
                    toast.coverUrl
                } else {
                    imageUrl(baseUrl, toast.coverUrl)
                },
                seed = toast.coverSeed,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                shape = PinBomShape.thumb,
            )
            Spacer(Modifier.width(Dimens.sm))
        }

        Text(
            text = toast.message,
            style = PinBomText.bodySmall,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        if (toast.actionLabel != null) {
            Spacer(Modifier.width(Dimens.sm))
            Text(
                text = toast.actionLabel,
                style = PinBomText.button.copy(fontWeight = FontWeight.Bold),
                color = colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        toast.onAction?.invoke()
                        onDismiss()
                    }
                    .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            )
        }
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun ToastGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(PinBomTheme.colors.background),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier.padding(Dimens.xl),
                verticalArrangement = Arrangement.spacedBy(Dimens.md),
            ) {
                ActionToast(
                    toast = PinBomToast(
                        message = "ذخیره شد در دکوراسیون خانه",
                        coverUrl = "",
                        coverSeed = 3,
                        actionLabel = "تغییر",
                    ),
                    onDismiss = {},
                )
                ActionToast(
                    toast = PinBomToast(message = "لینک کپی شد"),
                    onDismiss = {},
                )
                ActionToast(
                    toast = PinBomToast(message = "اتصال اینترنت برقرار نیست"),
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview(name = "Toast · light", backgroundColor = 0xFFFFFFFF, widthDp = 400, heightDp = 280)
@Composable
private fun PreviewToastLight() = ToastGallery(dark = false)

@Preview(name = "Toast · dark", backgroundColor = 0xFF121212, widthDp = 400, heightDp = 280)
@Composable
private fun PreviewToastDark() = ToastGallery(dark = true)
