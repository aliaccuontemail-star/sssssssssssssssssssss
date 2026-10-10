package ir.pinbom.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · colour tokens
 *  Single source of truth. Screens must never declare raw Color(0x…) values;
 *  they read MaterialTheme.colorScheme or PinBomTheme.colors.
 * ──────────────────────────────────────────────────────────────────────────── */

// region Brand
val BrandRed = Color(0xFFE60023)            // primary · light
val BrandRedBright = Color(0xFFFF3B4E)      // primary · dark
val BrandRedPressed = Color(0xFFAD081B)     // primaryPressed · light
val BrandRedPressedDark = Color(0xFFD6283A) // primaryPressed · dark
val BrandRedDeep = Color(0xFFB3001B)        // gradient end
val BrandRedSoft = Color(0xFFFF5A6A)        // gradient start · soft accents
val BrandRedTint = Color(0x1AE60023)        // 10% red · unread rows, badges
// endregion

/* ── Navigation bar ──────────────────────────────────────────────────────────
 * The floating pill follows the theme: charcoal on a dark canvas, a light pill
 * with a hairline on a white one. Every colour the bar draws comes from one
 * object, so a style cannot pick up an untinted literal by accident — and a
 * theme flip crossfades the whole palette instead of cutting to it (see
 * `animatedBarColors` in Navigation.kt).
 *
 * It is deliberately not built from the theme's text ramp: the idle icon is a
 * muted grey-lavender, the active tile sits a step away from the fill, and the
 * accent is the brand red of the theme around it. */
@Immutable
data class NavigationBarColors(
    /** The bar's own surface. */
    val fill: Color,
    /** Idle icon tint. */
    val idle: Color,
    /** The tile behind the active icon (TILE) and the rail's travelling marker. */
    val indicator: Color,
    /** The soft, large, low-alpha shadow under the pill. */
    val shadow: Color,
    /** 1dp hairline: only the light pill needs an outline to read on white. */
    val hairline: Color,
    /** Active icon, glow and underline. */
    val accent: Color,
)

/** Light canvas: a white pill, muted grey icons, brand red for the active slot. */
val LightNavigationBarColors = NavigationBarColors(
    fill = Color(0xFFF4F4F6),
    idle = Color(0xFF8E8E99),
    indicator = Color(0xFFE9E9EE),
    shadow = Color(0x1F000000),
    hairline = Color(0xFFE9E9E9),
    accent = BrandRed,
)

/** Dark canvas: the charcoal version, with the brighter brand red. */
val DarkNavigationBarColors = NavigationBarColors(
    fill = Color(0xFF2C2C31),
    idle = Color(0xFF7C7A88),
    indicator = Color(0xFF4A4A55),
    shadow = Color(0x33000000),
    hairline = Color.Transparent,
    accent = BrandRedBright,
)

/** The bar palette for the theme around it. */
fun navigationBarColors(dark: Boolean): NavigationBarColors =
    if (dark) DarkNavigationBarColors else LightNavigationBarColors

/** Diagonal brand gradient used by the logo, splash and hero moments. */
val BrandGradientStops: List<Color> = listOf(BrandRedSoft, BrandRed, BrandRedDeep)
val BrandGradient: Brush = Brush.linearGradient(colors = BrandGradientStops)

/* Pin placeholder palette — deterministic by `pin.id % size`. The masonry grid
 * never flashes white: every tile paints a pastel until the photo arrives. */
val PinPastelsLight: List<Color> = listOf(
    Color(0xFFFDE2E4), Color(0xFFFAD2E1), Color(0xFFFFE5D9), Color(0xFFFCF6BD),
    Color(0xFFD0F4DE), Color(0xFFA9DEF9), Color(0xFFE4C1F9), Color(0xFFFDE4CF),
    Color(0xFFFFD7BA), Color(0xFFCDE7BE), Color(0xFFB8E0D2), Color(0xFFD6E2E9),
)

val PinPastelsDark: List<Color> = listOf(
    Color(0xFF3A2226), Color(0xFF38242C), Color(0xFF3B2A22), Color(0xFF373320),
    Color(0xFF1F3529), Color(0xFF1D2F3C), Color(0xFF2E2438), Color(0xFF392C22),
    Color(0xFF3A2A1F), Color(0xFF25301F), Color(0xFF1F3A33), Color(0xFF242E36),
)

val AvatarPalette: List<Color> = listOf(
    Color(0xFFE60023), Color(0xFFE8590C), Color(0xFFF08C00), Color(0xFF2F9E44),
    Color(0xFF0CA678), Color(0xFF1971C2), Color(0xFF4263EB), Color(0xFF7048E8),
    Color(0xFFAE3EC9), Color(0xFFC2255C), Color(0xFF495057), Color(0xFF087F5B),
)

/** Deterministic pastel for a pin id. */
fun pastelFor(id: Long, dark: Boolean = false): Color {
    val palette = if (dark) PinPastelsDark else PinPastelsLight
    val index = ((id % palette.size) + palette.size).toInt() % palette.size
    return palette[index]
}

/** Deterministic avatar colour for a username. */
fun avatarColorFor(key: String?): Color {
    if (key.isNullOrBlank()) return AvatarPalette.first()
    var hash = 7
    for (ch in key) hash = hash * 31 + ch.code
    return AvatarPalette[((hash % AvatarPalette.size) + AvatarPalette.size) % AvatarPalette.size]
}

