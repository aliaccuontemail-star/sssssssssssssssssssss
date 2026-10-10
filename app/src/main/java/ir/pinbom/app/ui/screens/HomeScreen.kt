package ir.pinbom.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.local.PinEntity
import ir.pinbom.app.data.remote.Author
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.ui.HomeViewModel
import ir.pinbom.app.ui.components.AppendSpinner
import ir.pinbom.app.ui.components.PinBomBrand
import ir.pinbom.app.ui.components.PinBomChipRow
import ir.pinbom.app.ui.components.PinBomPullToRefresh
import ir.pinbom.app.ui.components.EmptyState
import ir.pinbom.app.ui.components.ErrorState
import ir.pinbom.app.ui.components.IconCircleButton
import ir.pinbom.app.ui.components.OfflineBanner
import ir.pinbom.app.ui.components.PinCard
import ir.pinbom.app.ui.components.PinGridSkeleton
import ir.pinbom.app.ui.components.rememberIsOnline
import ir.pinbom.app.ui.nav.Routes
import ir.pinbom.app.ui.nav.sharedPinBounds
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.work.UploadEvents

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · خانه
 *  Brand header, three feed chips, and a masonry wall of pins. The wall never
 *  flashes white: the cached Room rows paint first, the skeleton covers the very
 *  first load, and the offline banner explains the rest.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun HomeScreen(
    nav: NavHostController,
    session: SessionManager,
    vm: HomeViewModel = hiltViewModel(),
) {
    val feed = vm.items.collectAsLazyPagingItems()
    val cached by vm.cached.collectAsStateWithLifecycle(initialValue = emptyList())
    val kind by vm.selectedKind.collectAsStateWithLifecycle()
    val online = rememberIsOnline()

    /* A finished upload means a new pin exists: pull the wall once, the next
     * time the feed sees it. The nonce makes sure that happens exactly once. */
    val uploaded by UploadEvents.completed.collectAsStateWithLifecycle()
    var appliedUpload by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(uploaded) {
        if (uploaded > appliedUpload) {
            appliedUpload = uploaded
            feed.refresh()
        }
    }

    Column(Modifier.fillMaxSize()) {
        HomeHeader(onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } })

        FeedChips(selected = kind, onSelect = vm::setKind)

        OfflineBanner(
            visible = !online,
            onRetry = { feed.refresh() },
        )

        val refreshing = feed.loadState.refresh is LoadState.Loading && feed.itemCount > 0
        PinBomPullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { feed.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val columns = pinGridColumns(maxWidth)
                val cachedPins = remember(cached) { cached.map { it.toPin() } }

                val loading = feed.loadState.refresh is LoadState.Loading
                val failed = feed.loadState.refresh is LoadState.Error

                when {
                    /* Cached rows win over the skeleton: reopening the app offline
                     * shows the wall you already have, not a grey placeholder. */
                    feed.itemCount == 0 && cachedPins.isNotEmpty() && (loading || failed) ->
                        LazyVerticalStaggeredGrid(
                            columns = columns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = Dimens.grid, end = Dimens.grid, top = Dimens.grid, bottom = Dimens.grid + Dimens.navBarSpace),
                            verticalItemSpacing = Dimens.gridGap,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.gridGap),
                        ) {
                            staticPins(
                                pins = cachedPins,
                                baseUrl = session.baseUrl,
                                onPinClick = { nav.navigate(Routes.pin(it.id)) },
                            )
                        }

                    feed.itemCount == 0 && loading ->
                        PinGridSkeleton(
                            columns = columns,
                            itemCount = 6,
                            contentPadding = PaddingValues(start = Dimens.grid, end = Dimens.grid, top = Dimens.grid, bottom = Dimens.grid + Dimens.navBarSpace),
                            modifier = Modifier.fillMaxSize(),
                        )

                    feed.itemCount == 0 && failed ->
                        PullableCenter {
                            ErrorState(onRetry = { feed.retry() })
                        }

                    feed.itemCount == 0 ->
                        PullableCenter {
                            EmptyState(
                                title = stringResource(R.string.state_empty_title),
                                description = stringResource(R.string.state_empty_body),
                            )
                        }

                    else -> LazyVerticalStaggeredGrid(
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = Dimens.grid, end = Dimens.grid, top = Dimens.grid, bottom = Dimens.grid + Dimens.navBarSpace),
                        verticalItemSpacing = Dimens.gridGap,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.gridGap),
                    ) {
                        pagedPins(
                            feed = feed,
                            baseUrl = session.baseUrl,
                            appending = feed.loadState.append is LoadState.Loading,
                            onPinClick = { nav.navigate(Routes.pin(it.id)) },
                        )
                    }
                }
            }
        }
    }
}

