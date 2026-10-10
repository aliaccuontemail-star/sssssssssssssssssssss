package ir.pinbom.app.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ir.pinbom.app.R
import ir.pinbom.app.ui.UploadStatus
import ir.pinbom.app.ui.UploadUiState
import ir.pinbom.app.ui.theme.BrandRedSoft
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Localizer
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.localizeDecimal
import ir.pinbom.app.ui.theme.reduceMotion
import ir.pinbom.app.ui.theme.scaledTenths
import ir.pinbom.app.work.UploadStage
import ir.pinbom.app.work.labelRes
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · upload progress card
 *  The one surface that owns the whole upload story: the picked image in a ring
 *  that fills, a thick gradient bar that sweeps while the bytes are moving, and
 *  a percentage that counts up smoothly — capped at 99 until the server has
 *  actually accepted the pin, so nothing ever claims to be finished before it
 *  is. Then the check draws itself in, the card breathes out, and it dismisses
 *  itself a moment later.
 *
 *  It is deliberately not owned by a screen: it floats at the top of the shell,
 *  below the status bar, so leaving the create screen never loses sight of an
 *  upload that is still running.
 *
 *  Nothing here paints a raw colour: every tone comes from [PinBomTheme] or from
 *  Material's scheme, so light and dark are the same design twice.
 * ──────────────────────────────────────────────────────────────────────────── */

/** Thumbnail, ring and bar geometry. */
private val ThumbSize = 48.dp
private val BarHeight = 8.dp
private val RingStroke = 3.dp
private val PercentWidth = 56.dp

/**
 * Shows and hides the card, keeping the last state on screen long enough to
 * animate out. Drop it in a [Box] that is aligned to the top of the shell.
 */
@Composable
fun UploadProgressHost(
    state: UploadUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = state.status != UploadStatus.Idle
    val reduce = reduceMotion()

    /* The last visible state survives the exit animation, so the card slides
     * away as itself instead of collapsing into an empty frame. */
    var displayed by remember { mutableStateOf<UploadUiState?>(null) }
    LaunchedEffect(state) { if (state.status != UploadStatus.Idle) displayed = state }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = if (reduce) snap() else spring(dampingRatio = 0.75f, stiffness = 320f),
            initialOffsetY = { height -> -height },
        ) + fadeIn(animationSpec = tween(durationMillis = Motion.short)),
        exit = slideOutVertically(
            animationSpec = if (reduce) snap() else tween(durationMillis = Motion.medium, easing = Motion.emphasized),
            targetOffsetY = { height -> -height },
        ) + fadeOut(animationSpec = tween(durationMillis = Motion.instant)),
        modifier = modifier,
    ) {
        displayed?.let { shown ->
            UploadProgressCard(
                state = shown,
                onCancel = onCancel,
                onRetry = onRetry,
                onDismiss = onDismiss,
            )
        }
    }
}

/** The card itself — exported so the create screen may position it itself. */
@Composable
fun UploadProgressCard(
    state: UploadUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PinBomTheme.colors
    val errorColor = MaterialTheme.colorScheme.error
    val reduce = reduceMotion()
    val haptics = LocalHapticFeedback.current

    val success = state.status == UploadStatus.Success
    val failed = state.status == UploadStatus.Failed
    val running = state.status == UploadStatus.Running

    /* One animated number feeds the ring, the bar and the digits, so the three
     * can never disagree with each other. */
    val fill by animateFloatAsState(
        targetValue = if (success) 100f else state.percent.toFloat(),
        animationSpec = if (reduce) snap() else tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "uploadFill",
    )
    val shown = fill.roundToInt().coerceIn(0, 100)

    /* The moment the server says yes: one long press, once. */
    LaunchedEffect(success) {
        if (success) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /* The check mark draws itself in. */
    val check = remember { Animatable(0f) }
    LaunchedEffect(success) {
        if (success) {
            check.animateTo(1f, tween(durationMillis = 420, easing = Motion.standard))
        } else {
            check.snapTo(0f)
        }
    }

    val stageLabel = stringResource(state.stage.labelRes())
    val percentLabel = stringResource(R.string.upload_card_percent, Localizer.number(shown))

    /* TalkBack hears the milestones only: 25, 50, 75, 100 — never every tick.
     * The description below changes exactly four times per upload. */
    val announcement = stringResource(
        R.string.upload_card_announce,
        stageLabel,
        Localizer.number((state.percent / 25) * 25),
    )

    Row(
        modifier = modifier
            .widthIn(min = 280.dp, max = 440.dp)
            .shadow(
                elevation = 12.dp,
                shape = PinBomShape.pin,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = Color.Black.copy(alpha = 0.30f),
            )
            .clip(PinBomShape.pin)
            .background(colors.surface.copy(alpha = 0.96f))
            .padding(horizontal = Dimens.md, vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UploadThumbnail(
            model = state.thumbnail,
            progress = fill / 100f,
            success = success,
            failed = failed,
            check = check.value,
        )
        Spacer(Modifier.width(Dimens.md))

        if (failed) {
            FailureBody(
                message = state.error,
                tint = errorColor,
                onRetry = onRetry,
                onDismiss = onDismiss,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Crossfade(
                        targetState = stageLabel,
                        animationSpec = if (reduce) snap() else tween(durationMillis = Motion.short),
                        label = "uploadStage",
                        modifier = Modifier.weight(1f),
                    ) { label ->
                        Text(
                            text = label,
                            style = PinBomText.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(Dimens.sm))
                    Text(
                        text = percentLabel,
                        style = PinBomText.title.copy(fontFeatureSettings = "tnum"),
                        color = if (success) colors.success else colors.textPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .widthIn(min = PercentWidth)
                            .clearAndSetSemantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = announcement
                            },
                    )
                }

                Spacer(Modifier.height(Dimens.sm))

                UploadBar(
                    progress = fill / 100f,
                    shimmer = running,
                    tint = if (success) colors.success else colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fill, 0f..100f) },
                )

                val size = sizeLine(state.written, state.total, success)
                if (size.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.sm))
                    Text(
                        text = size,
                        style = PinBomText.caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                    )
                }
            }

            if (running) {
                Spacer(Modifier.width(Dimens.sm))
                IconCircleButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.upload_card_cancel),
                    onClick = onCancel,
                    size = Dimens.touch,
                    iconSize = 18.dp,
                    containerColor = colors.surfaceGrey,
                )
            }
        }
    }
}

