package ir.pinbom.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.pinbom.app.R
import ir.pinbom.app.ui.theme.BrandRed
import ir.pinbom.app.ui.theme.BrandRedDeep
import ir.pinbom.app.ui.theme.BrandRedSoft
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · brand mark
 *
 *  One glyph, three renderings — all cut from the same geometry:
 *    • brand/pinbom-glyph.svg           (design source)
 *    • res/drawable/ic_pinbom_*.xml     (launcher, splash, notification)
 *    • PinBomGlyphPath / PinBomGlyphVector below (Compose, in-app)
 *
 *  The mark: a bold geometric “B” whose two bowls are stacked pin bubbles
 *  sharing one stem. Both counters are real holes (evenOdd fill), so the mark
 *  still reads as a B at 24dp. Authored in a 108×108 space so it maps 1:1 onto
 *  the adaptive-icon canvas. No dots, no spark, no floating decorations.
 * ──────────────────────────────────────────────────────────────────────────── */

/**
 * String form of the glyph. Single source of truth for both Compose renderings —
 * identical to the pathData in the vector drawables.
 * Outer outline + two counter subpaths; fill them with [PathFillType.EvenOdd].
 * Glyph box: x 34..72, y 28..82 (optically centred in the 108 viewport).
 */
private const val PINBOM_GLYPH_PATH_DATA: String =
    "M38,28 H55 A13,13 0 0 1 55,54 H58 A14,14 0 0 1 58,82 H38 Q34,82 34,78 V32 Q34,28 38,28 Z " +
        "M46,36 H54 A5,5 0 0 1 54,46 H46 Z " +
        "M46,59 H56 A6,6 0 0 1 56,71 H46 Z"

/** The glyph as a Compose [Path], in the 108×108 brand space. Two real counters. */
val PinBomGlyphPath: Path = PathParser()
    .parsePathString(PINBOM_GLYPH_PATH_DATA)
    .toPath(Path())
    .apply { fillType = PathFillType.EvenOdd }

/** Icon-shaped twin of the glyph, for `Icon(imageVector = PinBomGlyphVector)`. */
val PinBomGlyphVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "PinBomGlyph",
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
    )
        .addPath(
            pathData = PathParser().parsePathString(PINBOM_GLYPH_PATH_DATA).toNodes(),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
        )
        .build()
}

/** Authored space of the glyph. */
const val PINBOM_GLYPH_VIEWPORT = 108f

/**
 * Optical scale of the glyph inside its container. The glyph box is 54 units
 * tall, so 1.2 puts it at ~60% of the container — the same weight as the
 * launcher icon (`ic_pinbom.xml` uses the same number in its group).
 */
const val PINBOM_MARK_GLYPH_SCALE = 1.2f

private fun DrawScope.drawPinBomGlyph(scale: Float, alpha: Float, color: Color, shadow: Boolean) {
    val side = kotlin.math.min(size.width, size.height)
    val factor = (side / PINBOM_GLYPH_VIEWPORT) * PINBOM_MARK_GLYPH_SCALE * scale
    val left = (size.width - PINBOM_GLYPH_VIEWPORT * factor) / 2f
    val top = (size.height - PINBOM_GLYPH_VIEWPORT * factor) / 2f

    if (shadow) {
        withTransform({
            translate(left = left, top = top + 1.5f * factor)
            scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero)
        }) {
            drawPath(path = PinBomGlyphPath, color = Color.Black.copy(alpha = 0.10f * alpha))
        }
    }
    withTransform({
        translate(left = left, top = top)
        scale(scaleX = factor, scaleY = factor, pivot = Offset.Zero)
    }) {
        drawPath(path = PinBomGlyphPath, color = color.copy(alpha = alpha))
    }
}

/**
 * The PinBom mark: brand-gradient squircle (or circle) with the glyph inside.
 * `animated = true` pops the glyph in with a spring — used on the auth hero,
 * the server-setup screen and the cold-boot hand-off after the splash.
 */
@Composable
fun PinBomMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    animated: Boolean = false,
    rounded: Boolean = true,
    contentDescription: String? = stringResource(R.string.cd_pinbom_logo),
) {
    val glyphScale = remember { Animatable(if (animated) 0.72f else 1f) }
    val glyphAlpha = remember { Animatable(if (animated) 0f else 1f) }
    val label = contentDescription

    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        coroutineScope {
            launch { glyphScale.animateTo(1f, Motion.popSpring) }
            launch { glyphAlpha.animateTo(1f, tween(durationMillis = 520, easing = Motion.standard)) }
        }
    }

    val shape = if (rounded) RoundedCornerShape(size * 0.24f) else CircleShape

    Canvas(
        modifier = modifier
            .size(size)
            .clip(shape)
            .semantics { label?.let { this.contentDescription = it } },
    ) {
        val width = this.size.width
        val height = this.size.height

        // Diagonal brand gradient.
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(BrandRedSoft, BrandRed, BrandRedDeep),
                start = Offset(0f, 0f),
                end = Offset(width, height),
            ),
        )
        // 12% top-start highlight — the fill reads as lit, not flat.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(width * 0.26f, height * 0.22f),
                radius = width * 0.86f,
            ),
        )
        // 12% bottom shade for weight.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)),
                startY = height * 0.42f,
                endY = height,
            ),
        )
        drawPinBomGlyph(
            scale = glyphScale.value,
            alpha = glyphAlpha.value,
            color = Color.White,
            shadow = true,
        )
    }
}

