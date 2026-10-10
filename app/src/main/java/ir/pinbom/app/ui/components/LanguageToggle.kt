package ir.pinbom.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ir.pinbom.app.R
import ir.pinbom.app.ui.theme.AppLanguage
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.LocalAppLanguage
import ir.pinbom.app.ui.theme.pinbomClick

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · compact language pill — “EN | فا”
 *  Sits in the top-end corner of the auth and server-setup screens, so somebody
 *  who cannot read the language the app came up in can switch before signing in.
 *  Each label is written in its own language.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun LanguageTogglePill(
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PinBomTheme.colors
    val current = LocalAppLanguage.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.surfaceGrey)
            .padding(Dimens.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLanguage.entries.forEach { language ->
            val selected = language == current
            /* “EN | فا” — always in the language it names, never in the UI language. */
            val label = stringResource(
                if (language == AppLanguage.English) R.string.language_short_en
                else R.string.language_short_fa,
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) colors.surface else Color.Transparent)
                    .pinbomClick(
                        onClick = { onSelect(language) },
                        onClickLabel = language.nativeName,
                        role = Role.RadioButton,
                        pressedScale = 0.94f,
                    )
                    .sizeIn(minWidth = 44.dp, minHeight = 32.dp)
                    .padding(horizontal = Dimens.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = PinBomText.caption,
                    color = if (selected) colors.textPrimary else colors.textTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}