/* ── The image being uploaded, with its ring ──────────────────────────────── */

@Composable
private fun UploadThumbnail(
    model: String?,
    progress: Float,
    success: Boolean,
    failed: Boolean,
    check: Float,
) {
    val colors = PinBomTheme.colors
    val errorColor = MaterialTheme.colorScheme.error
    val tint = when {
        success -> colors.success
        failed -> errorColor
        else -> colors.primary
    }

    Box(Modifier.size(ThumbSize)) {
        /* A grey disc under the photo: the ring stays readable while the image
         * is still decoding, and something is always there when it is gone. */
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.surfaceGrey),
        )

        val resolved = thumbModel(model)
        if (resolved != null) {
            AsyncImage(
                model = resolved,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }

        if (success || failed) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.scrim),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = colors.divider,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (progress > 0f) {
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            if (success) drawCheck(progress = check, color = colors.onImage)
        }
    }
}

/* ── The bar ──────────────────────────────────────────────────────────────── */

@Composable
private fun UploadBar(
    progress: Float,
    shimmer: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = PinBomTheme.colors
    val reduce = reduceMotion()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    /* The sweep keeps the bar alive between two ticks of real progress. */
    val transition = rememberInfiniteTransition(label = "uploadShimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "uploadSweep",
    )

    Canvas(
        modifier = modifier
            .height(BarHeight)
            .clip(PinBomShape.pill),
    ) {
        val width = size.width
        val height = size.height
        val radius = height / 2f
        val filled = (width * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)

        drawRoundRect(
            color = colors.surfaceGrey,
            size = Size(width, height),
            cornerRadius = CornerRadius(radius),
        )

        if (filled > 0f) {
            /* In RTL the bar grows from the right, so the fill and its gradient
             * are simply mirrored — the same design, the other direction. */
            val leading = if (rtl) width - filled else 0f
            val stops = if (rtl) listOf(BrandRedSoft, tint) else listOf(tint, BrandRedSoft)

            drawRoundRect(
                /* Anchored to the fill itself, so the gradient never depends on
                 * where in the track the bar happens to be. */
                brush = Brush.horizontalGradient(stops, startX = leading, endX = leading + filled),
                topLeft = Offset(leading, 0f),
                size = Size(filled, height),
                cornerRadius = CornerRadius(radius),
            )

            if (shimmer && !reduce) {
                val band = width * 0.45f
                val travel = if (rtl) 1f - sweep else sweep
                val start = -band + (width + band) * travel

                clipRect(left = leading, top = 0f, right = leading + filled, bottom = height) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.onImage.copy(alpha = 0.30f),
                                Color.Transparent,
                            ),
                            startX = start,
                            endX = start + band,
                        ),
                        topLeft = Offset(start, 0f),
                        size = Size(band, height),
                    )
                }
            }
        }
    }
}

/* ── The failed face ──────────────────────────────────────────────────────── */

