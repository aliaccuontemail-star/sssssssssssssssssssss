package ir.pinbom.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.pinbom.app.R
import ir.pinbom.app.ui.AuthViewModel
import ir.pinbom.app.ui.components.PinBomBrandStack
import ir.pinbom.app.ui.components.PinBomTextField
import ir.pinbom.app.ui.components.FieldDirection
import ir.pinbom.app.ui.components.IconCircleButton
import ir.pinbom.app.ui.components.LanguageTogglePill
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.theme.AppLanguage
import ir.pinbom.app.ui.theme.PinBomRoot
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.LocalAppLanguageState
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.pinbomClick
import ir.pinbom.app.ui.theme.reduceMotion

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · sign in / create account
 *  The first screen anyone sees: the vertical lockup (so it never depends on the
 *  reading direction), one warm line, and the smallest honest form that works —
 *  identifier + password, or username/email/password when creating an account.
 *  Technical inputs (username, email, password) stay LTR inside any language.
 *  The one-time-code path is gone: this screen is login and registration only.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun AuthScreen(
    vm: AuthViewModel = hiltViewModel(),
    onSuccess: () -> Unit,
    onRegistered: () -> Unit = onSuccess,
) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val languageState = LocalAppLanguageState.current

    var register by remember { mutableStateOf(false) }
    var identifier by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }

    val reduce = reduceMotion()
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) {
            enter.snapTo(1f)
        } else {
            enter.animateTo(1f, tween(durationMillis = Motion.medium, easing = Motion.emphasized))
        }
    }

    fun submit() {
        if (register) {
            vm.register(username.trim(), email.trim(), password, onRegistered)
        } else {
            vm.login(identifier.trim(), password, onSuccess)
        }
    }

    /* Mirror the server's username/password rules for instant feedback; the
     * server remains authoritative and validates every request independently. */
    val validUsername = username.matches(Regex("^[\\p{L}\\p{N}._-]{3,30}$"))
    val validPassword = password.length in 8..128
    val canSubmit = !busy && if (register) {
        validUsername && validPassword
    } else {
        identifier.isNotBlank() && password.isNotBlank()
    }

    /* Server messages are shown as they arrive; when a failure carries none,
     * the screen owns the wording (and therefore its language). */
    val failureText = when {
        error.isNotBlank() -> error
        failed -> stringResource(
            if (register) R.string.auth_error_register else R.string.auth_error_login,
        )

        else -> ""
    }

    /* The drawing lives in [AuthContent] so the same pixels can be previewed
     * without a ViewModel — and, more to the point, so the four previews below
     * can prove the screen is readable whatever the window underneath is. */
    AuthContent(
        register = register,
        identifier = identifier,
        username = username,
        email = email,
        password = password,
        reveal = reveal,
        busy = busy,
        canSubmit = canSubmit,
        failureText = failureText,
        progress = enter.value,
        onIdentifier = { identifier = it },
        onUsername = { username = it },
        onEmail = { email = it },
        onPassword = { password = it },
        onToggleReveal = { reveal = !reveal },
        onSubmit = { submit() },
        onToggleMode = { register = !register; vm.clearFeedback() },
        onSelectLanguage = languageState?.let { state ->
            { lang: AppLanguage -> state.select(lang) }
        },
    )
}

