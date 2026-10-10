package ir.pinbom.app.ui.nav

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.ui.UploadViewModel
import ir.pinbom.app.ui.components.PinBomBottomBar
import ir.pinbom.app.ui.components.PinBomRail
import ir.pinbom.app.ui.components.NavBarStyle
import ir.pinbom.app.ui.components.UploadProgressHost
import ir.pinbom.app.ui.screens.BoardScreen
import ir.pinbom.app.ui.screens.BoardsScreen
import ir.pinbom.app.ui.screens.CreatePinScreen
import ir.pinbom.app.ui.screens.HomeScreen
import ir.pinbom.app.ui.screens.NotificationsScreen
import ir.pinbom.app.ui.screens.PinDetailScreen
import ir.pinbom.app.ui.screens.ProfileScreen
import ir.pinbom.app.ui.screens.SearchScreen
import ir.pinbom.app.ui.screens.SettingsScreen
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.BrandRed
import androidx.compose.ui.graphics.Color
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.reduceMotion
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · navigation shell
 *  One NavHost, one shell. The floating pill (phones) or the floating rail
 *  (≥600dp) stays up on the five tabs and slides away on any pushed detail, so a
 *  pin can own the whole screen. Route strings, arguments and deep links are
 *  untouched: `pinbom://pin/{id}`, `pinbom://create`, `pinbom://search` still land here.
 *
 *  The shell lives inside one [SharedTransitionLayout], so the tile a user taps
 *  in any wall and the hero of the pin it opens are the same object for the
 *  duration of the push. Routes that show pins publish their scopes with
 *  [SharedPinRoute]; routes that don't simply never see them.
 *
 *  Motion is directional now. A tab switch is a fade-through (no slide: the two
 *  screens are siblings, not a hierarchy), a pushed screen travels a *fraction*
 *  of the width while the one behind it steps back and dims, the create screen
 *  rises from the bottom edge, and a pop is the exact mirror of the push. With
 *  reduced motion everything collapses to a hard cut.
 * ──────────────────────────────────────────────────────────────────────────── */

/** Tab switches: out fast, in settled — 220ms end to end. */
private const val TAB_OUT_MS = 90
private const val TAB_IN_MS = 220

/** A pushed screen travels 12% of the window; the one behind steps back 6%. */
private const val PUSH_TRAVEL = 0.12f
private const val PUSH_REST = 0.06f

/** While the shell is this busy, a second tap is a bounce, not a navigation. */
private const val NAV_DEBOUNCE_MS = 320L

@Composable
fun AppNavGraph(
    nav: NavHostController,
    session: SessionManager,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    accentColor: Color = BrandRed,
    onAccentChange: (Color) -> Unit = {},
    navStyle: NavBarStyle = NavBarStyle.Default,
    onNavStyleChange: (NavBarStyle) -> Unit = {},
) {
    SharedTransitionLayout {
        AppNavShell(
            nav = nav,
            session = session,
            dark = dark,
            onDarkChange = onDarkChange,
            onLogout = onLogout,
            accentColor = accentColor,
            onAccentChange = onAccentChange,
            navStyle = navStyle,
            onNavStyleChange = onNavStyleChange,
            sharedScope = this,
        )
    }
}