/**
 * Wordmark: «پینبوم» / “PinBom” in the language’s own black weight — Vazirmatn Black
 * for Persian, Inter Black for English — via the current typography. Just text:
 * no dot flourish, no ornament. The text comes from `@string/app_name`, so it
 * follows the app language.
 */
@Composable
fun PinBomWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = PinBomText.headline,
    color: Color = PinBomTheme.colors.textPrimary,
) {
    Text(
        text = stringResource(R.string.app_name),
        modifier = modifier,
        style = style.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
        ),
        color = color,
        maxLines = 1,
    )
}

/**
 * Mark + wordmark. The order follows the layout direction: LTR puts the mark on
 * the left, RTL on the right — no mirroring hacks, just a Row. [markSize] defaults
 * to the wordmark cap-height plus ~30%, so the two stay optically matched.
 */
@Composable
fun PinBomBrand(
    modifier: Modifier = Modifier,
    markSize: Dp? = null,
    animated: Boolean = false,
    showWordmark: Boolean = true,
    wordmarkStyle: TextStyle = PinBomText.titleLarge,
) {
    val mark = markSize ?: (wordmarkStyle.fontSize.value * 1.3f).dp
    val wordmarkAlpha = remember { Animatable(if (animated) 0f else 1f) }
    val wordmarkLift = remember { Animatable(if (animated) 14f else 0f) }

    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        delay(380)
        coroutineScope {
            launch { wordmarkAlpha.animateTo(1f, tween(durationMillis = 340, easing = Motion.standard)) }
            launch { wordmarkLift.animateTo(0f, Motion.layoutSpring) }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        PinBomMark(size = mark, animated = animated)
        if (showWordmark) {
            PinBomWordmark(
                modifier = Modifier
                    .alpha(wordmarkAlpha.value)
                    .offset(y = wordmarkLift.value.dp),
                style = wordmarkStyle,
            )
        }
    }
}

/**
 * The vertical lockup: mark on top, wordmark underneath, both centred. Used by
 * the auth and server-setup heroes, where a horizontal lockup would depend on
 * the layout direction.
 */
@Composable
fun PinBomBrandStack(
    modifier: Modifier = Modifier,
    markSize: Dp = 64.dp,
    animated: Boolean = false,
    wordmarkStyle: TextStyle = PinBomText.headline,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinBomMark(size = markSize, animated = animated)
        Box(Modifier.size(Dimens.md))
        PinBomWordmark(style = wordmarkStyle)
    }
}

/** Cold-boot screen: the mark pops in place of a blank white screen. */
@Composable
fun PinBomBootScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        PinBomBrandStack(markSize = 64.dp, animated = true)
    }
}

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Preview(name = "Mark · sizes · light", backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewMarkLight() {
    PinBomTheme(darkTheme = false) {
        Row(
            Modifier
                .background(PinBomTheme.colors.background)
                .padding(Dimens.lg),
            horizontalArrangement = Arrangement.spacedBy(Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PinBomMark(size = 24.dp, animated = false)
            PinBomMark(size = 28.dp, animated = false)
            PinBomMark(size = 48.dp, animated = false)
            PinBomMark(size = 96.dp, animated = false)
        }
    }
}

@Preview(name = "Mark · sizes · dark", backgroundColor = 0xFF121212)
@Composable
private fun PreviewMarkDark() {
    PinBomTheme(darkTheme = true) {
        Row(
            Modifier
                .background(PinBomTheme.colors.background)
                .padding(Dimens.lg),
            horizontalArrangement = Arrangement.spacedBy(Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PinBomMark(size = 24.dp, animated = false)
            PinBomMark(size = 28.dp, animated = false)
            PinBomMark(size = 48.dp, animated = false)
            PinBomMark(size = 96.dp, animated = false)
            PinBomMark(size = 48.dp, animated = false, rounded = false)
        }
    }
}

@Preview(name = "Lockup · LTR", backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLockupLtr() {
    PinBomTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .background(PinBomTheme.colors.background)
                    .padding(Dimens.xl),
                horizontalArrangement = Arrangement.spacedBy(Dimens.xxl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PinBomBrand(wordmarkStyle = PinBomText.titleLarge)
                PinBomBrandStack(markSize = 48.dp, wordmarkStyle = PinBomText.title)
            }
        }
    }
}

@Preview(name = "Lockup · RTL", backgroundColor = 0xFF121212)
@Composable
private fun PreviewLockupRtl() {
    PinBomTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                Modifier
                    .background(PinBomTheme.colors.background)
                    .padding(Dimens.xl),
                horizontalArrangement = Arrangement.spacedBy(Dimens.xxl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PinBomBrand(wordmarkStyle = PinBomText.titleLarge)
                PinBomBrandStack(markSize = 48.dp, wordmarkStyle = PinBomText.title)
            }
        }
    }
}
