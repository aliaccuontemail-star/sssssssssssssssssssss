package ir.pinbom.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Board
import ir.pinbom.app.ui.BoardsViewModel
import ir.pinbom.app.ui.components.ActionToastHost
import ir.pinbom.app.ui.components.BoardCreateRow
import ir.pinbom.app.ui.components.BoardRow
import ir.pinbom.app.ui.components.PinBomBottomSheet
import ir.pinbom.app.ui.components.PinBomDialog
import ir.pinbom.app.ui.components.PinBomDialogContent
import ir.pinbom.app.ui.components.PinBomPullToRefresh
import ir.pinbom.app.ui.components.PinBomTextField
import ir.pinbom.app.ui.components.EmptyState
import ir.pinbom.app.ui.components.IconCircleButton
import ir.pinbom.app.ui.components.ListRowSkeleton
import ir.pinbom.app.ui.components.PillButtonSize
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.components.SecondaryPillButton
import ir.pinbom.app.ui.components.SheetHeader
import ir.pinbom.app.ui.components.rememberPinBomToastState
import ir.pinbom.app.ui.components.ContentTextField
import ir.pinbom.app.ui.nav.Routes
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · بوردهای من
 *  Every row here belongs to the signed-in user, so the row-level «ویرایش» and
 *  «حذف» need no ownership guesswork. Creating happens from the first row; the
 *  destructive action always goes through the dialog.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun BoardsScreen(
    nav: NavHostController,
    session: SessionManager,
    vm: BoardsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }

    val boards by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val lastAction by vm.lastAction.collectAsStateWithLifecycle()

    val toast = rememberPinBomToastState()
    var createOpen by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Board?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf<Board?>(null) }

    val createdLabel = stringResource(R.string.board_created)
    val updatedLabel = stringResource(R.string.board_updated)
    val deletedLabel = stringResource(R.string.board_deleted)
    val errorLabel = stringResource(R.string.state_error_title)

    LaunchedEffect(lastAction) {
        when (lastAction.substringBefore(':')) {
            "created" -> toast.show(createdLabel)
            "updated" -> toast.show(updatedLabel)
            "deleted" -> toast.show(deletedLabel)
            "failed" -> toast.show(errorLabel)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.topBar)
                    .padding(horizontal = Dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = { nav.popBackStack() },
                    containerColor = PinBomTheme.colors.surfaceGrey,
                )
                Spacer(Modifier.width(Dimens.sm))
                Text(
                    text = stringResource(R.string.boards_title),
                    style = PinBomText.titleLarge,
                    color = PinBomTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            PinBomPullToRefresh(
                isRefreshing = loading && boards.isNotEmpty(),
                onRefresh = { vm.load() },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.screen,
                        end = Dimens.screen,
                        top = Dimens.xs,
                        bottom = Dimens.huge,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.xs),
                ) {
                    item(key = "create-board") {
                        BoardCreateRow(
                            title = stringResource(R.string.pin_new_board),
                            onClick = { createOpen = true },
                        )
                    }

                    when {
                        boards.isEmpty() && loading -> items(count = 4, key = { "skeleton-$it" }) {
                            ListRowSkeleton(avatarSize = Dimens.boardThumb)
                        }

                        boards.isEmpty() -> item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.6f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmptyState(
                                    title = stringResource(R.string.state_empty_title),
                                    description = stringResource(R.string.state_empty_body),
                                )
                            }
                        }

                        else -> items(items = boards, key = { it.id }) { board ->
                            BoardRow(
                                title = board.title,
                                onClick = { nav.navigate(Routes.board(board.id)) },
                                covers = listOf(board.cover_url).filter { it.isNotBlank() },
                                seed = board.id,
                                baseUrl = session.baseUrl,
                                isPrivate = board.private_board,
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconCircleButton(
                                            icon = Icons.Outlined.Edit,
                                            contentDescription = stringResource(R.string.action_edit),
                                            onClick = {
                                                editing = board
                                                editTitle = board.title
                                                editDesc = board.description
                                            },
                                            size = Dimens.iconButtonSmall,
                                            iconSize = 16.dp,
                                            containerColor = PinBomTheme.colors.surfaceGrey,
                                        )
                                        Spacer(Modifier.width(Dimens.xs))
                                        IconCircleButton(
                                            icon = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(R.string.action_delete),
                                            onClick = { confirming = board },
                                            size = Dimens.iconButtonSmall,
                                            iconSize = 16.dp,
                                            containerColor = PinBomTheme.colors.surfaceGrey,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        ActionToastHost(
            state = toast,
            modifier = Modifier.align(Alignment.BottomCenter),
            baseUrl = session.baseUrl,
        )
    }

    if (createOpen) {
        PinBomDialog(onDismissRequest = { createOpen = false }) {
            Text(
                text = stringResource(R.string.pin_new_board),
                style = PinBomText.title,
                color = PinBomTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(Dimens.md))
            ContentTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = stringResource(R.string.pin_board_name),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.sm))
            ContentTextField(
                value = newDesc,
                onValueChange = { newDesc = it },
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
                    onClick = { createOpen = false },
                    size = PillButtonSize.Small,
                )
                PrimaryPillButton(
                    text = stringResource(R.string.action_done),
                    enabled = newTitle.isNotBlank(),
                    onClick = {
                        vm.create(newTitle.trim(), newDesc.trim())
                        newTitle = ""
                        newDesc = ""
                        createOpen = false
                    },
                    size = PillButtonSize.Small,
                )
            }
        }
    }

    val current = editing
    if (current != null) {
        PinBomBottomSheet(onDismissRequest = { editing = null }) {
            SheetHeader(title = stringResource(R.string.board_edit_title))
            ContentTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                placeholder = stringResource(R.string.pin_board_name),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.sm))
            ContentTextField(
                value = editDesc,
                onValueChange = { editDesc = it },
                placeholder = stringResource(R.string.board_description_hint),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.xl))
            PrimaryPillButton(
                text = stringResource(R.string.action_save),
                enabled = editTitle.isNotBlank(),
                fillWidth = true,
                onClick = {
                    vm.update(current.id, editTitle.trim(), editDesc.trim())
                    editing = null
                },
            )
        }
    }

    val doomed = confirming
    if (doomed != null) {
        PinBomDialog(onDismissRequest = { confirming = null }) {
            PinBomDialogContent(
                title = stringResource(R.string.board_delete_title),
                body = stringResource(R.string.board_delete_body),
                confirmLabel = stringResource(R.string.action_delete),
                onConfirm = {
                    vm.delete(doomed.id)
                    confirming = null
                },
                onDismiss = { confirming = null },
                dismissLabel = stringResource(R.string.action_cancel),
                destructive = true,
            )
        }
    }
}
