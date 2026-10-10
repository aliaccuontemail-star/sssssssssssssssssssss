package ir.pinbom.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.data.remote.SearchBoard
import ir.pinbom.app.data.remote.SearchUser
import ir.pinbom.app.ui.SearchViewModel
import ir.pinbom.app.ui.components.Avatar
import ir.pinbom.app.ui.components.BoardRow
import ir.pinbom.app.ui.components.PinBomChipRow
import ir.pinbom.app.ui.components.PinBomPullToRefresh
import ir.pinbom.app.ui.components.PinBomSearchBar
import ir.pinbom.app.ui.components.EmptyState
import ir.pinbom.app.ui.components.ErrorState
import ir.pinbom.app.ui.components.ListRowSkeleton
import ir.pinbom.app.ui.components.OfflineBanner
import ir.pinbom.app.ui.components.PinGridSkeleton
import ir.pinbom.app.ui.components.SectionHeader
import ir.pinbom.app.ui.components.rememberIsOnline
import ir.pinbom.app.ui.components.ContentText
import ir.pinbom.app.ui.nav.Routes
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.pinbomClick
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · جستجو
 *  One field, three result kinds. While the field is empty the screen offers the
 *  queries you already ran (in-memory, this session) instead of a blank page,
 *  and the results grid is the same masonry wall as Home.
 * ──────────────────────────────────────────────────────────────────────────── */

private const val TYPE_PINS = "pins"
private const val TYPE_BOARDS = "boards"
private const val TYPE_USERS = "users"

@Composable
fun SearchScreen(
    nav: NavHostController,
    session: SessionManager,
    vm: SearchViewModel = hiltViewModel(),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val pins = vm.results.collectAsLazyPagingItems()
    val users by vm.users.collectAsStateWithLifecycle()
    val boards by vm.boards.collectAsStateWithLifecycle()
    val busy by vm.secondaryBusy.collectAsStateWithLifecycle()
    val online = rememberIsOnline()

    val recents = remember { mutableStateListOf<String>() }
    val requester = remember { FocusRequester() }

    LaunchedEffect(query, type) {
        if (type != TYPE_PINS) vm.searchSecondary(query, type)
    }

    fun rememberQuery() {
        val text = query.trim()
        if (text.isEmpty()) return
        recents.remove(text)
        recents.add(0, text)
        while (recents.size > 6) recents.removeAt(recents.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        PinBomSearchBar(
            value = query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screen, vertical = Dimens.sm),
            placeholder = stringResource(R.string.search_placeholder),
            onSearch = ::rememberQuery,
            onClear = { vm.setQuery("") },
            requester = requester,
        )

        ResultChips(selected = type, onSelect = vm::setType)

        OfflineBanner(
            visible = !online,
            onRetry = { if (type == TYPE_PINS) pins.refresh() else vm.searchSecondary(query, type) },
        )

        val refreshing = when (type) {
            TYPE_PINS -> pins.loadState.refresh is LoadState.Loading && pins.itemCount > 0
            else -> busy && (users.isNotEmpty() || boards.isNotEmpty())
        }

        PinBomPullToRefresh(
            isRefreshing = refreshing,
            onRefresh = { if (type == TYPE_PINS) pins.refresh() else vm.searchSecondary(query, type) },
            modifier = Modifier.weight(1f),
        ) {
            when {
                query.isBlank() -> IdleContent(
                    recents = recents,
                    onRecentClick = { recent -> vm.setQuery(recent) },
                )

                type == TYPE_PINS -> PinResults(
                    pins = pins,
                    baseUrl = session.baseUrl,
                    onPinClick = { nav.navigate(Routes.pin(it.id)) },
                )

                type == TYPE_USERS -> UserResults(
                    users = users,
                    loading = busy,
                    baseUrl = session.baseUrl,
                    onUserClick = { nav.navigate(Routes.profile(it.username)) },
                    onRetry = { vm.searchSecondary(query, type) },
                )

                else -> BoardResults(
                    boards = boards,
                    loading = busy,
                    baseUrl = session.baseUrl,
                    onBoardClick = { nav.navigate(Routes.board(it.id)) },
                    onRetry = { vm.searchSecondary(query, type) },
                )
            }
        }
    }
}

/* ── Result kinds ─────────────────────────────────────────────────────────── */

