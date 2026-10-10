package ir.pinbom.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Board
import ir.pinbom.app.ui.BoardsViewModel
import ir.pinbom.app.ui.UploadViewModel
import ir.pinbom.app.ui.components.ActionToastHost
import ir.pinbom.app.ui.components.BoardCreateRow
import ir.pinbom.app.ui.components.BoardRow
import ir.pinbom.app.ui.components.PinBomBottomSheet
import ir.pinbom.app.ui.components.PinBomDialog
import ir.pinbom.app.ui.components.PinBomTextField
import ir.pinbom.app.ui.components.ContentTextField
import ir.pinbom.app.ui.components.IconCircleButton
import ir.pinbom.app.ui.components.PillButtonSize
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.components.SecondaryPillButton
import ir.pinbom.app.ui.components.SheetHeader
import ir.pinbom.app.ui.components.rememberPinBomToastState
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.pinbomClick
import ir.pinbom.app.work.UploadWorker

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · ساخت پین
 *  Pick the image, name it, tag it, drop it in a board — then the upload is
 *  handed to WorkManager, so leaving the screen never loses it. The queue keys
 *  are the same six strings the worker already reads.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun CreatePinScreen(
    session: SessionManager,
    boardsVm: BoardsViewModel = hiltViewModel(),
    /* Handed down by the nav shell, which owns the one upload card in the app.
     * The default only matters if the screen is ever shown on its own. */
    uploads: UploadViewModel = hiltViewModel(),
) {
    val toast = rememberPinBomToastState()

    val boards by boardsVm.items.collectAsStateWithLifecycle()
    val lastAction by boardsVm.lastAction.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { boardsVm.load() }

    var uri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var board by remember { mutableStateOf<Board?>(null) }
    var boardSheet by remember { mutableStateOf(false) }
    var newBoardOpen by remember { mutableStateOf(false) }
    var newBoardName by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) uri = picked
    }

    /* Android 13+ wants an explicit yes before the upload can post its progress
     * notification. Asked for at the first publish and never waited on: whatever
     * the answer, the picture goes up — the difference is only whether there is a
     * notification in the shade to watch it. A denied (or never asked) result
     * leaves the in-app progress card, which needs no permission at all. */
    val context = LocalContext.current
    val notifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val askForNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val pickLabel = stringResource(R.string.create_pick_image)
    val createdLabel = stringResource(R.string.board_created)
    val errorLabel = stringResource(R.string.state_error_title)

    LaunchedEffect(lastAction) {
        when (lastAction.substringBefore(':')) {
            "created" -> toast.show(createdLabel)
            "failed" -> toast.show(errorLabel)
        }
    }

    val canPublish = uri != null && title.isNotBlank()

    fun publish() {
        val picked = uri ?: return
        if (title.isBlank()) return

        askForNotifications()

        /* Same contract as before the redesign: these exact keys, this queue.
         * The upload ViewModel keeps the six of them, prepares the picked image
         * into cacheDir/uploads and remembers the work id — which is what lets
         * the progress card follow the bytes all the way to the server. */
        uploads.start(
            uri = picked,
            title = title,
            description = description,
            tags = tags,
            server = session.baseUrl,
            boardId = board?.id?.toString().orEmpty(),
        )

        uri = null
        title = ""
        description = ""
        tags = ""
        board = null
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.topBar)
                    .padding(horizontal = Dimens.screen),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.create_title),
                    style = PinBomText.titleLarge,
                    color = PinBomTheme.colors.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                PrimaryPillButton(
                    text = stringResource(R.string.create_upload),
                    onClick = { publish() },
                    enabled = canPublish,
                    leadingIcon = Icons.Outlined.Upload,
                    size = PillButtonSize.Small,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screen)
                    .padding(bottom = Dimens.navBarSpace),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(PinBomShape.pin)
                        .background(PinBomTheme.colors.surfaceGrey)
                        .pinbomClick(
                            onClick = { launcher.launch("image/*") },
                            onClickLabel = pickLabel,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val picked = uri
                    if (picked == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = PinBomTheme.colors.textTertiary,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.height(Dimens.sm))
                            Text(
                                text = pickLabel,
                                style = PinBomText.body,
                                color = PinBomTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        AsyncImage(
                            model = picked,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        IconCircleButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.create_remove_image),
                            onClick = { uri = null },
                            containerColor = PinBomTheme.colors.surface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Dimens.sm),
                        )
                    }
                }

                Spacer(Modifier.height(Dimens.lg))

                ContentTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.create_field_title),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.md))
                ContentTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = stringResource(R.string.create_field_description),
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.md))
                ContentTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    placeholder = stringResource(R.string.create_field_tags),
                    leadingIcon = Icons.Outlined.Tag,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Dimens.lg))

                BoardRow(
                    title = board?.title ?: stringResource(R.string.create_choose_board),
                    onClick = { boardSheet = true },
                    covers = listOf(board?.cover_url.orEmpty()).filter { it.isNotBlank() },
                    seed = board?.id ?: 0L,
                    baseUrl = session.baseUrl,
                    isPrivate = board?.private_board == true,
                    selected = board != null,
                )

                Spacer(Modifier.height(Dimens.huge))
            }
        }

        ActionToastHost(
            state = toast,
            modifier = Modifier.align(Alignment.BottomCenter),
            baseUrl = session.baseUrl,
        )
    }

    if (boardSheet) {
        PinBomBottomSheet(onDismissRequest = { boardSheet = false }) {
            SheetHeader(title = stringResource(R.string.pin_save_to_board))
            if (boards.isEmpty()) {
                Text(
                    text = stringResource(R.string.pin_no_boards),
                    style = PinBomText.bodySmall,
                    color = PinBomTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = Dimens.md),
                )
            }
            boards.forEach { candidate ->
                BoardRow(
                    title = candidate.title,
                    onClick = {
                        board = candidate
                        boardSheet = false
                    },
                    modifier = Modifier.padding(bottom = Dimens.xs),
                    selected = board?.id == candidate.id,
                    covers = listOf(candidate.cover_url).filter { it.isNotBlank() },
                    seed = candidate.id,
                    baseUrl = session.baseUrl,
                    isPrivate = candidate.private_board,
                )
            }
            Spacer(Modifier.height(Dimens.sm))
            BoardCreateRow(
                title = stringResource(R.string.pin_new_board),
                onClick = {
                    boardSheet = false
                    newBoardOpen = true
                },
            )
        }
    }

    if (newBoardOpen) {
        PinBomDialog(onDismissRequest = { newBoardOpen = false }) {
            Text(
                text = stringResource(R.string.pin_new_board),
                style = PinBomText.title,
                color = PinBomTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(Dimens.md))
            ContentTextField(
                value = newBoardName,
                onValueChange = { newBoardName = it },
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
                    onClick = { newBoardOpen = false },
                    size = PillButtonSize.Small,
                )
                PrimaryPillButton(
                    text = stringResource(R.string.action_done),
                    enabled = newBoardName.isNotBlank(),
                    onClick = {
                        boardsVm.create(newBoardName.trim(), "")
                        newBoardName = ""
                        newBoardOpen = false
                    },
                    size = PillButtonSize.Small,
                )
            }
        }
    }
}