/* ── Header ───────────────────────────────────────────────────────────────── */

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.topBar)
            .padding(horizontal = Dimens.screen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /* 22sp wordmark → the mark lands at its cap-height + 30% (28dp). */
        PinBomBrand(markSize = 28.dp)
        Spacer(Modifier.weight(1f))
        IconCircleButton(
            icon = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.cd_open_settings),
            onClick = onSettings,
            containerColor = PinBomTheme.colors.surfaceGrey,
        )
    }
}

/* ── Feed chips ───────────────────────────────────────────────────────────── */

@Composable
private fun FeedChips(selected: String, onSelect: (String) -> Unit) {
    val keys = listOf("home", "following", "trending")
    val labels = listOf(
        stringResource(R.string.feed_for_you),
        stringResource(R.string.feed_following),
        stringResource(R.string.feed_trending),
    )

    PinBomChipRow(
        labels = labels,
        selectedIndex = keys.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> keys.getOrNull(index)?.let(onSelect) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Dimens.screen,
            end = Dimens.screen,
            top = Dimens.xs,
            bottom = Dimens.sm,
        ),
    )
}

/* ── Shared grid plumbing (SearchScreen uses these too) ───────────────────── */

/** 2 → 3 → 4 → 5 columns, the same breakpoints the whole app uses. */
internal fun pinGridColumns(width: Dp): StaggeredGridCells = when {
    width < 600.dp -> StaggeredGridCells.Fixed(2)
    width < 900.dp -> StaggeredGridCells.Fixed(3)
    width < 1200.dp -> StaggeredGridCells.Fixed(4)
    else -> StaggeredGridCells.Fixed(5)
}

/** Paged pins + the full-line footer spinner. */
internal fun LazyStaggeredGridScope.pagedPins(
    feed: LazyPagingItems<Pin>,
    baseUrl: String,
    appending: Boolean,
    onPinClick: (Pin) -> Unit,
) {
    items(
        count = feed.itemCount,
        key = feed.itemKey { it.id },
        contentType = feed.itemContentType { "pin" },
    ) { index ->
        feed[index]?.let { pin ->
            PinQuickCard(pin = pin, baseUrl = baseUrl, onOpen = onPinClick)
        }
    }
    if (appending) {
        item(span = StaggeredGridItemSpan.FullLine) {
            AppendSpinner()
        }
    }
}

/** Offline fallback straight from Room — no spinner, no blank wall. */
internal fun LazyStaggeredGridScope.staticPins(
    pins: List<Pin>,
    baseUrl: String,
    onPinClick: (Pin) -> Unit,
) {
    items(items = pins, key = { it.id }, contentType = { "pin" }) { pin ->
        PinQuickCard(pin = pin, baseUrl = baseUrl, onOpen = onPinClick)
    }
}

/**
 * The pin card every wall uses. Long-pressing a card opens the quick-action
 * bubble: «ذخیره» opens the pin — the board sheet lives there, so the flow stays
 * in one place — and «اشتراک‌گذاری» hands the platform share sheet the public
 * link. Wiring it here keeps Home, Search and the detail wall identical.
 */
@Composable
internal fun PinQuickCard(pin: Pin, baseUrl: String, onOpen: (Pin) -> Unit) {
    val context = LocalContext.current
    /* Share-sheet text follows the *app* language, so it comes from the
     * localized Resources PinBomTheme provides — never from the activity context,
     * whose resources may still be in the system language. */
    val resources = LocalResources.current
    PinCard(
        pin = pin,
        baseUrl = baseUrl,
        modifier = Modifier.sharedPinBounds(pin.id),
        onSaveClick = { onOpen(pin) },
        onShareClick = { sharePinLink(context, resources, pin) },
        onClick = { onOpen(pin) },
    )
}

/** Public pin link share, used by the cards here and by the detail screen. */
internal fun sharePinLink(context: Context, resources: Resources, pin: Pin) {
    shareLink(context, resources, pin.title, resources.getString(R.string.pin_link, pin.id.toString()))
}

/** The platform share sheet, for any PinBom link (pins, boards). */
internal fun shareLink(context: Context, resources: Resources, subject: String, url: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, "$url\n$subject")
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, resources.getString(R.string.action_share)))
    }
}

/** Centred state view that still answers the pull gesture. */
@Composable
internal fun PullableCenter(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Dimens.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

/** Room row → the API model the cards understand. */
internal fun PinEntity.toPin(): Pin = Pin(
    id = id,
    title = title,
    description = description,
    width = width,
    height = height,
    created_at = createdAt,
    image_url = imageUrl,
    author = Author(username = "", display_name = authorName),
)
