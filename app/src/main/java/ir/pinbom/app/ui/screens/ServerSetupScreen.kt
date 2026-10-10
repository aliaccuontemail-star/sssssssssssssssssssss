package ir.pinbom.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.pinbom.app.BuildConfig
import ir.pinbom.app.R
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.ServerProbe
import ir.pinbom.app.ui.components.PinBomBrandStack
import ir.pinbom.app.ui.components.PinBomTextField
import ir.pinbom.app.ui.components.FieldDirection
import ir.pinbom.app.ui.components.LanguageTogglePill
import ir.pinbom.app.ui.components.PrimaryPillButton
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Dimens
import ir.pinbom.app.ui.theme.LocalAppLanguageState
import kotlinx.coroutines.launch

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · connect to server
 *  The very first screen of a fresh install: the vertical lockup, one honest
 *  sentence and the address. The URL field is Latin technical input, so its
 *  characters stay LTR in either language. Nothing is written to DataStore until
 *  the URL looks real.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ServerSetupScreen(
    session: SessionManager,
    onDone: () -> Unit,
) {
    val colors = PinBomTheme.colors
    val scope = rememberCoroutineScope()
    val languageState = LocalAppLanguageState.current

    var url by remember {
        mutableStateOf(session.baseUrl.ifBlank { BuildConfig.DEFAULT_BASE_URL.trimEnd('/') })
    }
    var invalid by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    /* The address is proven before it is saved. Without this the screen accepted
     * anything well-formed — including the pre-filled 127.0.0.1, which on a phone
     * is the phone — and the failure only surfaced later, as a bare connection
     * error on the sign-in screen, with no hint that the address was the cause. */
    var failure by remember { mutableStateOf<String?>(null) }
    var loopback by remember { mutableStateOf(false) }

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
                .padding(horizontal = Dimens.xxl, vertical = Dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PinBomBrandStack(markSize = 64.dp, animated = false)
            Spacer(Modifier.height(Dimens.xxl))
            Text(
                text = stringResource(R.string.server_title),
                style = PinBomText.titleLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(Dimens.sm))
            Text(
                text = stringResource(R.string.server_body),
                style = PinBomText.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.xxl))

            PinBomTextField(
                value = url,
                onValueChange = {
                    url = it
                    invalid = false
                    failure = null
                    loopback = false
                },
                placeholder = stringResource(R.string.server_placeholder),
                leadingIcon = Icons.Outlined.Public,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                fieldDirection = FieldDirection.Latin,
                error = when {
                    invalid -> stringResource(R.string.server_invalid)
                    failure != null -> stringResource(R.string.server_unreachable, failure.orEmpty())
                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            /* The one misconfiguration that reads as "the app is broken": a
             * loopback address means a different machine on every kind of device. */
            if (loopback) {
                Spacer(Modifier.height(Dimens.sm))
                Text(
                    text = stringResource(R.string.server_loopback_hint),
                    style = PinBomText.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(Dimens.lg))

            PrimaryPillButton(
                text = stringResource(R.string.server_connect),
                onClick = {
                    val cleaned = url.trim().trimEnd('/')
                    if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
                        busy = true
                        invalid = false
                        failure = null
                        loopback = false
                        scope.launch {
                            when (val probe = ServerProbe.check(cleaned)) {
                                is ServerProbe.Result.Reachable -> {
                                    session.setServer(cleaned)
                                    busy = false
                                    onDone()
                                }

                                is ServerProbe.Result.Unreachable -> {
                                    busy = false
                                    failure = probe.reason
                                    loopback = ServerProbe.isLoopback(cleaned)
                                }
                            }
                        }
                    } else {
                        invalid = true
                    }
                },
                loading = busy,
                enabled = !busy && url.isNotBlank(),
                fillWidth = true,
            )
        }

        if (languageState != null) {
            LanguageTogglePill(
                onSelect = { languageState.select(it) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.md),
            )
        }
    }
}