/* ── Extra tokens Material 3 does not model (pills, hairlines, skrims) ────── */
@Immutable
data class PinBomColors(
    val primary: Color,
    val primaryPressed: Color,
    val background: Color,
    val surface: Color,
    val surfaceGrey: Color,
    val surfaceGreyPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val scrim: Color,
    val success: Color,
    val chipSelectedBg: Color,
    val chipSelectedText: Color,
    val skeleton: Color,
    val skeletonHighlight: Color,
    val pastels: List<Color>,
    val isDark: Boolean,
    /** Inverted surface: toasts and the offline banner (a dark chip on a light
     *  canvas, a grey one on a dark canvas). */
    val inverseSurface: Color = Color(0xFF2B2B2B),
    val onInverseSurface: Color = Color(0xFFF5F5F5),
    val onImage: Color = Color.White,
    val imageScrim: Color = Color(0x66000000),
)

val LightPinBomColors = PinBomColors(
    primary = BrandRed,
    primaryPressed = BrandRedPressed,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceGrey = Color(0xFFEFEFEF),
    surfaceGreyPressed = Color(0xFFE2E2E2),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5F5F5F),
    textTertiary = Color(0xFF8E8E8E),
    divider = Color(0xFFE9E9E9),
    scrim = Color(0x66000000),
    success = Color(0xFF0A8F4E),
    chipSelectedBg = Color(0xFF111111),
    chipSelectedText = Color(0xFFFFFFFF),
    skeleton = Color(0xFFEDEDED),
    skeletonHighlight = Color(0xFFF8F8F8),
    pastels = PinPastelsLight,
    isDark = false,
)

val DarkPinBomColors = PinBomColors(
    primary = BrandRedBright,
    primaryPressed = BrandRedPressedDark,
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    surfaceGrey = Color(0xFF262626),
    surfaceGreyPressed = Color(0xFF333333),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB3B3B3),
    textTertiary = Color(0xFF7A7A7A),
    divider = Color(0xFF2A2A2A),
    scrim = Color(0x99000000),
    success = Color(0xFF3DDC84),
    chipSelectedBg = Color(0xFFFFFFFF),
    chipSelectedText = Color(0xFF111111),
    skeleton = Color(0xFF232323),
    skeletonHighlight = Color(0xFF2E2E2E),
    pastels = PinPastelsDark,
    isDark = true,
    inverseSurface = Color(0xFF262626),
    onInverseSurface = Color(0xFFFFFFFF),
)

val LocalPinBomColors = staticCompositionLocalOf { LightPinBomColors }

/* ── Material 3 schemes, built from the tokens above ──────────────────────── */
internal val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADC),
    onPrimaryContainer = Color(0xFF410007),
    inversePrimary = BrandRedBright,
    secondary = LightPinBomColors.chipSelectedBg,
    onSecondary = Color.White,
    secondaryContainer = LightPinBomColors.surfaceGrey,
    onSecondaryContainer = LightPinBomColors.textPrimary,
    tertiary = LightPinBomColors.success,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8F0CE),
    onTertiaryContainer = Color(0xFF00210F),
    background = LightPinBomColors.background,
    onBackground = LightPinBomColors.textPrimary,
    surface = LightPinBomColors.surface,
    onSurface = LightPinBomColors.textPrimary,
    surfaceVariant = LightPinBomColors.surfaceGrey,
    onSurfaceVariant = LightPinBomColors.textSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF2B2B2B),
    inverseOnSurface = Color(0xFFF1F1F1),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = LightPinBomColors.divider,
    outlineVariant = Color(0xFFE9E9E9),
    scrim = LightPinBomColors.scrim,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFEDEDED),
    surfaceContainer = Color(0xFFF7F7F7),
    surfaceContainerHigh = Color(0xFFF1F1F1),
    surfaceContainerHighest = LightPinBomColors.surfaceGrey,
    surfaceContainerLow = Color(0xFFFBFBFB),
    surfaceContainerLowest = Color.White,
)

internal val DarkColorScheme = darkColorScheme(
    primary = BrandRedBright,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0A12),
    onPrimaryContainer = Color(0xFFFFDADC),
    inversePrimary = BrandRed,
    secondary = DarkPinBomColors.chipSelectedBg,
    onSecondary = DarkPinBomColors.chipSelectedText,
    secondaryContainer = DarkPinBomColors.surfaceGrey,
    onSecondaryContainer = DarkPinBomColors.textPrimary,
    tertiary = DarkPinBomColors.success,
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF00522F),
    onTertiaryContainer = Color(0xFFB8F0CE),
    background = DarkPinBomColors.background,
    onBackground = DarkPinBomColors.textPrimary,
    surface = DarkPinBomColors.surface,
    onSurface = DarkPinBomColors.textPrimary,
    surfaceVariant = DarkPinBomColors.surfaceGrey,
    onSurfaceVariant = DarkPinBomColors.textSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF1B1B1B),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF410002),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = DarkPinBomColors.divider,
    outlineVariant = Color(0xFF3A3A3A),
    scrim = DarkPinBomColors.scrim,
    surfaceBright = Color(0xFF333333),
    surfaceDim = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = DarkPinBomColors.surfaceGrey,
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainerLowest = Color(0xFF000000),
)
