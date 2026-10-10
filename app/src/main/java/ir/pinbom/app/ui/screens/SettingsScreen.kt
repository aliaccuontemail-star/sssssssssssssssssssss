package ir.pinbom.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.ui.SettingsViewModel
import ir.pinbom.app.ui.components.ActionToastHost
import ir.pinbom.app.ui.components.PinBomBottomSheet
import ir.pinbom.app.ui.components.PinBomDialog
import ir.pinbom.app.ui.components.PinBomDialogContent
import ir.pinbom.app.ui.components.ContentTextField
import ir.pinbom.app.ui.components.IconCircleButton
import ir.pinbom.app.ui.components.NavBarStyle
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.components.SecondaryPillButton
import ir.pinbom.app.ui.components.SectionHeader
import ir.pinbom.app.ui.components.SheetHeader
import ir.pinbom.app.ui.components.labelRes
import ir.pinbom.app.ui.components.rememberPinBomToastState
import ir.pinbom.app.ui.theme.AppLanguage
import ir.pinbom.app.ui.theme.BrandRed
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.LocalAppLanguage
import ir.pinbom.app.ui.theme.LocalAppLanguageState
import ir.pinbom.app.ui.theme.pinbomClick
import ir.pinbom.app.ui.theme.pinbomTextStyles

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · settings
 *  Profile fields, the theme switch, the language, the server that is actually in
 *  use, and the two ways out of the account — the destructive one behind a
 *  dialog. Changing the language re-renders the whole screen (strings, direction,
 *  digits, font) and keeps everything else exactly where it was.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun SettingsScreen(
    nav: NavHostController,
    session: SessionManager,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    accentColor: Color = BrandRed,
    onAccentChange: (Color) -> Unit = {},
    onLogout: () -> Unit,
    navStyle: NavBarStyle = NavBarStyle.Default,
    onNavStyleChange: (NavBarStyle) -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val colors = PinBomTheme.colors
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val lastAction by vm.lastAction.collectAsStateWithLifecycle()

    val toast = rememberPinBomToastState()
    val languageState = LocalAppLanguageState.current
    val language = LocalAppLanguage.current

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var languageSheet by remember { mutableStateOf(false) }
    var styleSheet by remember { mutableStateOf(false) }

    /* Success and failure that carry no server text are worded here, so they
     * arrive in the language the app is in right now. */
    val savedLabel = stringResource(R.string.settings_saved)
    val errorLabel = stringResource(R.string.state_error_title)
    LaunchedEffect(lastAction) {
        when (lastAction.substringBefore(':')) {
            "saved" -> toast.show(savedLabel)
            "failed" -> toast.show(message.ifBlank { errorLabel })
        }
    }

    /* Toast after a language switch: read in the *new* language, which is why it
     * fires from the language change instead of from the tap. */
    val languageChangedLabel = stringResource(R.string.settings_language_changed)
    val initialLanguage = remember { language }
    LaunchedEffect(language) {
        if (language != initialLanguage) toast.show(languageChangedLabel)
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
                    containerColor = colors.surfaceGrey,
                )
                Spacer(Modifier.width(Dimens.sm))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = PinBomText.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "profile-header") {
                    SectionHeader(title = stringResource(R.string.settings_profile_section))
                }
                item(key = "profile-fields") {
                    Column(Modifier.padding(horizontal = Dimens.screen)) {
                        ContentTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = stringResource(R.string.settings_display_name),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(Dimens.md))
                        ContentTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = stringResource(R.string.settings_bio),
                            singleLine = false,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(Dimens.md))
                        PrimaryPillButton(
                            text = stringResource(R.string.settings_save_profile),
                            onClick = { vm.save(name.trim(), bio.trim()) },
                            enabled = !busy && (name.isNotBlank() || bio.isNotBlank()),
                            loading = busy,
                            fillWidth = true,
                        )
                    }
                }
                item(key = "appearance-header") {
                    SectionHeader(title = stringResource(R.string.settings_appearance))
                }
                item(key = "appearance-row") {
                    Column(Modifier.padding(horizontal = Dimens.screen)) {
                        Text(
                            text = stringResource(R.string.settings_button_color),
                            style = PinBomText.body,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = Dimens.sm, bottom = Dimens.xs),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            listOf(
                                BrandRed, Color(0xFF6750A4), Color(0xFF2878D0), Color(0xFF008577),
                                Color(0xFF2E8540), Color(0xFFE87922), Color(0xFFB83280), Color(0xFF455A64),
                            ).forEach { choice ->
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(choice)
                                        .border(if (choice == accentColor) 3.dp else 1.dp,
                                            if (choice == accentColor) colors.textPrimary else colors.divider, CircleShape)
                                        .clickable(role = Role.RadioButton, onClick = { onAccentChange(choice) })
                                        .semantics { selected = choice == accentColor; role = Role.RadioButton },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (choice == accentColor) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        SettingsToggleRow(
                            icon = if (dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            title = stringResource(R.string.settings_dark_mode),
                            checked = dark,
                            onCheckedChange = onDarkChange,
                        )
                    }
                }
                item(key = "language-row") {
                    Column(Modifier.padding(horizontal = Dimens.screen)) {
                        SettingsValueRow(
                            icon = Icons.Outlined.Translate,
                            title = stringResource(R.string.settings_language),
                            value = language.nativeName,
                            onClick = { languageSheet = true },
                        )
                    }
                }
                item(key = "nav-style-row") {
                    Column(Modifier.padding(horizontal = Dimens.screen)) {
                        SettingsValueRow(
                            icon = Icons.Outlined.SmartButton,
                            title = stringResource(R.string.settings_nav_bar),
                            value = stringResource(navStyle.labelRes()),
                            onClick = { styleSheet = true },
                        )
                    }
                }
                item(key = "server-header") {
                    SectionHeader(title = stringResource(R.string.settings_server))
                }
                item(key = "server-row") {
                    Column(Modifier.padding(horizontal = Dimens.screen)) {
                        SettingsInfoRow(
                            icon = Icons.Outlined.Public,
                            title = session.baseUrl,
                        )
                    }
                }
                item(key = "account-header") {
                    SectionHeader(title = stringResource(R.string.settings_account))
                }
                item(key = "account-actions") {
                    Column(Modifier.padding(horizontal = Dimens.screen)) {
                        SecondaryPillButton(
                            text = stringResource(R.string.settings_logout),
                            onClick = onLogout,
                            leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                            fillWidth = true,
                        )
                        Spacer(Modifier.height(Dimens.sm))
                        SecondaryPillButton(
                            text = stringResource(R.string.settings_delete_account),
                            onClick = { confirmDelete = true },
                            leadingIcon = Icons.Outlined.DeleteOutline,
                            fillWidth = true,
                        )
                        Spacer(Modifier.height(Dimens.huge))
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

    if (languageSheet) {
        PinBomBottomSheet(onDismissRequest = { languageSheet = false }) {
            SheetHeader(title = stringResource(R.string.settings_language))
            AppLanguage.entries.forEach { option ->
                LanguageRow(
                    language = option,
                    selected = option == language,
                    onClick = {
                        languageSheet = false
                        languageState?.select(option)
                    },
                )
            }
            Spacer(Modifier.height(Dimens.sm))
        }
    }

    if (styleSheet) {
        PinBomBottomSheet(onDismissRequest = { styleSheet = false }) {
            SheetHeader(title = stringResource(R.string.settings_nav_bar))
            NavBarStyle.entries.forEach { option ->
                NavStyleRow(
                    label = stringResource(option.labelRes()),
                    selected = option == navStyle,
                    onClick = {
                        styleSheet = false
                        onNavStyleChange(option)
                    },
                )
            }
            Spacer(Modifier.height(Dimens.sm))
        }
    }

    if (confirmDelete) {
        PinBomDialog(onDismissRequest = { confirmDelete = false }) {
            PinBomDialogContent(
                title = stringResource(R.string.settings_delete_title),
                body = stringResource(R.string.settings_delete_body),
                confirmLabel = stringResource(R.string.settings_delete_account),
                onConfirm = {
                    confirmDelete = false
                    vm.delete(onLogout)
                },
                onDismiss = { confirmDelete = false },
                dismissLabel = stringResource(R.string.action_cancel),
                destructive = true,
            )
        }
    }
}

/* ── Rows ─────────────────────────────────────────────────────────────────── */

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = PinBomTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.md))
        Text(
            text = title,
            style = PinBomText.body,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = colors.surface,
                uncheckedTrackColor = colors.surfaceGreyPressed,
                uncheckedBorderColor = colors.divider,
            ),
        )
    }
}