@Composable
private fun FailureBody(
    message: String?,
    tint: Color,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PinBomTheme.colors

    Column(modifier) {
        Text(
            text = stringResource(R.string.upload_stage_failed),
            style = PinBomText.body,
            color = colors.textPrimary,
            maxLines = 1,
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.xs))
            Box(
                Modifier
                    .clip(PinBomShape.field)
                    .background(tint.copy(alpha = 0.10f))
                    .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            ) {
                /* Server wording, exactly as it arrived. */
                Text(
                    text = message,
                    style = PinBomText.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(Dimens.sm))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryPillButton(
                text = stringResource(R.string.upload_card_retry),
                onClick = onRetry,
                size = PillButtonSize.Small,
            )
            SecondaryPillButton(
                text = stringResource(R.string.upload_card_dismiss),
                onClick = onDismiss,
                size = PillButtonSize.Small,
            )
        }
    }
}

/* ── Bits ─────────────────────────────────────────────────────────────────── */

/** "۲٫۱ از ۴٫۸ مگابایت" / "2.1 / 4.8 MB" — one decimal, localised digits. */
@Composable
private fun sizeLine(written: Long, total: Long, success: Boolean): String {
    if (total <= 0L) return ""
    val done = if (success) total else written.coerceIn(0L, total)
    val persian = Localizer.isPersian

    return if (total >= 1_048_576L) {
        stringResource(
            R.string.upload_size_mb,
            localizeDecimal(scaledTenths(done / 1_048_576.0), persian),
            localizeDecimal(scaledTenths(total / 1_048_576.0), persian),
        )
    } else {
        stringResource(
            R.string.upload_size_kb,
            Localizer.number((done / 1024L).toInt()),
            Localizer.number((total / 1024L).toInt().coerceAtLeast(1)),
        )
    }
}

/** A content:// or file:// path is a Uri; anything else is left for Coil. */
private fun thumbModel(path: String?): Any? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("content:") || path.startsWith("file:")) Uri.parse(path) else path
}

/**
 * The check mark, stroked on as if drawn by hand: the short arm first, then the
 * long one, both with round caps. Purely geometric, so it scales with the box.
 */
private fun DrawScope.drawCheck(progress: Float, color: Color) {
    val stroke = size.minDimension * 0.12f
    val start = Offset(size.width * 0.28f, size.height * 0.52f)
    val corner = Offset(size.width * 0.44f, size.height * 0.68f)
    val end = Offset(size.width * 0.74f, size.height * 0.34f)
    val elbow = 0.42f

    if (progress <= elbow) {
        val t = (progress / elbow).coerceIn(0f, 1f)
        drawLine(
            color = color,
            start = start,
            end = point(start, corner, t),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    } else {
        drawLine(color = color, start = start, end = corner, strokeWidth = stroke, cap = StrokeCap.Round)
        val t = ((progress - elbow) / (1f - elbow)).coerceIn(0f, 1f)
        drawLine(
            color = color,
            start = corner,
            end = point(corner, end, t),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun point(from: Offset, to: Offset, fraction: Float): Offset =
    Offset(from.x + (to.x - from.x) * fraction, from.y + (to.y - from.y) * fraction)

/* ── Previews ─────────────────────────────────────────────────────────────── */

@Composable
private fun UploadCardGallery(dark: Boolean) {
    PinBomTheme(darkTheme = dark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PinBomTheme.colors.background)
                .padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg),
        ) {
            UploadProgressCard(
                state = UploadUiState(
                    status = UploadStatus.Running,
                    percent = 42,
                    stage = UploadStage.Uploading,
                    written = 2_200_000L,
                    total = 4_800_000L,
                ),
                onCancel = {},
                onRetry = {},
                onDismiss = {},
            )
            UploadProgressCard(
                state = UploadUiState(
                    status = UploadStatus.Success,
                    percent = 100,
                    stage = UploadStage.Done,
                    written = 4_800_000L,
                    total = 4_800_000L,
                ),
                onCancel = {},
                onRetry = {},
                onDismiss = {},
            )
            UploadProgressCard(
                state = UploadUiState(
                    status = UploadStatus.Failed,
                    stage = UploadStage.Failed,
                    error = "HTTP 413 · payload too large",
                ),
                onCancel = {},
                onRetry = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "Upload card · light", backgroundColor = 0xFFEDEDED, widthDp = 420, heightDp = 420)
@Composable
private fun PreviewUploadCardLight() = UploadCardGallery(dark = false)

@Preview(name = "Upload card · dark", backgroundColor = 0xFF121212, widthDp = 420, heightDp = 420)
@Composable
private fun PreviewUploadCardDark() = UploadCardGallery(dark = true)
