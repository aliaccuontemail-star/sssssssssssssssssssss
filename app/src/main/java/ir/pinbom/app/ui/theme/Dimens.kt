package ir.pinbom.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · spacing, sizing and rhythm
 *  One scale, seven steps. If a value is not in here it is a bug, not a taste.
 * ──────────────────────────────────────────────────────────────────────────── */

object Dimens {
    /** 2 · hairline offsets, badge nudges. */
    val xxs: Dp = 2.dp

    /** 4 · icon–text micro gaps, collage gaps. */
    val xs: Dp = 4.dp

    /** 8 · the grid gap and the default small gap. */
    val sm: Dp = 8.dp

    /** 12 · inside cards, between stacked fields. */
    val md: Dp = 12.dp

    /** 16 · screen padding, section padding. */
    val lg: Dp = 16.dp

    /** 20 · generous inside-sheet padding. */
    val xl: Dp = 20.dp

    /** 24 · sheet top padding, hero breathing room. */
    val xxl: Dp = 24.dp

    /** 32 · display spacing, hero blocks. */
    val xxxl: Dp = 32.dp

    /** 48 · empty-state blocks, large vertical rhythm. */
    val huge: Dp = 48.dp

    // Semantic names on top of the scale.
    /** Horizontal padding of every screen. */
    val screen = 16.dp

    /** The masonry grid uses tighter edges than text screens. */
    val grid = 8.dp

    /** Gap between masonry items. */
    val gridGap = 8.dp

    /** Standard list/toolbar row. */
    val row = 56.dp

    /** Bottom navigation height (plus the gesture inset). */
    val bottomBar = 64.dp

    /**
     * The room a scrolling tab screen keeps free at its end: the pill (64dp), its
     * bottom margin (12dp) and a little air (12dp). The pill floats *over* the
     * content, so without this the last row would hide behind it.
     */
    val navBarSpace = 88.dp

    /** Top app bar height when a screen uses one. */
    val topBar = 56.dp

    /** Minimum touch target. Never go below. */
    val touch: Dp = 48.dp

    /** Small circular icon button. */
    val iconButton = 40.dp

    /** Icon button that must not shift layout inside dense rows. */
    val iconButtonSmall = 32.dp

    /** Pill button heights. */
    val button = 48.dp
    val buttonSmall = 36.dp

    /** Chip height. */
    val chip = 36.dp

    /** Board row cover. */
    val boardThumb = 56.dp

    /** Notification trailing thumbnail. */
    val notificationThumb = 48.dp

    /** Progress/loading rule thickness. */
    val progressBar = 3.dp

    /** Navigation breakpoint: below this width the app uses the bottom bar. */
    val compactWidth = 600.dp

    /** How far a pushed screen slides in (the rest of the motion is fade). */
    val navPush = 24.dp
}
