package ir.pinbom.app.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.pinbom.app.R
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.pinbomClick

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · empty / error / offline
 *  A blank screen is a bug. Every list in the app can put one of these in place
 *  and the state will look designed: soft tinted disc, bold title, calm copy,
 *  one pill action.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Outlined.Image,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    StateScaffold(
        modifier = modifier,
        icon = icon,
        title = title,
        description = description,
        actionLabel = actionLabel,
        onAction = onAction,
        compact = compact,
    )
}

@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.state_error_title),
    description: String = stringResource(R.string.state_error_body),
    icon: ImageVector = Icons.Outlined.CloudOff,
    retryLabel: String = stringResource(R.string.action_retry),
) {
    StateScaffold(
        modifier = modifier,
        icon = icon,
        title = title,
        description = description,
        actionLabel = retryLabel,
        onAction = onRetry,
        tint = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun StateScaffold(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    description: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    compact: Boolean = false,
    tint: Color = PinBomTheme.colors.textPrimary,
) {
    val colors = PinBomTheme.colors
    val iconSize = if (compact) 64.dp else 88.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.xxl, vertical = if (compact) Dimens.xl else Dimens.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(colors.surfaceGrey),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = 0.75f),
                modifier = Modifier.size(iconSize * 0.42f),
            )
        }

        Spacer(Modifier.height(Dimens.lg))

        Text(
            text = title,
            style = if (compact) PinBomText.title else PinBomText.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        if (description != null) {
            Spacer(Modifier.height(Dimens.sm))
            Text(
                text = description,
                style = PinBomText.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Dimens.xl))
            PrimaryPillButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Slim banner that drops in from the top when the device goes offline. The feed
 * keeps showing cached pins underneath, which is the whole point.
 */
@Composable
fun OfflineBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_offline),
    onRetry: (() -> Unit)? = null,
) {
    val colors = PinBomTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(Motion.medium, easing = Motion.emphasized),
            expandFrom = Alignment.Top,
        ) + fadeIn(tween(Motion.short)),
        exit = shrinkVertically(
            animationSpec = tween(Motion.short),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(tween(Motion.instant)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerContainer())
                .padding(horizontal = Dimens.lg, vertical = Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = bannerContent(),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Dimens.sm))
            Text(
                text = message,
                style = PinBomText.caption,
                color = bannerContent(),
                modifier = Modifier.weight(1f),
            )
            if (onRetry != null) {
                Spacer(Modifier.width(Dimens.sm))
                Text(
                    text = stringResource(R.string.action_retry),
                    style = PinBomText.button,
                    color = colors.primary,
                    modifier = Modifier
                        .clip(PinBomShape.pill)
                        .pinbomClick(onClick = onRetry, pressedScale = 0.94f)
                        .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                )
            }
        }
    }
}

/* The offline banner is deliberately the *inverse* of the page: a dark slab in
 * the light theme, a raised grey slab in the dark one. That pair is a token now
 * (`inverseSurface` / `onInverseSurface`) rather than two literals repeated here
 * and in the toast — one place to keep readable in both themes. */
@Composable
private fun bannerContainer(): Color = PinBomTheme.colors.inverseSurface

@Composable
private fun bannerContent(): Color = PinBomTheme.colors.onInverseSurface

/**
 * Connectivity as UI state. Cheap, callback-based and permission-guarded: if
 * `ACCESS_NETWORK_STATE` is missing the helper simply reports “online” so the
 * app never shows a false banner.
 */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var online by remember(context) { mutableStateOf(isNetworkAvailable(context)) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = true
            }

            override fun onLost(network: Network) {
                online = isNetworkAvailable(context)
            }
        }
        val registered = manager?.let { runCatching { it.registerDefaultNetworkCallback(callback) }.isSuccess } == true
        onDispose {
            if (registered) runCatching { manager?.unregisterNetworkCallback(callback) }
        }
    }

    return online
}

private fun isNetworkAvailable(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(true)

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun StateGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            Modifier
                .background(PinBomTheme.colors.background)
                .fillMaxWidth(),
        ) {
            OfflineBanner(visible = true, onRetry = {})
            EmptyState(
                title = stringResource(R.string.state_empty_title),
                description = stringResource(R.string.state_empty_body),
                actionLabel = "کشف ایده‌ها",
                onAction = {},
                compact = true,
            )
            ErrorState(onRetry = {})
        }
    }
}

@Preview(name = "States · light", backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PreviewStatesLight() = StateGallery(dark = false)

@Preview(name = "States · dark", backgroundColor = 0xFF121212, widthDp = 400)
@Composable
private fun PreviewStatesDark() = StateGallery(dark = true)
