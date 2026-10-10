package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import ir.pinbom.app.R
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.avatarColorFor

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · Avatar
 *  Photo when we have one, otherwise a deterministic coloured disc with the
 *  first letter of the name. The fallback is the common case today (Author has
 *  no avatar field), so it is designed rather than tolerated.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun Avatar(
    name: String?,
    modifier: Modifier = Modifier,
    url: String? = null,
    baseUrl: String = "",
    size: Dp = 40.dp,
    ring: Color? = null,
    ringWidth: Dp = 2.dp,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val colors = PinBomTheme.colors
    val context = LocalContext.current
    val label = name?.trim().orEmpty()
    // One resolver for the whole app: absolute URLs, "/media/…" paths and bare
    // storage paths all end up as a fetchable URL (see imageUrl).
    val resolved = remember(url, baseUrl) { imageUrl(baseUrl, url.orEmpty()) }
    var failed by remember(resolved) { mutableStateOf(resolved.isBlank()) }

    val request = remember(resolved, context) {
        if (resolved.isBlank()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(resolved)
                .crossfade(180)
                .memoryCacheKey(resolved)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    val description = stringResource(R.string.cd_avatar, label.ifBlank { "?" })

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColorFor(label.ifBlank { "pinbom" }))
            .then(if (ring != null) Modifier.border(ringWidth, ring, CircleShape) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // Initial letter — always drawn; the photo simply covers it when ready.
        if (label.isNotBlank()) {
            Text(
                text = label.substringBefore(' ').take(1),
                style = PinBomText.button.copy(
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (size.value * 0.48f).sp,
                ),
                color = Color.White,
            )
        }

        if (request != null && !failed) {
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
                onError = { failed = true },
            )
        }

        badge?.invoke(this)
    }
}

/** Small multi-avatar row used by board rows and notifications. */
@Composable
fun AvatarStack(names: List<String>, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.xxs),
    ) {
        names.take(3).forEach { name -> Avatar(name = name, size = size) }
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Preview(name = "Avatar · light", backgroundColor = 0xFFFFFFFF, widthDp = 320)
@Composable
private fun PreviewAvatarLight() {
    PinBomTheme(darkTheme = false) {
        Column(Modifier.padding(Dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name = "نگار محمدی", size = 20.dp)
                Spacer(Modifier.width(Dimens.sm))
                Avatar(name = "سارا کریمی", size = 32.dp)
                Spacer(Modifier.width(Dimens.sm))
                Avatar(name = "پویا احمدی", size = 40.dp)
                Spacer(Modifier.width(Dimens.sm))
                Avatar(name = "الهام نوری", size = 56.dp, ring = PinBomTheme.colors.textPrimary)
                Spacer(Modifier.width(Dimens.sm))
                Avatar(name = "بهار سلطانی", size = 72.dp)
            }
            Spacer(Modifier.padding(Dimens.sm))
            AvatarStack(names = listOf("نگار", "سارا", "پویا", "الهام"), size = 28.dp)
        }
    }
}

@Preview(name = "Avatar · dark", backgroundColor = 0xFF121212, widthDp = 320)
@Composable
private fun PreviewAvatarDark() {
    PinBomTheme(darkTheme = true) {
        Row(Modifier.padding(Dimens.lg), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = "نگار محمدی", size = 20.dp)
            Spacer(Modifier.width(Dimens.sm))
            Avatar(name = "سارا کریمی", size = 32.dp)
            Spacer(Modifier.width(Dimens.sm))
            Avatar(name = "پویا احمدی", size = 40.dp)
            Spacer(Modifier.width(Dimens.sm))
            Avatar(name = "الهام نوری", size = 56.dp, ring = PinBomTheme.colors.textPrimary)
            Spacer(Modifier.width(Dimens.sm))
            Avatar(name = null, size = 48.dp)
        }
    }
}