/** Row with a current value and a chevron that follows the reading direction. */
@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    val colors = PinBomTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pinbomClick(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = title,
                pressedScale = 0.99f,
            )
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.md))
        Text(
            text = title,
            style = PinBomText.body,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = PinBomText.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.width(Dimens.xs))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String) {
    val colors = PinBomTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.md))
        Text(
            text = title,
            style = PinBomText.body,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One navigation-bar style. Same row language as the language picker next to it:
 * radio semantics, a check on the current choice, no new component.
 */
@Composable
private fun NavStyleRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = PinBomTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pinbomClick(
                onClick = onClick,
                role = Role.RadioButton,
                onClickLabel = label,
                pressedScale = 0.99f,
            )
            .padding(vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = PinBomText.body,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(Modifier.width(Dimens.sm))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * One language choice. Each name is written in its own language *and* its own
 * font, so “فارسی” never renders in Inter and “English” never in Vazirmatn —
 * whichever language the UI happens to be in.
 */
@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = PinBomTheme.colors
    val styles = remember(language) { pinbomTextStyles(language) }
    val label = language.nativeName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pinbomClick(
                onClick = onClick,
                role = Role.RadioButton,
                onClickLabel = label,
                pressedScale = 0.99f,
            )
            .padding(vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val direction = if (language.isRtl) TextDirection.Rtl else TextDirection.Ltr
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = styles.body.copy(textAlign = TextAlign.Start, textDirection = direction),
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (selected) {
            Spacer(Modifier.width(Dimens.sm))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
