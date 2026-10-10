package ir.pinbom.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ir.pinbom.app.R
import ir.pinbom.app.ui.components.PinBomNavItem
import ir.pinbom.app.ui.components.icons.PinBomNavIcons

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · the route table, in one place
 *  These strings are the contract with every deep link, shortcut and
 *  `nav.navigate(...)` call in the app. They existed before the redesign and
 *  they are not allowed to change — only the shell around them did.
 * ──────────────────────────────────────────────────────────────────────────── */

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CREATE = "create"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"

    const val SETTINGS = "settings"
    const val BOARDS = "boards"

    /** `profile/{username}` */
    const val PROFILE_ARG = "profile/{username}"

    /** `board/{id}` */
    const val BOARD_ARG = "board/{id}"

    /** `pin/{id}` */
    const val PIN_ARG = "pin/{id}"

    /** Argument names, used by both the graph and the deep links. */
    const val ARG_USERNAME = "username"
    const val ARG_ID = "id"

    fun profile(username: String) = "$PROFILE/$username"
    fun board(id: Long) = "board/$id"
    fun pin(id: Long) = "pin/$id"
}

/** The five shell destinations, in visual order. */
sealed class Destination(val route: String, val labelRes: Int) {
    data object Home : Destination(Routes.HOME, R.string.nav_home)
    data object Search : Destination(Routes.SEARCH, R.string.nav_search)
    data object Create : Destination(Routes.CREATE, R.string.nav_create)
    data object Notifications : Destination(Routes.NOTIFICATIONS, R.string.nav_notifications)
    data object Profile : Destination(Routes.PROFILE, R.string.nav_profile)
}

val Tabs: List<Destination> = listOf(
    Destination.Home,
    Destination.Search,
    Destination.Create,
    Destination.Notifications,
    Destination.Profile,
)

/** Routes that keep the shell (bottom bar / rail) visible. */
private val TabRouteSet: Set<String> = Tabs.map { it.route }.toSet()

/** Every other route is a pushed detail and hides the shell. */
fun isTabRoute(route: String?): Boolean = route != null && route in TabRouteSet

/**
 * The shell items. The profile destination is drawn as the user’s avatar —
 * [profileName] feeds the initials (and its deterministic colour), [profileUrl]
 * is reserved for the day the API returns an avatar.
 */
@Composable
fun pinbomTabs(profileName: String?, profileUrl: String? = null): List<PinBomNavItem> = listOf(
    PinBomNavItem(
        route = Destination.Home.route,
        label = stringResource(R.string.nav_home),
        icon = PinBomNavIcons.Home,
    ),
    PinBomNavItem(
        route = Destination.Search.route,
        label = stringResource(R.string.nav_search),
        icon = PinBomNavIcons.Search,
    ),
    PinBomNavItem(
        route = Destination.Create.route,
        label = stringResource(R.string.nav_create),
        icon = PinBomNavIcons.Create,
    ),
    PinBomNavItem(
        route = Destination.Notifications.route,
        label = stringResource(R.string.nav_notifications),
        icon = PinBomNavIcons.Notifications,
    ),
    PinBomNavItem(
        route = Destination.Profile.route,
        label = stringResource(R.string.nav_profile),
        icon = PinBomNavIcons.Profile,
        isProfile = true,
        avatarName = profileName,
        avatarUrl = profileUrl,
    ),
)