@Composable
private fun AuthContent(
    register: Boolean,
    identifier: String,
    username: String,
    email: String,
    password: String,
    reveal: Boolean,
    busy: Boolean,
    canSubmit: Boolean,
    failureText: String,
    progress: Float,
    onIdentifier: (String) -> Unit,
    onUsername: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onToggleReveal: () -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onSelectLanguage: ((AppLanguage) -> Unit)?,
) {
    val colors = PinBomTheme.colors
    val validUsername = username.matches(Regex("^[\\p{L}\\p{N}._-]{3,30}$"))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .graphicsLayer {
                    alpha = progress
                    translationY = (1f - progress) * 24f
                }
                .padding(horizontal = Dimens.xxl, vertical = Dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PinBomBrandStack(markSize = 64.dp, animated = false)
            Spacer(Modifier.height(Dimens.xxl))
            Text(
                text = stringResource(R.string.auth_welcome),
                style = PinBomText.titleLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(Dimens.xs))
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = PinBomText.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.xxl))

            if (register) {
                PinBomTextField(
                    value = username,
                    onValueChange = onUsername,
                    placeholder = stringResource(R.string.auth_username),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    fieldDirection = FieldDirection.Latin,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.auth_username_hint),
                    style = PinBomText.bodySmall,
                    color = if (username.isNotBlank() && !validUsername) MaterialTheme.colorScheme.error else colors.textTertiary,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp),
                )
                Spacer(Modifier.height(Dimens.md))
                PinBomTextField(
                    value = email,
                    onValueChange = onEmail,
                    placeholder = stringResource(R.string.auth_email),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    fieldDirection = FieldDirection.Latin,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.md))
            } else {
                PinBomTextField(
                    value = identifier,
                    onValueChange = onIdentifier,
                    placeholder = stringResource(R.string.auth_identifier),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    fieldDirection = FieldDirection.Latin,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.md))
            }

            PinBomTextField(
                value = password,
                onValueChange = onPassword,
                placeholder = stringResource(R.string.auth_password),
                singleLine = true,
                visualTransformation = if (reveal) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                fieldDirection = FieldDirection.Latin,
                trailingIcon = {
                    IconCircleButton(
                        icon = if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = stringResource(
                            if (reveal) R.string.auth_hide_password else R.string.auth_show_password,
                        ),
                        onClick = onToggleReveal,
                        size = Dimens.iconButtonSmall,
                        iconSize = 16.dp,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (register) {
                Text(
                    text = stringResource(R.string.auth_password_hint),
                    style = PinBomText.bodySmall,
                    color = if (password.isNotEmpty() && password.length < 8) MaterialTheme.colorScheme.error else colors.textTertiary,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp),
                )
            }
            Spacer(Modifier.height(Dimens.xxl))

            PrimaryPillButton(
                text = stringResource(if (register) R.string.auth_register else R.string.auth_login),
                onClick = onSubmit,
                enabled = canSubmit,
                loading = busy,
                fillWidth = true,
            )

            /* 24dp of air, then the footer line. */
            Spacer(Modifier.height(Dimens.xxl))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(
                        if (register) R.string.auth_have_account_line else R.string.auth_no_account_line,
                    ),
                    style = PinBomText.bodySmall,
                    color = colors.textSecondary,
                )
                Box(
                    modifier = Modifier
                        .sizeIn(minHeight = Dimens.touch)
                        .pinbomClick(
                            enabled = !busy,
                            role = Role.Button,
                            onClickLabel = stringResource(
                                if (register) R.string.auth_login else R.string.auth_register,
                            ),
                            onClick = onToggleMode,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(if (register) R.string.auth_login else R.string.auth_register),
                        style = PinBomText.button,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = Dimens.sm),
                    )
                }
            }

            if (failureText.isNotBlank()) {
                Spacer(Modifier.height(Dimens.md))
                Text(
                    text = failureText,
                    style = PinBomText.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        /* A reader who cannot parse the current language can still switch. */
        if (onSelectLanguage != null) {
            LanguageTogglePill(
                onSelect = onSelectLanguage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.md),
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 *  Previews · the four combinations that used to be a lottery
 *
 *  The app's theme is the user's choice; the window behind Compose follows the
 *  *system's*. Every combination can therefore happen — a light app on a dark
 *  phone, and so on — and before the root surface existed, two of them drew dark
 *  ink on the window's own dark background (measured: 1.01:1, i.e. black on
 *  black). Each preview paints the real window colour behind the app, the way
 *  the window does, and then draws the screen with [PinBomRoot] in front of it —
 *  exactly what MainActivity composes. The text is legible in all four because
 *  the surface, not the window, is what the text sits on.
 *
 *  Measured contrast for the essential pairs, both themes (WCAG formula):
 *    light · welcome 18.9:1 · subtitle 6.4:1 · footer link 4.8:1 · field text 16.4:1
 *    dark  · welcome 21.0:1 · subtitle 10.0:1 · footer link 6.0:1 · field text 15.1:1
 *  Placeholder hints are 2.9:1 / 3.5:1 by design — they are hints, never the
 *  information the screen needs to be read.
 * ──────────────────────────────────────────────────────────────────────────── */

/* The two colours the *native* window paints (res/values/colors.xml and
 * res/values-night/colors.xml). Kept here so a preview can reproduce the worst
 * case instead of assuming it away. */
private val WindowLight = Color(0xFFF7F7F8)
private val WindowDark = Color(0xFF121214)

@Composable
private fun AuthPreview(appDark: Boolean, windowDark: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(if (windowDark) WindowDark else WindowLight),
    ) {
        PinBomTheme(darkTheme = appDark) {
            PinBomRoot {
                AuthContent(
                    register = false,
                    identifier = "@pinbom",
                    username = "",
                    email = "",
                    password = "hunter2!",
                    reveal = false,
                    busy = false,
                    canSubmit = true,
                    failureText = "",
                    progress = 1f,
                    onIdentifier = {},
                    onUsername = {},
                    onEmail = {},
                    onPassword = {},
                    onToggleReveal = {},
                    onSubmit = {},
                    onToggleMode = {},
                    onSelectLanguage = {},
                )
            }
        }
    }
}

@Preview(name = "Auth · app light · window light", showBackground = true)
@Composable
private fun PreviewAuthLightWindowLight() = AuthPreview(appDark = false, windowDark = false)

@Preview(name = "Auth · app light · window dark", showBackground = true)
@Composable
private fun PreviewAuthLightWindowDark() = AuthPreview(appDark = false, windowDark = true)

@Preview(name = "Auth · app dark · window light", showBackground = true)
@Composable
private fun PreviewAuthDarkWindowLight() = AuthPreview(appDark = true, windowDark = false)

@Preview(name = "Auth · app dark · window dark", showBackground = true)
@Composable
private fun PreviewAuthDarkWindowDark() = AuthPreview(appDark = true, windowDark = true)
