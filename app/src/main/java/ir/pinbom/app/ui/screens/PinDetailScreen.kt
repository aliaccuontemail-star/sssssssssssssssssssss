package ir.pinbom.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Board
import ir.pinbom.app.data.remote.Comment
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.ui.BoardsViewModel
import ir.pinbom.app.ui.PinViewModel
import ir.pinbom.app.ui.components.ActionToastHost
import ir.pinbom.app.ui.components.Avatar
import ir.pinbom.app.ui.components.BoardCreateRow
import ir.pinbom.app.ui.components.BoardRow
import ir.pinbom.app.ui.components.PinBomBottomSheet
import ir.pinbom.app.ui.components.PinBomDialog
import ir.pinbom.app.ui.components.PinBomTextField
import ir.pinbom.app.ui.components.CountText
import ir.pinbom.app.ui.components.ErrorState
import ir.pinbom.app.ui.components.HeartBurst
import ir.pinbom.app.ui.components.IconCircleButton
import ir.pinbom.app.ui.components.PillButtonSize
import ir.pinbom.app.ui.components.PinCard
import ir.pinbom.app.ui.components.PinImage
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.components.RelativeTimeText
import ir.pinbom.app.ui.components.SecondaryPillButton
import ir.pinbom.app.ui.components.SectionHeader
import ir.pinbom.app.ui.components.SheetHeader
import ir.pinbom.app.ui.components.SkeletonLine
import ir.pinbom.app.ui.components.imageUrl
import ir.pinbom.app.ui.components.pinAspectRatio
import ir.pinbom.app.ui.components.rememberPinBomToastState
import ir.pinbom.app.ui.components.ContentText
import ir.pinbom.app.ui.components.ContentTextField
import ir.pinbom.app.ui.nav.Routes
import ir.pinbom.app.ui.nav.sharedPinBounds
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.pinbomClick
import ir.pinbom.app.ui.theme.reduceMotion
import ir.pinbom.app.ui.theme.skeleton

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · صفحهٔ پین
 *  One masonry wall: the pin owns the top as a rounded hero, then the author,
 *  the title, the action bar, the comments and finally the same wall of related
 *  pins the feed uses. Double-tap the hero for the heart burst, tap it for the
 *  fullscreen viewer, and save through the sheet — the toast offers «تغییر».
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun PinDetailScreen(
    nav: NavHostController,
    id: Long,
    session: SessionManager,
    vm: PinViewModel = hiltViewModel(),
    boardsVm: BoardsViewModel = hiltViewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }

    val pin by vm.pin.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    val related by vm.related.collectAsStateWithLifecycle()
    val boards by boardsVm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()

    val context = LocalContext.current
    /* Localized resources (the app language), for text that leaves Compose. */
    val resources = LocalResources.current
    val toast = rememberPinBomToastState()
    val reduce = reduceMotion()

    var saveSheet by remember { mutableStateOf(false) }
    var newBoardDialog by remember { mutableStateOf(false) }
    var newBoardTitle by remember { mutableStateOf("") }
    var viewerOpen by remember { mutableStateOf(false) }
    var likeTrigger by remember { mutableStateOf(0) }
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(saveSheet) { if (saveSheet) boardsVm.load() }

    /* Hoisted labels: `stringResource` cannot be called from a click lambda. */
    val shareLabel = stringResource(R.string.action_share)
    val linkLabel = stringResource(R.string.cd_copy_link)
    val saveLabel = stringResource(R.string.action_save)
    val savedLabel = stringResource(R.string.action_saved)
    val changeLabel = stringResource(R.string.action_change)
    val copiedLabel = stringResource(R.string.action_copied)
    val linkPattern = stringResource(R.string.pin_link)

    val item = pin

    /* Hero: a 6% push plus a fade, so the pin lands instead of blinking. The
     * API-level shared-element hand-off (SharedTransitionLayout) is scheduled
     * for the polish phase — see PHASE-6.md. */
    val hero = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(item?.id) {
        if (item != null) {
            if (reduce) {
                hero.snapTo(1f)
            } else {
                hero.snapTo(0f)
                hero.animateTo(1f, tween(durationMillis = Motion.long, easing = Motion.emphasized))
            }
        }
    }

    fun linkFor(pinId: Long): String = String.format(linkPattern, pinId.toString())

    fun fireToast(message: String) = toast.show(
        message = message,
        coverUrl = item?.image_url,
        coverSeed = item?.id ?: 0L,
        actionLabel = changeLabel,
        onAction = { saveSheet = true },
    )

    fun onShare() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, item?.title.orEmpty())
            putExtra(Intent.EXTRA_TEXT, linkFor(id) + "\n" + item?.title.orEmpty())
        }
        runCatching { context.startActivity(Intent.createChooser(send, shareLabel)) }
    }

    fun onCopyLink() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("pinbom", linkFor(id)))
        toast.show(message = copiedLabel)
    }

    Box(Modifier.fillMaxSize()) {
        when {
            item != null -> PinWall(
                pin = item,
                comments = comments,
                related = related,
                baseUrl = session.baseUrl,
                hero = { hero.value },
                likeTrigger = likeTrigger,
                draft = draft,
                onDraftChange = { draft = it },
                onOpenViewer = { viewerOpen = true },
                onDoubleTapHero = {
                    if (!item.liked) vm.like()
                    likeTrigger++
                },
                onAuthorClick = {
                    item.author?.username
                        ?.takeIf { it.isNotBlank() }
                        ?.let { nav.navigate(Routes.profile(it)) }
                },
                onSave = { saveSheet = true },
                onLike = { vm.like() },
                onShare = { onShare() },
                onCopyLink = { onCopyLink() },
                onSendComment = {
                    vm.comment(draft)
                    draft = ""
                },
                onRelatedClick = { relatedPin -> nav.navigate(Routes.pin(relatedPin.id)) },
                shareLabel = shareLabel,
                linkLabel = linkLabel,
                saveLabel = if (item.saved) savedLabel else saveLabel,
            )

            failed && !loading -> ErrorState(
                onRetry = { vm.load(id) },
                modifier = Modifier.align(Alignment.Center),
            )

            else -> PinDetailSkeleton()
        }

        /* Floating chrome — outside the scroll, always reachable. */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.sm, vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircleButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = { nav.popBackStack() },
                containerColor = PinBomTheme.colors.surface,
            )
            Spacer(Modifier.weight(1f))
            IconCircleButton(
                icon = Icons.Outlined.Share,
                contentDescription = shareLabel,
                onClick = { onShare() },
                containerColor = PinBomTheme.colors.surface,
            )
            Spacer(Modifier.width(Dimens.xs))
            IconCircleButton(
                icon = Icons.Outlined.Link,
                contentDescription = linkLabel,
                onClick = { onCopyLink() },
                containerColor = PinBomTheme.colors.surface,
            )
        }

        ActionToastHost(
            state = toast,
            modifier = Modifier.align(Alignment.BottomCenter),
            baseUrl = session.baseUrl,
        )
    }

    if (saveSheet && item != null) {
        SaveSheet(
            boards = boards,
            hint = stringResource(R.string.pin_save_hint),
            baseUrl = session.baseUrl,
            coverUrl = imageUrl(session.baseUrl, item.image_url),
            coverSeed = item.id,
            onDismiss = { saveSheet = false },
            onBoard = { board ->
                vm.save(board.id)
                saveSheet = false
                fireToast(
                    message = resources.getString(R.string.pin_saved_to, board.title),
                )
            },
            onNewBoard = { newBoardDialog = true },
        )
    }

    if (newBoardDialog) {
        PinBomDialog(onDismissRequest = { newBoardDialog = false }) {
            Text(
                text = stringResource(R.string.pin_new_board),
                style = PinBomText.title,
                color = PinBomTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(Dimens.md))
            ContentTextField(
                value = newBoardTitle,
                onValueChange = { newBoardTitle = it },
                placeholder = stringResource(R.string.pin_board_name),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm, Alignment.End),
            ) {
                SecondaryPillButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { newBoardDialog = false },
                    size = PillButtonSize.Small,
                )
                PrimaryPillButton(
                    text = stringResource(R.string.action_done),
                    enabled = newBoardTitle.isNotBlank(),
                    onClick = {
                        boardsVm.create(newBoardTitle, "")
                        newBoardTitle = ""
                        newBoardDialog = false
                    },
                    size = PillButtonSize.Small,
                )
            }
        }
    }

    if (viewerOpen && item != null) {
        ImageViewer(
            pin = item,
            baseUrl = session.baseUrl,
            onDismiss = { viewerOpen = false },
        )
    }
}

