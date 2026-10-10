package ir.pinbom.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.navigation.NavHostController
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Board
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.data.remote.UserProfile
import ir.pinbom.app.ui.BoardsViewModel
import ir.pinbom.app.ui.ProfileViewModel
import ir.pinbom.app.ui.components.ActionToastHost
import ir.pinbom.app.ui.components.Avatar
import ir.pinbom.app.ui.components.BoardCreateRow
import ir.pinbom.app.ui.components.BoardRow
import ir.pinbom.app.ui.components.PinBomChipRow
import ir.pinbom.app.ui.components.PinBomDialog
import ir.pinbom.app.ui.components.PinBomPullToRefresh
import ir.pinbom.app.ui.components.PinBomTextField
import ir.pinbom.app.ui.components.ContentText
import ir.pinbom.app.ui.components.EmptyState
import ir.pinbom.app.ui.components.ErrorState
import ir.pinbom.app.ui.components.Hairline
import ir.pinbom.app.ui.components.imageUrl
import ir.pinbom.app.ui.components.PillButtonSize
import ir.pinbom.app.ui.components.PinGridSkeleton
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.components.SecondaryPillButton
import ir.pinbom.app.ui.components.SkeletonLine
import ir.pinbom.app.ui.components.rememberPinBomToastState
import ir.pinbom.app.ui.nav.Routes
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Localizer
import ir.pinbom.app.ui.theme.skeleton
import ir.pinbom.app.work.UploadEvents

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · پروفایل
 *  One wall again: a centred identity block, the three chips, then either the
 *  pin wall, the board list or the saved pins. Owners get «تنظیمات» and can
 *  create a board right here; guests get the follow button.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ProfileScreen(
    nav: NavHostController,
    session: SessionManager,
    username: String = session.username,
    vm: ProfileViewModel = hiltViewModel(),
    boardsVm: BoardsViewModel = hiltViewModel(),
) {
    val target = username.ifBlank { session.username }
    LaunchedEffect(target) { vm.load(target) }

    val profile by vm.user.collectAsStateWithLifecycle()
    val pins by vm.pins.collectAsStateWithLifecycle()
    val boards by vm.boards.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val mediaBusy by vm.mediaBusy.collectAsStateWithLifecycle()
    val mediaAction by vm.mediaAction.collectAsStateWithLifecycle()
    val mediaMessage by vm.mediaMessage.collectAsStateWithLifecycle()
    val lastAction by boardsVm.lastAction.collectAsStateWithLifecycle()

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadProfileMedia(uri, "avatar", target)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadProfileMedia(uri, "cover", target)
    }

    val toast = rememberPinBomToastState()
    var tab by rememberSaveable(target) { mutableStateOf(0) }
    var newBoardOpen by remember { mutableStateOf(false) }
    var pulling by remember { mutableStateOf(false) }
    var newBoardTitle by remember { mutableStateOf("") }
    var newBoardDesc by remember { mutableStateOf("") }

    /* Own profile: the tab route, or the API says so. */
    val isMe = target == session.username || profile?.me == true

    val pinsLabel = stringResource(R.string.profile_tab_pins)
    val boardsLabel = stringResource(R.string.profile_tab_boards)
    val savedLabel = stringResource(R.string.profile_tab_saved)
    val labels = if (isMe) listOf(pinsLabel, boardsLabel, savedLabel) else listOf(pinsLabel, boardsLabel)
    val tabIndex = tab.coerceIn(0, labels.lastIndex)

    val createdLabel = stringResource(R.string.board_created)
    val updatedLabel = stringResource(R.string.board_updated)
    val deletedLabel = stringResource(R.string.board_deleted)
    val errorLabel = stringResource(R.string.state_error_title)
    val mediaSavedLabel = stringResource(R.string.profile_media_saved)
    val mediaFailedLabel = stringResource(R.string.profile_media_failed)

    LaunchedEffect(mediaAction) {
        when (mediaAction.substringBefore(':')) {
            "saved" -> toast.show(mediaSavedLabel)
            "failed" -> toast.show(mediaMessage.ifBlank { mediaFailedLabel })
        }
    }

    /* The pull spinner belongs to a pull, not to the very first load. */
    LaunchedEffect(loading) { if (!loading) pulling = false }

    /* A finished upload lands in the owner's own wall: reload it once, the next
     * time this screen is on. */
    val uploaded by UploadEvents.completed.collectAsStateWithLifecycle()
    var appliedUpload by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(uploaded) {
        if (uploaded > appliedUpload) {
            appliedUpload = uploaded
            if (target == session.username) {
                pulling = true
                vm.load(target)
            }
        }
    }

    LaunchedEffect(lastAction) {
        when (lastAction.substringBefore(':')) {
            "created" -> toast.show(createdLabel)
            "updated" -> toast.show(updatedLabel)
            "deleted" -> toast.show(deletedLabel)
            "failed" -> toast.show(errorLabel)
        }
    }

    Box(Modifier.fillMaxSize()) {
        val current = profile
        when {
            current == null && failed && !loading -> ErrorState(
                onRetry = { vm.load(target) },
                modifier = Modifier.align(Alignment.Center),
            )

            current == null -> ProfileSkeleton()

            else -> PinBomPullToRefresh(
                isRefreshing = pulling && loading,
                onRefresh = {
                    pulling = true
                    vm.load(target)
                },
            ) {
                ProfileWall(
                    profile = current,
                    isMe = isMe,
                    tabIndex = tabIndex,
                    pins = pins,
                    boards = boards,
                    saved = saved,
                    baseUrl = session.baseUrl,
                    onSelectTab = { tab = it },
                    onFollow = { vm.follow() },
                    onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    mediaBusy = mediaBusy,
                    onChangeAvatar = { avatarPicker.launch("image/*") },
                    onChangeCover = { coverPicker.launch("image/*") },
                    onOpenPin = { pin -> nav.navigate(Routes.pin(pin.id)) },
                    onOpenBoard = { board -> nav.navigate(Routes.board(board.id)) },
                    onCreateBoard = { newBoardOpen = true },
                )
            }
        }

        ActionToastHost(
            state = toast,
            modifier = Modifier.align(Alignment.BottomCenter),
            baseUrl = session.baseUrl,
        )
    }

    if (newBoardOpen) {
        PinBomDialog(onDismissRequest = { newBoardOpen = false }) {
            Text(
                text = stringResource(R.string.pin_new_board),
                style = PinBomText.title,
                color = PinBomTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(Dimens.md))
            PinBomTextField(
                value = newBoardTitle,
                onValueChange = { newBoardTitle = it },
                placeholder = stringResource(R.string.pin_board_name),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.sm))
            PinBomTextField(
                value = newBoardDesc,
                onValueChange = { newBoardDesc = it },
                placeholder = stringResource(R.string.board_description_hint),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm, Alignment.End),
            ) {
                SecondaryPillButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { newBoardOpen = false },
                    size = PillButtonSize.Small,
                )
                PrimaryPillButton(
                    text = stringResource(R.string.action_done),
                    enabled = newBoardTitle.isNotBlank(),
                    onClick = {
                        boardsVm.create(newBoardTitle.trim(), newBoardDesc.trim())
                        newBoardTitle = ""
                        newBoardDesc = ""
                        newBoardOpen = false
                        vm.load(target)
                    },
                    size = PillButtonSize.Small,
                )
            }
        }
    }
}

