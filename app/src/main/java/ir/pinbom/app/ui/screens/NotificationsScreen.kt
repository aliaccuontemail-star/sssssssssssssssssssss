package ir.pinbom.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Notification
import ir.pinbom.app.ui.NotificationsViewModel
import ir.pinbom.app.ui.components.Avatar
import ir.pinbom.app.ui.components.BadgeDot
import ir.pinbom.app.ui.components.PinBomPullToRefresh
import ir.pinbom.app.ui.components.EmptyState
import ir.pinbom.app.ui.components.ErrorState
import ir.pinbom.app.ui.components.ListRowSkeleton
import ir.pinbom.app.ui.components.RelativeTimeText
import ir.pinbom.app.ui.components.ContentText
import ir.pinbom.app.ui.nav.Routes
import ir.pinbom.app.ui.theme.PinBomShape
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.pinbomClick

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · اعلان‌ها
 *  Unread rows carry a brand ring, a tinted surface and a dot; tapping opens the
 *  pin if the notification points at one, otherwise the person. Reading the
 *  list marks it read on the server, exactly as before.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun NotificationsScreen(
    nav: NavHostController,
    session: SessionManager,
    vm: NotificationsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        vm.load()
        vm.markRead()
    }

    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()

    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(loading) { if (!loading) pulling = false }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.topBar)
                .padding(horizontal = Dimens.screen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.notifications_title),
                style = PinBomText.titleLarge,
                color = PinBomTheme.colors.textPrimary,
            )
        }

        when {
            items.isEmpty() && loading -> NotificationSkeleton()

            items.isEmpty() && failed -> PullableCenter(modifier = Modifier.weight(1f)) {
                ErrorState(onRetry = { vm.load() })
            }

            items.isEmpty() -> PullableCenter(modifier = Modifier.weight(1f)) {
                EmptyState(
                    title = stringResource(R.string.notifications_empty_title),
                    description = stringResource(R.string.notifications_empty_body),
                    icon = Icons.Outlined.Notifications,
                )
            }

            else -> PinBomPullToRefresh(
                isRefreshing = pulling && loading,
                onRefresh = {
                    pulling = true
                    vm.load()
                },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.sm,
                        end = Dimens.sm,
                        bottom = Dimens.huge + Dimens.navBarSpace,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.xxs),
                ) {
                    items(items = items, key = { it.id }, contentType = { "notification" }) { item ->
                        NotificationRow(
                            item = item,
                            baseUrl = session.baseUrl,
                            onOpen = {
                                val handle = item.actor?.username.orEmpty()
                                when {
                                    item.pin_id > 0 -> nav.navigate(Routes.pin(item.pin_id))
                                    handle.isNotBlank() -> nav.navigate(Routes.profile(handle))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/* ── Row ──────────────────────────────────────────────────────────────────── */

@Composable
private fun NotificationRow(item: Notification, baseUrl: String, onOpen: () -> Unit) {
    val colors = PinBomTheme.colors
    val fallback = stringResource(R.string.app_name)
    val unreadLabel = stringResource(R.string.cd_unread)
    val handle = item.actor?.username.orEmpty()
    val name = item.actor?.display_name.orEmpty().ifBlank { handle }.ifBlank { fallback }
    val hasTarget = item.pin_id > 0 || handle.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PinBomShape.field)
            .background(if (item.read) Color.Transparent else colors.surfaceGrey)
            .pinbomClick(
                onClick = onOpen,
                enabled = hasTarget,
                indication = ripple(bounded = true, color = colors.textSecondary),
                role = Role.Button,
            )
            .padding(horizontal = Dimens.md, vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            name = name,
            baseUrl = baseUrl,
            size = 44.dp,
            ring = if (item.read) null else colors.primary,
        )
        Spacer(Modifier.width(Dimens.md))
        Column(Modifier.weight(1f)) {
            ContentText(
                text = name,
                style = PinBomText.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Dimens.xxs))
            ContentText(
                text = item.body,
                style = PinBomText.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Dimens.xs))
            RelativeTimeText(iso = item.created_at)
        }
        if (!item.read) {
            Spacer(Modifier.width(Dimens.sm))
            BadgeDot(modifier = Modifier.semantics { contentDescription = unreadLabel })
        }
    }
}

/* ── Loading ──────────────────────────────────────────────────────────────── */

@Composable
private fun NotificationSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.md, vertical = Dimens.sm),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        repeat(6) {
            ListRowSkeleton()
        }
    }
}