/* ── The wall ─────────────────────────────────────────────────────────────── */

@Composable
private fun PinWall(
    pin: Pin,
    comments: List<Comment>,
    related: List<Pin>,
    baseUrl: String,
    hero: () -> Float,
    likeTrigger: Int,
    draft: String,
    onDraftChange: (String) -> Unit,
    onOpenViewer: () -> Unit,
    onDoubleTapHero: () -> Unit,
    onAuthorClick: () -> Unit,
    onSave: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onSendComment: () -> Unit,
    onRelatedClick: (Pin) -> Unit,
    shareLabel: String,
    linkLabel: String,
    saveLabel: String,
) {
    val colors = PinBomTheme.colors
    val context = LocalContext.current
    /* Localized resources (the app language), for text that leaves Compose. */
    val resources = LocalResources.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = StaggeredGridCells.Fixed(
            when {
                maxWidth < 600.dp -> 2
                maxWidth < 900.dp -> 3
                else -> 4
            },
        )

        LazyVerticalStaggeredGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.grid,
                end = Dimens.grid,
                bottom = Dimens.huge,
            ),
            verticalItemSpacing = Dimens.gridGap,
            horizontalArrangement = Arrangement.spacedBy(Dimens.gridGap),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                PinHero(
                    pin = pin,
                    baseUrl = baseUrl,
                    hero = hero,
                    likeTrigger = likeTrigger,
                    onOpen = onOpenViewer,
                    onDoubleTap = onDoubleTapHero,
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                AuthorRow(pin = pin, baseUrl = baseUrl, onClick = onAuthorClick)
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                Column(Modifier.padding(horizontal = Dimens.screen)) {
                    if (pin.title.isNotBlank()) {
                        ContentText(
                            text = pin.title,
                            style = PinBomText.titleLarge,
                            color = colors.textPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(Dimens.xs))
                    }
                    if (pin.description.isNotBlank()) {
                        ContentText(
                            text = pin.description,
                            style = PinBomText.body,
                            color = colors.textSecondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(Dimens.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CountText(
                            count = pin.likes.toLong(),
                            icon = Icons.Filled.Favorite,
                            color = if (pin.liked) colors.primary else colors.textSecondary,
                        )
                        Spacer(Modifier.width(Dimens.md))
                        CountText(
                            count = pin.comments.toLong(),
                            icon = Icons.Outlined.ModeComment,
                            color = colors.textTertiary,
                        )
                        Spacer(Modifier.width(Dimens.md))
                        RelativeTimeText(iso = pin.created_at)
                    }
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                ActionBar(
                    saved = pin.saved,
                    liked = pin.liked,
                    saveLabel = saveLabel,
                    shareLabel = shareLabel,
                    linkLabel = linkLabel,
                    onSave = onSave,
                    onLike = onLike,
                    onShare = onShare,
                    onCopyLink = onCopyLink,
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(title = stringResource(R.string.pin_comments))
            }

            if (comments.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = stringResource(R.string.pin_no_comments),
                        style = PinBomText.bodySmall,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(
                            start = Dimens.screen,
                            end = Dimens.screen,
                            bottom = Dimens.md,
                        ),
                    )
                }
            } else {
                items(
                    count = comments.size,
                    key = { index -> comments[index].id },
                    span = { StaggeredGridItemSpan.FullLine },
                ) { index ->
                    CommentRow(comment = comments[index], baseUrl = baseUrl)
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                CommentComposer(
                    value = draft,
                    onValueChange = onDraftChange,
                    onSend = onSendComment,
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(title = stringResource(R.string.pin_related))
            }

            items(items = related, key = { it.id }, contentType = { "pin" }) { relatedPin ->
                PinCard(
                    pin = relatedPin,
                    baseUrl = baseUrl,
                    onSaveClick = { onRelatedClick(relatedPin) },
                    onShareClick = { sharePinLink(context, resources, relatedPin) },
                    onClick = { onRelatedClick(relatedPin) },
                )
            }
        }
    }
}

/* ── Hero ─────────────────────────────────────────────────────────────────── */

@Composable
private fun PinHero(
    pin: Pin,
    baseUrl: String,
    hero: () -> Float,
    likeTrigger: Int,
    onOpen: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.sm)
            .sharedPinBounds(pin.id)
            /* The entrance value is read *here*, in the layer: reading it in
             * composition would recompose the whole wall on every frame of the
             * 380ms pop-in. */
            .graphicsLayer {
                val progress = hero()
                val scale = 0.94f + 0.06f * progress
                scaleX = scale
                scaleY = scale
                alpha = progress.coerceIn(0f, 1f)
            },
    ) {
        PinImage(
            url = imageUrl(baseUrl, pin.image_url),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    hapticFeedbackEnabled = false,
                    onDoubleClick = onDoubleTap,
                    onClick = onOpen,
                ),
            seed = pin.id,
            contentDescription = stringResource(R.string.cd_pin_image, pin.title),
            shape = PinBomShape.pin,
            aspectRatio = pinAspectRatio(pin),
            contentScale = ContentScale.Crop,
        )

        HeartBurst(
            trigger = likeTrigger,
            color = PinBomTheme.colors.primary,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/* ── Author ───────────────────────────────────────────────────────────────── */

@Composable
private fun AuthorRow(pin: Pin, baseUrl: String, onClick: () -> Unit) {
    val colors = PinBomTheme.colors
    val handle = pin.author?.username.orEmpty()
    val name = pin.author?.display_name?.ifBlank { handle }.orEmpty()
    val fallback = stringResource(R.string.app_name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.sm)
            .pinbomClick(
                onClick = onClick,
                indication = ripple(bounded = true, color = colors.textSecondary),
                role = Role.Button,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            name = name.ifBlank { fallback },
            baseUrl = baseUrl,
            size = 44.dp,
        )
        Spacer(Modifier.width(Dimens.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = name.ifBlank { fallback },
                style = PinBomText.title,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (handle.isNotBlank()) {
                Text(
                    text = "@$handle",
                    style = PinBomText.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SecondaryPillButton(
            text = stringResource(R.string.pin_profile),
            onClick = onClick,
            size = PillButtonSize.Small,
        )
    }
}

/* ── Actions ──────────────────────────────────────────────────────────────── */

@Composable
private fun ActionBar(
    saved: Boolean,
    liked: Boolean,
    saveLabel: String,
    shareLabel: String,
    linkLabel: String,
    onSave: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
) {
    val colors = PinBomTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrimaryPillButton(
            text = saveLabel,
            onClick = onSave,
            leadingIcon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
        )
        Spacer(Modifier.weight(1f))
        IconCircleButton(
            icon = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.cd_like),
            onClick = onLike,
            containerColor = colors.surfaceGrey,
            contentColor = if (liked) colors.primary else colors.textPrimary,
        )
        Spacer(Modifier.width(Dimens.sm))
        IconCircleButton(
            icon = Icons.Outlined.Share,
            contentDescription = shareLabel,
            onClick = onShare,
            containerColor = colors.surfaceGrey,
        )
        Spacer(Modifier.width(Dimens.sm))
        IconCircleButton(
            icon = Icons.Outlined.Link,
            contentDescription = linkLabel,
            onClick = onCopyLink,
            containerColor = colors.surfaceGrey,
        )
    }
}

/* ── Comments ─────────────────────────────────────────────────────────────── */

@Composable
private fun CommentRow(comment: Comment, baseUrl: String) {
    val colors = PinBomTheme.colors
    val author = comment.user.display_name.ifBlank { comment.user.username }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.sm),
    ) {
        Avatar(name = author, baseUrl = baseUrl, size = 36.dp)
        Spacer(Modifier.width(Dimens.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author,
                    style = PinBomText.caption,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(Dimens.sm))
                RelativeTimeText(iso = comment.created_at)
            }
            Spacer(Modifier.height(Dimens.xxs))
            ContentText(
                text = comment.body,
                style = PinBomText.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CommentComposer(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screen, vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.pin_comment_hint),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Dimens.sm))
        IconCircleButton(
            icon = Icons.AutoMirrored.Outlined.Send,
            contentDescription = stringResource(R.string.pin_send),
            onClick = onSend,
            enabled = value.isNotBlank(),
            containerColor = PinBomTheme.colors.surfaceGrey,
        )
    }
}