/* ── The wall ─────────────────────────────────────────────────────────────── */

@Composable
private fun ProfileWall(
    profile: UserProfile,
    isMe: Boolean,
    tabIndex: Int,
    pins: List<Pin>,
    boards: List<Board>,
    saved: List<Pin>,
    baseUrl: String,
    onSelectTab: (Int) -> Unit,
    onFollow: () -> Unit,
    onSettings: () -> Unit,
    mediaBusy: Boolean,
    onChangeAvatar: () -> Unit,
    onChangeCover: () -> Unit,
    onOpenPin: (Pin) -> Unit,
    onOpenBoard: (Board) -> Unit,
    onCreateBoard: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = pinGridColumns(maxWidth)

        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.grid,
                end = Dimens.grid,
                bottom = Dimens.huge + Dimens.navBarSpace,
            ),
            verticalItemSpacing = Dimens.gridGap,
            horizontalArrangement = Arrangement.spacedBy(Dimens.gridGap),
        ) {
            item(span = StaggeredGridItemSpan.FullLine, key = "profile-identity") {
                ProfileHeader(
                    profile = profile,
                    isMe = isMe,
                    baseUrl = baseUrl,
                    onFollow = onFollow,
                    onSettings = onSettings,
                    mediaBusy = mediaBusy,
                    onChangeAvatar = onChangeAvatar,
                    onChangeCover = onChangeCover,
                )
            }

            item(span = StaggeredGridItemSpan.FullLine, key = "profile-tabs") {
                PinBomChipRow(
                    labels = if (isMe) {
                        listOf(
                            stringResource(R.string.profile_tab_pins),
                            stringResource(R.string.profile_tab_boards),
                            stringResource(R.string.profile_tab_saved),
                        )
                    } else {
                        listOf(
                            stringResource(R.string.profile_tab_pins),
                            stringResource(R.string.profile_tab_boards),
                        )
                    },
                    selectedIndex = tabIndex,
                    onSelect = onSelectTab,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = Dimens.screen,
                        end = Dimens.screen,
                        top = Dimens.xs,
                        bottom = Dimens.sm,
                    ),
                )
            }

            if (tabIndex == 1) {
                if (isMe) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "profile-new-board") {
                        BoardCreateRow(
                            title = stringResource(R.string.pin_new_board),
                            onClick = onCreateBoard,
                        )
                    }
                }
                if (boards.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "profile-boards-empty") {
                        ProfileEmpty()
                    }
                }
                items(
                    items = boards,
                    key = { "board-${it.id}" },
                    contentType = { "board" },
                    span = { StaggeredGridItemSpan.FullLine },
                ) { board ->
                    BoardRow(
                        title = board.title,
                        onClick = { onOpenBoard(board) },
                        covers = listOf(board.cover_url).filter { it.isNotBlank() },
                        seed = board.id,
                        baseUrl = baseUrl,
                        isPrivate = board.private_board,
                    )
                }
            } else {
                val wall = if (tabIndex == 0) pins else saved
                if (wall.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "profile-wall-empty") {
                        ProfileEmpty()
                    }
                }
                items(items = wall, key = { it.id }, contentType = { "pin" }) { pin ->
                    PinQuickCard(pin = pin, baseUrl = baseUrl, onOpen = onOpenPin)
                }
            }
        }
    }
}

