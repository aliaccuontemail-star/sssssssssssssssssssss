package ir.pinbom.app.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · shared pin transition
 *  A pin tile in any wall and the hero on the pin screen are the same object, so
 *  they share bounds: the tile's bounds animate into the hero's on the way in,
 *  and back on the way out. The scopes travel per route through a composition
 *  local, which keeps every screen signature unchanged — a surface that never
 *  publishes a scope (search results, previews) simply gets its modifier back.
 * ──────────────────────────────────────────────────────────────────────────── */

/** The two scopes [sharedPinBounds] needs, published once per pin route. */
@Immutable
internal class SharedPinScopes(
    val shared: SharedTransitionScope,
    val animated: AnimatedVisibilityScope,
)

internal val LocalSharedPinScopes = staticCompositionLocalOf<SharedPinScopes?> { null }

/**
 * Publishes the scopes a route's walls and its pin hero need. Whatever is inside
 * keeps working without them, so a route can opt in without touching its screen.
 */
@Composable
internal fun SharedPinRoute(
    shared: SharedTransitionScope,
    animated: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    val scopes = remember(shared, animated) { SharedPinScopes(shared, animated) }
    CompositionLocalProvider(LocalSharedPinScopes provides scopes, content = content)
}

/** One key per pin, so a tile matches the hero whichever wall opened it. */
internal fun sharedPinKey(id: Long): String = "pin-$id"

/**
 * Tags a layout as the shared bounds of pin [id]. A no-op when the route did not
 * publish a scope, so the same card is safe in previews and in lists that are
 * not walls. Bounds morph with the default spring; content cross-fades, which is
 * what makes a tile (image + author row) become a hero (image only).
 */
@Composable
internal fun Modifier.sharedPinBounds(id: Long): Modifier {
    val scopes = LocalSharedPinScopes.current ?: return this
    return with(scopes.shared) {
        this@sharedPinBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(sharedPinKey(id)),
            animatedVisibilityScope = scopes.animated,
        )
    }
}
