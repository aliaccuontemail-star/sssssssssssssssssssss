package ir.pinbom.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · shape tokens
 *  Pinterest’s language is “generous radius, zero shadow”. Everything soft,
 *  everything flat, nothing raised.
 * ──────────────────────────────────────────────────────────────────────────── */

val PinBomShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object PinBomShape {
    /** Pin image, image tiles, toast. */
    val pin = RoundedCornerShape(16.dp)

    /** Board cover collage, large search tiles, create-image frame. */
    val boardCover = RoundedCornerShape(20.dp)

    /** Big “discover” tiles. */
    val searchTile = RoundedCornerShape(24.dp)

    /** Bottom sheets (top corners only). */
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Dialogs. */
    val dialog = RoundedCornerShape(28.dp)

    /** Chips, buttons, search bar, single-line fields — fully pill. */
    val pill = RoundedCornerShape(percent = 50)

    /** Multiline fields & small inline surfaces. */
    val field = RoundedCornerShape(16.dp)

    val avatar = CircleShape

    /** 12dp — notification thumbnails, small thumbs in lists. */
    val thumb = RoundedCornerShape(12.dp)

    /** 18dp — image in the pin detail hero. */
    val hero = RoundedCornerShape(28.dp)
}