/* ── Identity block ───────────────────────────────────────────────────────── */

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isMe: Boolean,
    baseUrl: String,
    onFollow: () -> Unit,
    onSettings: () -> Unit,
    mediaBusy: Boolean,
    onChangeAvatar: () -> Unit,
    onChangeCover: () -> Unit,
) {
    val colors = PinBomTheme.colors
    val name = profile.display_name.ifBlank { profile.username }
    val context = LocalContext.current
    val coverRequest = remember(profile.cover_url, baseUrl, context) {
        imageUrl(baseUrl, profile.cover_url).takeIf { it.isNotBlank() }?.let { resolved ->
            ImageRequest.Builder(context).data(resolved).crossfade(180).build()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(202.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceGrey),
            ) {
                if (coverRequest != null) {
                    AsyncImage(
                        model = coverRequest,
                        contentDescription = stringResource(R.string.cd_profile_cover),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (isMe) {
                    ProfileMediaAction(
                        busy = mediaBusy,
                        description = stringResource(R.string.profile_change_cover),
                        onClick = onChangeCover,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Dimens.md),
            ) {
                Avatar(
                    name = name,
                    url = profile.avatar_url,
                    baseUrl = baseUrl,
                    size = 92.dp,
                    ring = colors.background,
                    ringWidth = 4.dp,
                )
                if (isMe) {
                    ProfileMediaAction(
                        busy = mediaBusy,
                        description = stringResource(R.string.profile_change_avatar),
                        onClick = onChangeAvatar,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        }
        Spacer(Modifier.height(Dimens.sm))
        Text(
            text = name,
            style = PinBomText.titleLarge,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "@${profile.username}",
            style = PinBomText.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(Dimens.sm))
            ContentText(
                text = profile.bio,
                style = PinBomText.body,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Dimens.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileStat(
                value = profile.pins,
                label = stringResource(R.string.profile_stat_pins),
                modifier = Modifier.weight(1f),
            )
            ProfileStat(
                value = profile.followers,
                label = stringResource(R.string.profile_stat_followers),
                modifier = Modifier.weight(1f),
            )
            ProfileStat(
                value = profile.following,
                label = stringResource(R.string.profile_stat_following),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Dimens.lg))
        when {
            isMe -> SecondaryPillButton(
                text = stringResource(R.string.profile_edit_profile),
                onClick = onSettings,
                leadingIcon = Icons.Outlined.Settings,
                fillWidth = true,
            )
            profile.following_me -> SecondaryPillButton(
                text = stringResource(R.string.action_following),
                onClick = onFollow,
                selected = true,
            )
            else -> PrimaryPillButton(
                text = stringResource(R.string.action_follow),
                onClick = onFollow,
            )
        }
        Spacer(Modifier.height(Dimens.lg))
        Hairline(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProfileMediaAction(
    busy: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PinBomTheme.colors
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = colors.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        IconButton(
            onClick = onClick,
            enabled = !busy,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color = colors.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = description,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(value: Int, label: String, modifier: Modifier = Modifier) {
    val colors = PinBomTheme.colors
    Column(
        modifier = modifier.padding(horizontal = Dimens.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = Localizer.number(value),
            style = PinBomText.title,
            color = colors.textPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(Dimens.xxs))
        Text(
            text = label,
            style = PinBomText.caption,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfileEmpty() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xxxl),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            title = stringResource(R.string.state_empty_title),
            description = stringResource(R.string.state_empty_body),
            compact = true,
        )
    }
}

/* ── Loading ──────────────────────────────────────────────────────────────── */

@Composable
private fun ProfileSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screen, vertical = Dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .skeleton(shape = PinBomShape.pill),
            )
            Spacer(Modifier.height(Dimens.lg))
            SkeletonLine(widthFraction = 0.42f, height = 16.dp)
            Spacer(Modifier.height(Dimens.sm))
            SkeletonLine(widthFraction = 0.26f, height = 10.dp)
            Spacer(Modifier.height(Dimens.xl))
            SkeletonLine(widthFraction = 0.54f, height = 10.dp)
        }
        PinGridSkeleton(
            columns = StaggeredGridCells.Fixed(2),
            itemCount = 6,
            modifier = Modifier.weight(1f),
        )
    }
}
