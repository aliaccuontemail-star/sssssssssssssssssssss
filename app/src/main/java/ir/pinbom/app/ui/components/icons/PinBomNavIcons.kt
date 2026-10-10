package ir.pinbom.app.ui.components.icons

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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · navigation icons
 *  Five line icons drawn for the navigation bar only: 24×24 viewport, 1.75 unit
 *  stroke, round caps and joins, no fills. They are the *only* place these
 *  glyphs exist — nothing else in the app uses them.
 *
 *  Geometry rule for every glyph: the art is centred on (12, 12) and the
 *  outline stops ~2.5–3 units short of the viewport edge, so all five read at
 *  the same optical size and the 1.75 stroke (0.875 either side of the centre
 *  line) never touches the 24×24 box. The two circle glyphs — the magnifier and
 *  the profile head — are authored centre-first for exactly that reason: an arc
 *  chain has to start on the circle, and starting from a corner is what pushed
 *  the old Search circle to (17.3, 4.5) and the old Profile head to (15.85, 4.25).
 *
 *  The stroke colour here is a placeholder: `Icon()` tints the whole vector
 *  through a colour filter, so the active/inactive colours come from the bar.
 * ──────────────────────────────────────────────────────────────────────────── */

private val StrokeWidth = 1.75f

/** One line-style glyph: no fill, one stroke, round caps and joins. */
private fun navIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = StrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )
    }.build()

object PinBomNavIcons {

    /**
     * A house with a rounded roof and a small lintel line inside.
     * Path bounds: x 3.65…20.35, y 4.15…20.40 — centred on (12, 12.28).
     */
    val Home: ImageVector by lazy {
        navIcon("PinBom.Nav.Home") {
            // Walls, rounded roof and floor, closed in one outline.
            moveTo(3.65f, 18.40f)
            lineTo(3.65f, 10.60f)
            curveTo(3.65f, 9.75f, 4.06f, 8.90f, 4.78f, 8.35f)
            lineTo(10.65f, 4.15f)
            curveTo(11.46f, 3.56f, 12.54f, 3.56f, 13.35f, 4.15f)
            lineTo(19.22f, 8.35f)
            curveTo(19.94f, 8.90f, 20.35f, 9.75f, 20.35f, 10.60f)
            lineTo(20.35f, 18.40f)
            curveTo(20.35f, 19.50f, 19.38f, 20.40f, 18.19f, 20.40f)
            lineTo(5.81f, 20.40f)
            curveTo(4.62f, 20.40f, 3.65f, 19.50f, 3.65f, 18.40f)
            close()
            // The lintel line inside the house.
            moveTo(9.41f, 16.00f)
            lineTo(14.59f, 16.00f)
        }
    }

    /**
     * A rounded magnifier: a circle centred on (10.5, 10.5) with radius 6.25,
     * plus the handle running to (20, 20).
     * Path bounds: x 4.25…20.00, y 4.25…20.00 — centred on (12, 12).
     */
    val Search: ImageVector by lazy {
        navIcon("PinBom.Nav.Search") {
            /* The circle starts at its left extreme — (centre.x − r, centre.y) —
             * so the two semicircle arcs (dx = +2r, then −2r) close on the same
             * point and leave the centre exactly where it was authored. */
            moveTo(4.25f, 10.50f)
            arcToRelative(
                a = 6.25f, b = 6.25f, theta = 0f,
                isMoreThanHalf = true, isPositiveArc = true,
                dx1 = 12.50f, dy1 = 0f,
            )
            arcToRelative(
                a = 6.25f, b = 6.25f, theta = 0f,
                isMoreThanHalf = true, isPositiveArc = true,
                dx1 = -12.50f, dy1 = 0f,
            )
            close()
            // The handle, leaving the circle at its lower-right diagonal.
            moveTo(15.20f, 15.20f)
            lineTo(20.00f, 20.00f)
        }
    }

    /**
     * A rounded square with a plus inside.
     * Path bounds: x 3.70…20.30, y 3.70…20.30 — centred on (12, 12).
     */
    val Create: ImageVector by lazy {
        navIcon("PinBom.Nav.Create") {
            moveTo(8.70f, 3.70f)
            lineTo(15.30f, 3.70f)
            arcToRelative(5f, 5f, 0f, false, true, 5f, 5f)
            lineTo(20.30f, 15.30f)
            arcToRelative(5f, 5f, 0f, false, true, -5f, 5f)
            lineTo(8.70f, 20.30f)
            arcToRelative(5f, 5f, 0f, false, true, -5f, -5f)
            lineTo(3.70f, 8.70f)
            arcToRelative(5f, 5f, 0f, false, true, 5f, -5f)
            close()
            // The plus, centred in the square.
            moveTo(12.00f, 8.50f)
            lineTo(12.00f, 15.50f)
            moveTo(8.50f, 12.00f)
            lineTo(15.50f, 12.00f)
        }
    }

