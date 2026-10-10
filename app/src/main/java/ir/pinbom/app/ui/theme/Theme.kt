package ir.pinbom.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · theme entry point
 *
 *  Three things are decided here, all from the selected language:
 *    • the layout direction (fa → RTL, en → LTR; nothing is hard-forced),
 *    • the locale the whole tree reads strings in — `LocalConfiguration` and
 *      `LocalResources` get the language's configuration and resources, which is
 *      exactly what `stringResource` (and `painterResource`, `colorResource`, …)
 *      read, so every built-in string API flips with the language,
 *    • the font family (Vazirmatn for Persian, Inter for English).
 *
 *  `LocalContext` is deliberately **not** overridden: it must stay the Activity.
 *  Hilt's ViewModel factory walks the `ContextWrapper` chain up to a
 *  `ComponentActivity` and throws
 *  «Expected an activity context for creating a HiltViewModelFactory» for
 *  anything else, and the activity-result / back-press / report-drawn owners
 *  resolve through it as well. Overriding it with a configuration context
 *  therefore crashes the first screen that asks for a ViewModel — the bug this
 *  file used to have. Screens that need *localized* text outside of Compose's
 *  resource APIs read `LocalResources.current`, not `LocalContext.current`.
 *
 *  Dark mode is *in-app* (Settings → Appearance), not just the system setting,
 *  so the status/navigation bar icons follow `darkTheme`, not the system value.
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun PinBomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    language: AppLanguage = AppLanguage.Persian,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    accentColor: Color = BrandRed,
    content: @Composable () -> Unit,
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val basePinbomColors = if (darkTheme) DarkPinBomColors else LightPinBomColors
    val selectedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 260),
        label = "buttonAccentTransition",
    )
    val selectedPressed = selectedAccent.copy(alpha = 0.82f)
    val colorScheme = baseScheme.copy(
        primary = selectedAccent,
        inversePrimary = selectedAccent,
        primaryContainer = selectedAccent.copy(alpha = if (darkTheme) 0.30f else 0.16f),
    )
    val pinbomColors = basePinbomColors.copy(
        primary = selectedAccent,
        primaryPressed = selectedPressed,
    )

    val languageState = rememberAppLanguageState(language = language, onSelect = onSelectLanguage)
    val direction = if (languageState.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val textStyles = remember(languageState.language) { pinbomTextStyles(languageState.language) }
    val typography = remember(languageState.language) { PinBomTypography(languageState.language) }

    CompositionLocalProvider(
        LocalPinBomColors provides pinbomColors,
        LocalPinBomTextStyles provides textStyles,
        LocalAppLanguage provides languageState.language,
        LocalAppLanguageState provides languageState,
        LocalLayoutDirection provides direction,
        LocalConfiguration provides languageState.configuration,
        LocalResources provides languageState.resources,
        LocalReduceMotion provides rememberReduceMotion(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = PinBomShapes,
        ) {
            PinBomSystemBars(darkTheme = darkTheme)
            content()
        }
    }
}

/** Accessor for the extra tokens: `PinBomTheme.colors.surfaceGrey`. */
object PinBomTheme {
    val colors: PinBomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPinBomColors.current

    /**
     * The navigation bar's palette, resolved from the theme around it. The bar
     * and the rail read this, never a constant, so the pill is charcoal on dark
     * and a white pill with a hairline on light.
     */
    val navigationBar: NavigationBarColors
        @Composable
        @ReadOnlyComposable
        get() = navigationBarColors(LocalPinBomColors.current.isDark)
}

/**
 * The root every screen is drawn on: the theme's own canvas, the theme's own
 * default text colour.
 *
 * This is the fix for “black text on a black screen”. The in-app theme is
 * independent of the system theme, and the window behind Compose follows the
 * *system* one (res/values-night), so any composable without a background of its
 * own used to show the window through it — while `LocalContentColor`, which a
 * `Text` without an explicit colour reads, stayed black outside a `Surface`.
 * Wrapping the whole tree in one surface removes both halves of that bug at
 * once: the background is themed, and the default content colour is themed.
 *
 * It must be called inside [PinBomTheme].
 */
@Composable
fun PinBomRoot(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PinBomTheme.colors.background,
        contentColor = PinBomTheme.colors.textPrimary,
        content = content,
    )
}

/**
 * Transparent, edge-to-edge system bars whose icon colour follows the in-app
 * theme. Called automatically by [PinBomTheme]; screens never touch this.
 */
@Composable
fun PinBomSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return

    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        // Icons must contrast with our background, not with the system theme.
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Kill the platform's automatic scrim so the bars stay truly transparent.
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
}