/* ── Save sheet ───────────────────────────────────────────────────────────── */

@Composable
private fun SaveSheet(
    boards: List<Board>,
    hint: String,
    baseUrl: String,
    coverUrl: String,
    coverSeed: Long,
    onDismiss: () -> Unit,
    onBoard: (Board) -> Unit,
    onNewBoard: () -> Unit,
) {
    PinBomBottomSheet(onDismissRequest = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.pin_save_to_board),
            subtitle = hint.takeIf { it.isNotBlank() },
            trailing = {
                PinImage(
                    url = coverUrl,
                    seed = coverSeed,
                    modifier = Modifier.size(44.dp),
                    shape = PinBomShape.thumb,
                    contentDescription = null,
                )
            },
        )
        if (boards.isEmpty()) {
            Text(
                text = stringResource(R.string.pin_no_boards),
                style = PinBomText.bodySmall,
                color = PinBomTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = Dimens.md),
            )
        }
        boards.forEach { board ->
            BoardRow(
                title = board.title,
                onClick = { onBoard(board) },
                modifier = Modifier.padding(bottom = Dimens.xs),
                covers = listOf(board.cover_url).filter { it.isNotBlank() },
                seed = board.id,
                baseUrl = baseUrl,
                isPrivate = board.private_board,
            )
        }
        Spacer(Modifier.height(Dimens.sm))
        BoardCreateRow(
            title = stringResource(R.string.pin_new_board),
            onClick = onNewBoard,
        )
    }
}

