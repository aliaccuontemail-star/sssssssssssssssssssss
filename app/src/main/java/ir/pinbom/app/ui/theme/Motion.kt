package ir.pinbom.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · motion tokens, press feedback, shimmer
 *  Motion is the difference between “a list of images” and “an app”. Press
 *  springs, pop overshoots, steady layout springs — never a linear default.
 * ──────────────────────────────────────────────────────────────────────────── */

object Motion {
    /* Durations (ms) */
    const val instant = 90
    const val short = 160
    const val medium = 260
    const val long = 380

    /* Easings */
    val standard: Easing = FastOutSlowInEasing

    /** Material-ish emphasised curve for enter/exit. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** EaseOutBack-like overshoot — pops, checkmarks, badges. */
    val overshoot: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Sheet drags and reversible gestures. */
    val anticipate: Easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

    /* Springs */
    /** Taps: fast, slightly bouncy, never sluggish. */
    val pressSpring: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Pops: like buttons, save checkmarks, follow state. */
    val popSpring: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 400f)

    /** Layout and scroll-driven values: steady. */
    val layoutSpring: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 300f)

    /** Colour crossfades for like/save/follow states. */
    val colorShort: TweenSpec<Color> = tween(durationMillis = short, easing = standard)

    fun <T> instantSpec(): TweenSpec<T> = tween(durationMillis = instant, easing = LinearEasing)

    fun <T> shortSpec(): TweenSpec<T> = tween(durationMillis = short, easing = standard)

    fun <T> mediumSpec(): TweenSpec<T> = tween(durationMillis = medium, easing = standard)

    fun <T> longSpec(): TweenSpec<T> = tween(durationMillis = long, easing = emphasized)
}

/** True when the user disabled system animations (Animator duration scale: off). */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun reduceMotion(): Boolean = LocalReduceMotion.current

/** Reads the animator duration scale once per composition context. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f) == 0f
    }
}

/** Adapts any spec to “instant” when reduced motion is requested. */
@Composable
fun <T> motion(spec: AnimationSpec<T>): AnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else spec

/**
 * Scales the element down while pressed and springs it back on release.
 * Callers must pass the same [source] to their `clickable`.
 */
@Composable
fun Modifier.pressScale(
    source: InteractionSource,
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
): Modifier {
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduce) pressedScale else 1f,
        animationSpec = if (reduce) snap() else Motion.pressSpring,
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * The PinBom tap: spring press feedback + optional ripple. Ripple is opt-in —
 * image surfaces feel better without it, list rows keep the familiar response.
 */
@Composable
fun Modifier.pinbomClick(
    onClick: () -> Unit,
    source: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    indication: Indication? = null,
    onClickLabel: String? = null,
    role: Role? = null,
): Modifier = this
    .pressScale(source = source, pressedScale = pressedScale, enabled = enabled)
    .clickable(
        interactionSource = source,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )

/** Convenience overload that owns its interaction source. */
@Composable
fun Modifier.pinbomClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    indication: Indication? = null,
    onClickLabel: String? = null,
    role: Role? = null,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.pinbomClick(
        onClick = onClick,
        source = source,
        enabled = enabled,
        pressedScale = pressedScale,
        indication = indication,
        onClickLabel = onClickLabel,
        role = role,
    )
}

/**
 * A slow light sweep drawn *over* the content. Pair it with a
 * `background(PinBomTheme.colors.skeleton, shape)` to build skeletons that never
 * flash white and never look dead.
 */
@Composable
fun Modifier.shimmer(enabled: Boolean = true): Modifier {
    if (!enabled || LocalReduceMotion.current) return this
    val highlight = PinBomTheme.colors.skeletonHighlight
    /* The sweep follows the reading direction: left → right in English,
     * right → left in Persian. */
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return this.drawWithContent {
        drawContent()
        val width = size.width
        val travel = if (rtl) 1f - progress else progress
        val x = -width + travel * (width * 2f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + width, size.height),
            ),
        )
    }
}

/** Skeleton block: flat token colour + shimmer sweep. */
@Composable
fun Modifier.skeleton(
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true,
): Modifier = this
    .clip(shape)
    .background(PinBomTheme.colors.skeleton)
    .shimmer(enabled)
