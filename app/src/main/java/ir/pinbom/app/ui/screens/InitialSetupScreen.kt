package ir.pinbom.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.pinbom.app.R
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.components.SecondaryPillButton
import ir.pinbom.app.ui.theme.AppLanguage
import ir.pinbom.app.ui.theme.BrandRed
import ir.pinbom.app.ui.theme.LocalAppLanguage
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.PinBomText

private val AccentChoices = listOf(
    BrandRed,
    Color(0xFF6750A4),
    Color(0xFF2878D0),
    Color(0xFF008577),
    Color(0xFF2E8540),
    Color(0xFFE87922),
    Color(0xFFB83280),
    Color(0xFF455A64),
)

@Composable
fun InitialSetupScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    accentColor: Color,
    onAccentChange: (Color) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onComplete: () -> Unit,
) {
    val colors = PinBomTheme.colors
    val language = LocalAppLanguage.current
    val surfaceBorder = colors.divider
    var step by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = accentColor, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.setup_title), style = PinBomText.titleLarge, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.setup_subtitle), style = PinBomText.body, color = colors.textSecondary)
        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(2) { index ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(CircleShape)
                        .background(if (index <= step) accentColor else colors.surfaceGrey),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (step == 0) stringResource(R.string.setup_step_one) else stringResource(R.string.setup_step_two),
            style = PinBomText.caption,
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(250)) { width -> if (forward) width / 5 else -width / 5 } +
                    fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { width -> if (forward) -width / 5 else width / 5 } +
                        fadeOut(tween(160)))
            },
            label = "initialSetupPages",
        ) { page ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, surfaceBorder, RoundedCornerShape(24.dp)),
            ) {
                Column(Modifier.padding(18.dp)) {
                    if (page == 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = colors.textSecondary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(stringResource(R.string.setup_dark_mode), style = PinBomText.title, color = colors.textPrimary)
                                Text(stringResource(R.string.setup_dark_mode_desc), style = PinBomText.bodySmall, color = colors.textSecondary)
                            }
                            Switch(checked = dark, onCheckedChange = onDarkChange)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Palette, contentDescription = null, tint = colors.textSecondary)
                            Text(
                                stringResource(R.string.setup_button_color),
                                style = PinBomText.title,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AccentChoices.forEach { choice ->
                                val selected = choice == accentColor
                                val stroke by animateColorAsState(
                                    targetValue = if (selected) choice else surfaceBorder,
                                    animationSpec = tween(160),
                                    label = "setupAccentBorder",
                                )
                                Box(
                                    Modifier.size(31.dp).clip(CircleShape).background(choice)
                                        .border(if (selected) 3.dp else 1.dp, stroke, CircleShape)
                                        .clickable(role = Role.RadioButton, onClick = { onAccentChange(choice) })
                                        .semantics { role = Role.RadioButton },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Text(stringResource(R.string.setup_language), style = PinBomText.title, color = colors.textPrimary)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            AppLanguage.entries.forEach { option ->
                                val selected = option == language
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) accentColor.copy(alpha = 0.12f) else colors.surfaceGrey)
                                        .border(if (selected) 1.5.dp else 1.dp, if (selected) accentColor else surfaceBorder, RoundedCornerShape(14.dp))
                                        .clickable(role = Role.RadioButton, onClick = { onLanguageChange(option) })
                                        .semantics { role = Role.RadioButton }
                                        .padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(option.nativeName, style = PinBomText.body, color = if (selected) accentColor else colors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        if (step == 0) {
            PrimaryPillButton(
                text = stringResource(R.string.setup_next),
                onClick = { step = 1 },
                fillWidth = true,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryPillButton(
                    text = stringResource(R.string.setup_back),
                    onClick = { step = 0 },
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
                PrimaryPillButton(
                    text = stringResource(R.string.setup_finish),
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.setup_footer),
            style = PinBomText.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