/* ── Fullscreen viewer ────────────────────────────────────────────────────── */

@Composable
private fun ImageViewer(pin: Pin, baseUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PinBomTheme.colors.scrim)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
    ) {
        AsyncImage(
            model = imageUrl(baseUrl, pin.image_url),
            contentDescription = stringResource(R.string.cd_pin_image, pin.title),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentScale = ContentScale.Fit,
        )

        IconCircleButton(
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.cd_close),
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.xl),
            containerColor = PinBomTheme.colors.surface,
        )
    }
}

/* ── Loading ──────────────────────────────────────────────────────────────── */

@Composable
private fun PinDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.screen, vertical = Dimens.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .skeleton(shape = PinBomShape.pin),
        )
        Spacer(Modifier.height(Dimens.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .skeleton(shape = PinBomShape.pill),
            )
            Spacer(Modifier.width(Dimens.md))
            Column {
                SkeletonLine(widthFraction = 0.42f)
                Spacer(Modifier.height(Dimens.xs))
                SkeletonLine(widthFraction = 0.26f, height = 10.dp)
            }
        }
        Spacer(Modifier.height(Dimens.lg))
        SkeletonLine(widthFraction = 0.86f, height = 18.dp)
        Spacer(Modifier.height(Dimens.sm))
        SkeletonLine(widthFraction = 0.64f)
        Spacer(Modifier.height(Dimens.xs))
        SkeletonLine(widthFraction = 0.34f)
    }
}
