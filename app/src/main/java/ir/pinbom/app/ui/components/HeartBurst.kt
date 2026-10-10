package ir.pinbom.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.reduceMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · heart burst
 *  The “double tap to like” moment: the heart overshoots 0.6 → 1.3 → 1.0 while a
 *  ring of particles flies out and fades. Purely decorative, so it is skipped
 *  entirely (but the like still fires) when animations are disabled.
 * ──────────────────────────────────────────────────────────────────────────── */

/**
 * Fires once per [trigger] change. Drop it on top of the image inside a Box —
 * it covers the whole area and never intercepts touches.
 */
@Composable
fun HeartBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    color: Color = PinBomTheme.colors.primary,
    onFinished: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val reduced = reduceMotion()
    val finished by rememberUpdatedState(onFinished)

    val scaleAnim = remember { Animatable(0f) }
    val ringAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        if (!reduced) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scaleAnim.snapTo(0.6f)
            ringAnim.snapTo(0f)
            alphaAnim.snapTo(1f)
            coroutineScope {
                launch { scaleAnim.animateTo(1.3f, tween(durationMillis = 180, easing = Motion.overshoot)) }
                launch { ringAnim.animateTo(1f, tween(durationMillis = 520, easing = Motion.standard)) }
                launch { alphaAnim.animateTo(0f, tween(durationMillis = 380, delayMillis = 240)) }
            }
            scaleAnim.snapTo(0f)
        }
        finished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (scaleAnim.value <= 0f && ringAnim.value <= 0f) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val unit = size.minDimension * 0.16f
        val alpha = alphaAnim.value.coerceIn(0f, 1f)

        // Particles flying outwards.
        val progress = ringAnim.value
        if (progress > 0f && progress < 1f && alpha > 0f) {
            val radius = unit * (1.4f + progress * 2.6f)
            repeat(8) { index ->
                val angle = (Math.PI / 4.0) * index - Math.PI / 2.0
                drawCircle(
                    color = color.copy(alpha = ((1f - progress) * alpha).coerceIn(0f, 1f)),
                    radius = unit * 0.13f,
                    center = Offset(
                        center.x + (radius * cos(angle)).toFloat(),
                        center.y + (radius * sin(angle)).toFloat(),
                    ),
                )
            }
        }

        // The heart.
        val heartScale = if (scaleAnim.value > 0f) scaleAnim.value else 0f
        if (heartScale > 0f && alpha > 0f) {
            scale(scaleX = heartScale, scaleY = heartScale, pivot = center) {
                drawHeart(center = center, radius = unit * 1.5f, color = color.copy(alpha = alpha))
            }
        }
    }
}

/**
 * Two overlapping discs for the lobes plus a triangle for the tip — robust at
 * any size and cheap to draw every frame.
 */
private fun DrawScope.drawHeart(center: Offset, radius: Float, color: Color) {
    val lobeRadius = radius * 0.55f
    val lobeCenterY = center.y - radius * 0.42f

    drawCircle(color = color, radius = lobeRadius, center = Offset(center.x - radius * 0.5f, lobeCenterY))
    drawCircle(color = color, radius = lobeRadius, center = Offset(center.x + radius * 0.5f, lobeCenterY))

    drawPath(
        path = Path().apply {
            moveTo(center.x - radius * 0.97f, lobeCenterY + radius * 0.10f)
            lineTo(center.x + radius * 0.97f, lobeCenterY + radius * 0.10f)
            lineTo(center.x, center.y + radius * 0.92f)
            close()
        },
        color = color,
    )
}

/** Thin alias for readability at call sites that host the gesture themselves. */
@Composable
fun DoubleTapLikeOverlay(
    likeTrigger: Int,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) = HeartBurst(trigger = likeTrigger, modifier = modifier, onFinished = onFinished)

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Preview(name = "HeartBurst · light", backgroundColor = 0xFFEDEDED, widthDp = 240, heightDp = 240)
@Composable
private fun PreviewHeartBurstLight() {
    PinBomTheme(darkTheme = false) {
        Box(
            Modifier
                .size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            HeartBurst(trigger = 1)
        }
    }
}

@Preview(name = "HeartBurst · dark", backgroundColor = 0xFF121212, widthDp = 240, heightDp = 240)
@Composable
private fun PreviewHeartBurstDark() {
    PinBomTheme(darkTheme = true) {
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            HeartBurst(trigger = 1)
        }
    }
}