@Composable
private fun ResultChips(selected: String, onSelect: (String) -> Unit) {
    val keys = listOf(TYPE_PINS, TYPE_BOARDS, TYPE_USERS)
    val labels = listOf(
        stringResource(R.string.search_tab_pins),
        stringResource(R.string.search_tab_boards),
        stringResource(R.string.search_tab_users),
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

@Composable
private fun IdleContent(
    recents: List<String>,
    onRecentClick: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (recents.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.search_recent_title))
            PinBomChipRow(
                labels = recents,
                selectedIndex = -1,
                onSelect = { index -> recents.getOrNull(index)?.let(onRecentClick) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Dimens.screen,
                    end = Dimens.screen,
                    top = Dimens.xs,
                    bottom = Dimens.sm,
                ),
            )
        }
        PullableCenter {
            EmptyState(
                title = stringResource(R.string.search_idle_title),
                description = stringResource(R.string.search_idle_body),
                icon = Icons.Outlined.Search,
            )
        }
    }
}

@Composable
private fun PinResults(
    pins: LazyPagingItems<Pin>,
    baseUrl: String,
    onPinClick: (Pin) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = pinGridColumns(maxWidth)

        when {
            pins.loadState.refresh is LoadState.Loading && pins.itemCount == 0 ->
                PinGridSkeleton(
                    columns = columns,
                    itemCount = 6,
                    modifier = Modifier.fillMaxSize(),
                )

            pins.loadState.refresh is LoadState.Error && pins.itemCount == 0 ->
                PullableCenter {
                    ErrorState(onRetry = { pins.retry() })
                }

            pins.itemCount == 0 ->
                PullableCenter {
                    EmptyState(
                        title = stringResource(R.string.search_no_results_title),
                        description = stringResource(R.string.search_no_results_body),
                        icon = Icons.Outlined.Search,
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
                    feed = pins,
                    baseUrl = baseUrl,
                    appending = pins.loadState.append is LoadState.Loading,
                    onPinClick = onPinClick,
                )
            }
        }
    }
}

@Composable
private fun UserResults(
    users: List<SearchUser>,
    loading: Boolean,
    baseUrl: String,
    onUserClick: (SearchUser) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loading && users.isEmpty() -> SkeletonList(count = 6)
        users.isEmpty() -> PullableCenter {
            NoResults(
                onRetry = onRetry,
                title = stringResource(R.string.search_no_results_title),
                description = stringResource(R.string.search_no_results_body),
            )
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.navBarSpace),
        ) {
            items(items = users, key = { it.id }, contentType = { "user" }) { user ->
                UserRow(
                    user = user,
                    baseUrl = baseUrl,
                    onClick = { onUserClick(user) },
                )
            }
        }
    }
}

@Composable
private fun BoardResults(
    boards: List<SearchBoard>,
    loading: Boolean,
    baseUrl: String,
    onBoardClick: (SearchBoard) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loading && boards.isEmpty() -> SkeletonList(count = 6)
        boards.isEmpty() -> PullableCenter {
            NoResults(
                onRetry = onRetry,
                title = stringResource(R.string.search_no_results_title),
                description = stringResource(R.string.search_no_results_body),
            )
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Dimens.navBarSpace),
        ) {
            items(items = boards, key = { it.id }, contentType = { "board" }) { board ->
                BoardRow(
                    title = board.title,
                    onClick = { onBoardClick(board) },
                    modifier = Modifier.padding(horizontal = Dimens.screen, vertical = Dimens.xs),
                    covers = listOf(board.cover_url).filter { it.isNotBlank() },
                    seed = board.id,
                    baseUrl = baseUrl,
                    isPrivate = board.private_board,
                )
            }
        }
    }
}

/* ── Rows ─────────────────────────────────────────────────────────────────── */

@Composable
private fun UserRow(user: SearchUser, baseUrl: String, onClick: () -> Unit) {
    val colors = PinBomTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = Dimens.screen)
            .pinbomClick(
                onClick = onClick,
                pressedScale = 0.99f,
                indication = ripple(bounded = true, color = colors.textSecondary),
                role = Role.Button,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            name = user.display_name.ifBlank { user.username },
            url = user.avatar_url.ifBlank { null },
            baseUrl = baseUrl,
            size = 44.dp,
        )
        Spacer(Modifier.width(Dimens.md))
        Column(Modifier.weight(1f)) {
            ContentText(
                text = user.display_name.ifBlank { user.username },
                style = PinBomText.title,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ContentText(
                text = "@${user.username}",
                style = PinBomText.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronLeft,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SkeletonList(count: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinBomTheme.colors.background),
    ) {
        repeat(count) {
            ListRowSkeleton(
                modifier = Modifier.padding(horizontal = Dimens.screen, vertical = Dimens.xs),
            )
        }
    }
}

/** Boards and people share one "nothing found" treatment, with a retry path. */
@Composable
private fun NoResults(onRetry: () -> Unit, title: String, description: String) {
    EmptyState(
        title = title,
        description = description,
        icon = Icons.Outlined.Search,
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
    )
}