@Composable
private fun AppNavShell(
    nav: NavHostController,
    session: SessionManager,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    accentColor: Color,
    onAccentChange: (Color) -> Unit,
    navStyle: NavBarStyle,
    onNavStyleChange: (NavBarStyle) -> Unit,
    sharedScope: SharedTransitionScope,
) {
    val colors = PinBomTheme.colors
    val currentEntry by nav.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val shellVisible = isTabRoute(currentRoute)

    val tabs = pinbomTabs(profileName = session.username)

    /* One upload, one card, for the whole shell. Hoisted *above* the NavHost on
     * purpose: the card is resolved against the activity, so it survives every
     * navigation and rotation, and the create screen borrows this very instance
     * instead of owning an upload of its own. */
    val uploads: UploadViewModel = hiltViewModel()

    /* Rapid taps on the bar used to stack duplicate entries — and a duplicate
     * entry means a second ViewModel, a second pager and a second round of
     * network calls for a screen the user was already on. One gate, one frame of
     * patience. */
    var lastNavAt by remember { mutableStateOf(0L) }

    fun open(route: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastNavAt < NAV_DEBOUNCE_MS) return
        lastNavAt = now
        nav.navigate(route) {
            /* The canonical shell navigation: one copy of each tab, each tab
             * keeps what it had (scroll, ViewModel, draft) and the back stack
             * stays a back stack instead of a history of visits. */
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val reduce = reduceMotion()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    /* +1 when “forward” means “towards the right edge”. */
    val forwardSign = if (rtl) -1f else 1f

    /* A fraction of the window, not the whole width: the pushed screen travels
     * far enough to read as “in front”, not far enough to feel like a slideshow. */
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val travelPx = windowWidth * PUSH_TRAVEL
    val restPx = windowWidth * PUSH_REST

    /* ── The four motions ─────────────────────────────────────────────────── */

    /* Siblings: fade through, with a whisper of scale. */
    val tabIn: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = TAB_IN_MS, easing = Motion.standard),
    ) + scaleIn(
        animationSpec = tween(durationMillis = TAB_IN_MS, easing = Motion.standard),
        initialScale = 0.98f,
    )
    val tabOut: ExitTransition = fadeOut(
        animationSpec = tween(durationMillis = TAB_OUT_MS, easing = Motion.standard),
    ) + scaleOut(
        animationSpec = tween(durationMillis = TAB_OUT_MS, easing = Motion.standard),
        targetScale = 0.98f,
    )

    /* A push: forward 12%, fade in; the screen behind steps back 6% and dims. */
    val pushIn: EnterTransition = slideInHorizontally(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        initialOffsetX = { (it * PUSH_TRAVEL * forwardSign).roundToInt() },
    ) + fadeIn(animationSpec = tween(durationMillis = Motion.short, easing = Motion.standard))

    val pushBack: ExitTransition = slideOutHorizontally(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        targetOffsetX = { (-it * PUSH_REST * forwardSign).roundToInt() },
    ) + fadeOut(
        animationSpec = tween(durationMillis = Motion.short, easing = Motion.standard),
        targetAlpha = 0.92f,
    )

    /* The exact mirror: the screen behind comes back, the pushed one leaves. */
    val popBackIn: EnterTransition = slideInHorizontally(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        initialOffsetX = { (-it * PUSH_REST * forwardSign).roundToInt() },
    ) + fadeIn(
        animationSpec = tween(durationMillis = Motion.short, easing = Motion.standard),
        initialAlpha = 0.92f,
    )

    val popOut: ExitTransition = slideOutHorizontally(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        targetOffsetX = { (it * PUSH_TRAVEL * forwardSign).roundToInt() },
    ) + fadeOut(animationSpec = tween(durationMillis = Motion.medium, easing = Motion.standard))

    /* The composer rises from the bottom edge and settles back down. */
    val createIn: EnterTransition = slideInVertically(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        initialOffsetY = { it / 6 },
    ) + fadeIn(animationSpec = tween(durationMillis = Motion.short, easing = Motion.standard))

    val createOut: ExitTransition = slideOutVertically(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        targetOffsetY = { it / 6 },
    ) + fadeOut(
        animationSpec = tween(durationMillis = Motion.short, easing = Motion.standard),
        targetAlpha = 0f,
    )

    /* The rail slides its own width out; the pill drops out of the bottom edge.
     * Both are handed to file-scope wrappers below — inside a Row or a Box the
     * scoped overloads of AnimatedVisibility would shadow the plain one, and a
     * wrapper has no receiver to shadow anything. */
    val railIn: EnterTransition = expandHorizontally(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
    )
    val railOut: ExitTransition = shrinkHorizontally(
        animationSpec = tween(durationMillis = Motion.short, easing = Motion.emphasized),
    )
    val pillIn: EnterTransition = slideInVertically(
        animationSpec = tween(durationMillis = Motion.medium, easing = Motion.emphasized),
        initialOffsetY = { it },
    ) + fadeIn(animationSpec = tween(durationMillis = Motion.short, easing = Motion.standard))
    val pillOut: ExitTransition = slideOutVertically(
        animationSpec = tween(durationMillis = Motion.short, easing = Motion.emphasized),
        targetOffsetY = { it },
    ) + fadeOut(animationSpec = tween(durationMillis = Motion.instant))

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < Dimens.compactWidth

        Row(Modifier.fillMaxSize().clipToBounds()) {
            if (!compact) {
                RailReveal(
                    visible = shellVisible,
                    enter = railIn,
                    exit = railOut,
                ) {
                    PinBomRail(
                        items = tabs,
                        currentRoute = currentRoute,
                        onSelect = { open(it.route) },
                        baseUrl = session.baseUrl,
                        onLogoClick = { open(Destination.Home.route) },
                        style = navStyle,
                    )
                }
            }

            /* One Box owns the shell and the floating pill. The pill has to
             * sit *outside* the Scaffold's content insets: it adds its own
             * navigation-bar padding, and inside those insets the system bar
             * would be counted twice. */
            Box(
                Modifier.weight(1f).fillMaxHeight().pointerInput(currentRoute, shellVisible, rtl) {
                    if (shellVisible) {
                        var dragDistance = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                dragDistance += amount
                                if (kotlin.math.abs(dragDistance) > size.width * 0.10f) change.consume()
                            },
                            onDragEnd = {
                                val threshold = size.width * 0.18f
                                if (kotlin.math.abs(dragDistance) >= threshold) {
                                    val currentIndex = Tabs.indexOfFirst { it.route == currentRoute }
                                    if (currentIndex >= 0) {
                                        val physicalStep = if (dragDistance < 0f) 1 else -1
                                        val logicalStep = if (rtl) -physicalStep else physicalStep
                                        val nextIndex = (currentIndex + logicalStep).coerceIn(0, Tabs.lastIndex)
                                        if (nextIndex != currentIndex) open(Tabs[nextIndex].route)
                                    }
                                }
                                dragDistance = 0f
                            },
                            onDragCancel = { dragDistance = 0f },
                        )
                    }
                },
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = colors.background,
                ) { innerPadding ->
                    /* The NavHost no longer reserves room for the bar: the pill
                     * floats over the content, so a wall keeps its images to the very
                     * bottom of the screen and the tab screens add the matching
                     * [Dimens.navBarSpace] to their own end padding. */
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background),
                    ) {
                        NavHost(
                            navController = nav,
                            startDestination = Destination.Home.route,
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            enterTransition = {
                                when {
                                    reduce -> EnterTransition.None
                                    isTabRoute(targetState.destination.route) -> tabIn
                                    targetState.destination.route == Destination.Create.route -> createIn
                                    else -> pushIn
                                }
                            },
                            exitTransition = {
                                when {
                                    reduce -> ExitTransition.None
                                    isTabRoute(targetState.destination.route) -> tabOut
                                    else -> pushBack
                                }
                            },
                            popEnterTransition = {
                                when {
                                    reduce -> EnterTransition.None
                                    isTabRoute(initialState.destination.route) -> tabIn
                                    else -> popBackIn
                                }
                            },
                            popExitTransition = {
                                when {
                                    reduce -> ExitTransition.None
                                    isTabRoute(initialState.destination.route) -> tabOut
                                    initialState.destination.route == Destination.Create.route -> createOut
                                    else -> popOut
                                }
                            },
                        ) {
                            composable(Destination.Home.route) {
                                SharedPinRoute(sharedScope, this) { HomeScreen(nav, session) }
                            }
                            composable(Destination.Search.route) { SearchScreen(nav, session) }
                            composable(Destination.Create.route) {
                                CreatePinScreen(session, uploads = uploads)
                            }
                            composable(Destination.Notifications.route) { NotificationsScreen(nav, session) }
                            composable(Destination.Profile.route) {
                                SharedPinRoute(sharedScope, this) { ProfileScreen(nav, session) }
                            }

                            composable(Routes.PROFILE_ARG) { entry ->
                                SharedPinRoute(sharedScope, this) {
                                    ProfileScreen(
                                        nav,
                                        session,
                                        entry.arguments?.getString(Routes.ARG_USERNAME).orEmpty(),
                                    )
                                }
                            }
                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    nav = nav,
                                    session = session,
                                    dark = dark,
                                    onDarkChange = onDarkChange,
                                    accentColor = accentColor,
                                    onAccentChange = onAccentChange,
                                    onLogout = onLogout,
                                    navStyle = navStyle,
                                    onNavStyleChange = onNavStyleChange,
                                )
                            }
                            composable(Routes.BOARDS) { BoardsScreen(nav, session) }
                            composable(Routes.BOARD_ARG) { entry ->
                                SharedPinRoute(sharedScope, this) {
                                    BoardScreen(
                                        nav,
                                        entry.arguments?.getString(Routes.ARG_ID)?.toLongOrNull() ?: 0L,
                                        session,
                                    )
                                }
                            }
                            composable(Routes.PIN_ARG) { entry ->
                                SharedPinRoute(sharedScope, this) {
                                    PinDetailScreen(
                                        nav,
                                        entry.arguments?.getString(Routes.ARG_ID)?.toLongOrNull() ?: 0L,
                                        session,
                                    )
                                }
                            }
                        }

                        /* The upload card: floating, never in the way, below the
                         * status bar and above every screen for as long as the upload
                         * is unfinished — that is what makes it survive navigation.
                         * The progress stream is collected *inside* the overlay, so a
                         * tick redraws the card instead of recomposing the shell. */
                        UploadOverlay(
                            uploads = uploads,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(
                                    top = innerPadding.calculateTopPadding() + Dimens.sm,
                                    start = Dimens.screen,
                                    end = Dimens.screen,
                                ),
                        )
                    }
                }

                /* The floating pill: over the content, under nothing, and
                 * perfectly still while the user switches tabs. It only travels
                 * when the shell itself leaves (tab → detail). */
                if (compact) {
                    BarReveal(
                        visible = shellVisible,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = pillIn,
                        exit = pillOut,
                    ) {
                        PinBomBottomBar(
                            items = tabs,
                            currentRoute = currentRoute,
                            onSelect = { open(it.route) },
                            baseUrl = session.baseUrl,
                            style = navStyle,
                        )
                    }
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 *  The upload card, walled off from the shell.
 *
 *  Upload progress arrives as WorkInfo updates: several per second, per file.
 *  Reading that stream here rather than in [AppNavShell] keeps each tick inside
 *  this one composable — the shell, the NavHost and the bar do not recompose for
 *  a progress bar they never read.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun UploadOverlay(
    uploads: UploadViewModel,
    modifier: Modifier = Modifier,
) {
    val state by uploads.state.collectAsStateWithLifecycle()
    UploadProgressHost(
        state = state,
        onCancel = uploads::cancel,
        onRetry = uploads::retry,
        onDismiss = uploads::dismiss,
        modifier = modifier,
    )
}

/* ────────────────────────────────────────────────────────────────────────────
 *  The two chrome reveals, at file scope on purpose.
 *
 *  `AnimatedVisibility` is declared three times — once plain, once on RowScope
 *  and once on ColumnScope — and inside a Row or a Column the scoped overloads
 *  win. These wrappers take the transitions as arguments instead, so the call
 *  site cannot be ambiguous and the motion lives in one place.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun RailReveal(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = enter, exit = exit) { content() }
}

@Composable
private fun BarReveal(
    visible: Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = enter, exit = exit) { content() }
}