    /**
     * A rounded bell with a small clapper arc below it: a dome, a flared skirt
     * closed by the rim line, and the clapper centred under it.
     * Path bounds: x 4.20…19.80, y 3.80…20.42 — centred on (12, 12.11).
     */
    val Notifications: ImageVector by lazy {
        navIcon("PinBom.Nav.Notifications") {
            moveTo(4.20f, 17.20f)
            curveTo(5.60f, 16.00f, 6.50f, 14.30f, 6.50f, 12.20f)
            lineTo(6.50f, 9.90f)
            curveTo(6.50f, 6.50f, 9.10f, 3.80f, 12.00f, 3.80f)
            curveTo(14.90f, 3.80f, 17.50f, 6.50f, 17.50f, 9.90f)
            lineTo(17.50f, 12.20f)
            curveTo(17.50f, 14.30f, 18.40f, 16.00f, 19.80f, 17.20f)
            close()
            // The clapper.
            moveTo(9.80f, 19.20f)
            curveTo(10.50f, 20.42f, 13.50f, 20.42f, 14.20f, 19.20f)
        }
    }

    /**
     * A head above rounded shoulders. The head is a circle centred on (12, 8)
     * with radius 3.75; the shoulders are symmetric about x = 12.
     * Path bounds: x 4.25…19.75, y 4.25…20.00 — centred on (12, 12.13).
     */
    val Profile: ImageVector by lazy {
        navIcon("PinBom.Nav.Profile") {
            /* Same centre-first trick as the magnifier: start at the circle's
             * left extreme, (centre.x − r, centre.y). */
            moveTo(8.25f, 8.00f)
            arcToRelative(
                a = 3.75f, b = 3.75f, theta = 0f,
                isMoreThanHalf = true, isPositiveArc = true,
                dx1 = 7.50f, dy1 = 0f,
            )
            arcToRelative(
                a = 3.75f, b = 3.75f, theta = 0f,
                isMoreThanHalf = true, isPositiveArc = true,
                dx1 = -7.50f, dy1 = 0f,
            )
            close()
            // Shoulders, centred under the head.
            moveTo(4.60f, 20.00f)
            curveTo(4.60f, 16.90f, 7.90f, 14.50f, 12.00f, 14.50f)
            curveTo(16.10f, 14.50f, 19.40f, 16.90f, 19.40f, 20.00f)
        }
    }
}

/* ── Preview ─────────────────────────────────────────────────────────────────
 * Every glyph, at the three sizes the app uses (slot icon 24, rail 28, large
 * 48), on both bar fills, in the idle and the active tint — which is the only
 * way a clipped or off-centre path is visible before it ships.
 * ─────────────────────────────────────────────────────────────────────────── */

@Composable
private fun IconRow(icons: List<Pair<String, ImageVector>>, tint: Color, size: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEach { (_, vector) ->
            Icon(imageVector = vector, contentDescription = null, tint = tint, modifier = Modifier.size(size))
        }
    }
}

@Composable
private fun IconBoard(dark: Boolean) {
    val bar = PinBomTheme.navigationBar
    val icons = listOf(
        "home" to PinBomNavIcons.Home,
        "search" to PinBomNavIcons.Search,
        "create" to PinBomNavIcons.Create,
        "notifications" to PinBomNavIcons.Notifications,
        "profile" to PinBomNavIcons.Profile,
    )

    Column(
        modifier = Modifier
            .background(bar.fill)
            .padding(Dimens.md),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        listOf(24.dp, 28.dp, 48.dp).forEach { size ->
            IconRow(icons = icons, tint = bar.idle, size = size)
            Spacer(Modifier.height(Dimens.xxs))
            IconRow(icons = icons, tint = bar.accent, size = size)
            Spacer(Modifier.height(Dimens.xs))
        }
        /* The hairline colour, on the bar itself: invisible on the dark pill,
         * a real edge on the light one. */
        Box(Modifier.fillMaxWidth().height(1.dp).background(bar.hairline))
    }
}

@Preview(name = "Nav icons · light bar", backgroundColor = 0xFFF4F4F6, widthDp = 320, heightDp = 300)
@Composable
private fun PreviewNavIconsLight() {
    PinBomTheme(darkTheme = false) { IconBoard(dark = false) }
}

@Preview(name = "Nav icons · dark bar", backgroundColor = 0xFF2C2C31, widthDp = 320, heightDp = 300)
@Composable
private fun PreviewNavIconsDark() {
    PinBomTheme(darkTheme = true) { IconBoard(dark = true) }
}

/** A single glyph, large, on both fills — the fastest way to eyeball centring. */
@Preview(name = "Nav icons · centring", backgroundColor = 0xFFF4F4F6, widthDp = 320, heightDp = 200)
@Composable
private fun PreviewNavIconCentring() {
    PinBomTheme(darkTheme = false) {
        val bar = PinBomTheme.navigationBar
        Row(
            modifier = Modifier
                .background(bar.fill)
                .padding(Dimens.md),
            horizontalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            listOf(PinBomNavIcons.Home, PinBomNavIcons.Search, PinBomNavIcons.Create, PinBomNavIcons.Notifications)
                .forEach { vector ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(bar.indicator),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = null,
                            tint = bar.accent,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
        }
    }
}
