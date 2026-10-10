package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · pull to refresh
 *  A thin wrapper so every list gets the same brand-red indicator. Material 3’s
 *  default is a muted onSurface arrow; ours matches the logo.
 * ──────────────────────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinBomPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = PinBomTheme.colors

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.surface,
                color = colors.primary,
            )
        },
        content = content,
    )
}

/** Full-span footer spinner shown while the next page loads. */
@Composable
fun AppendSpinner(modifier: Modifier = Modifier, visible: Boolean = true) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.lg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp,
            color = PinBomTheme.colors.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "PullToRefresh · light", backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 420)
@Composable
private fun PreviewPullLight() {
    PinBomTheme(darkTheme = false) {
        PinBomPullToRefresh(
            isRefreshing = false,
            onRefresh = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(PinBomTheme.colors.background),
        ) {
            Column {
                ListRowSkeleton()
                ListRowSkeleton()
                AppendSpinner()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "PullToRefresh · dark", backgroundColor = 0xFF121212, widthDp = 360, heightDp = 420)
@Composable
private fun PreviewPullDark() {
    PinBomTheme(darkTheme = true) {
        PinBomPullToRefresh(
            isRefreshing = true,
            onRefresh = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(PinBomTheme.colors.background),
        ) {
            Column {
                ListRowSkeleton()
                ListRowSkeleton()
            }
        }
    }
}
