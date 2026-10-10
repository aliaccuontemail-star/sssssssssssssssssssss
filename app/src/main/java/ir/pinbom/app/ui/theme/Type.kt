package ir.pinbom.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.pinbom.app.R

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · typography
 *  Two families, one scale:
 *    • Vazirmatn (SIL OFL 1.1) for Persian — tall x-height, even rhythm, the
 *      generous vertical metrics a Persian UI needs.
 *    • Inter (SIL OFL 1.1) for English — a geometric grotesque that does not
 *      look like the platform's default Roboto.
 *  Both languages use the exact same sizes and weights; only the family (and
 *  the script's own shaping) changes. Latin text inside the Persian UI stays on
 *  Vazirmatn (its Latin is part of the family), and Persian text inside the
 *  English UI falls back to the platform's Arabic-capable font automatically.
 * ──────────────────────────────────────────────────────────────────────────── */

/** Vazirmatn — the Persian workhorse. */
val PinBomFontFamily: FontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.vazirmatn_black, FontWeight.Black, FontStyle.Normal),
)

/** Inter — the Latin family for the English UI. */
val PinBomLatinFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.inter_black, FontWeight.Black, FontStyle.Normal),
)

/** Platform default family — used for signage glyphs and as an escape hatch. */
val PinBomFontFamilyFallback: FontFamily = FontFamily.Default

/** The family that carries the app's voice in [language]. */
fun pinbomFontFamily(language: AppLanguage): FontFamily = when (language) {
    AppLanguage.Persian -> PinBomFontFamily
    AppLanguage.English -> PinBomLatinFontFamily
}

private fun pinbomText(
    family: FontFamily,
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/* The ten styles of the PinBom scale. Everything else is mapped onto them. */
data class PinBomTextStyles(
    /** 32/40 Bold — auth hero, big empty-state titles. */
    val display: TextStyle,
    /** 28/36 Bold — profile name, screen titles. */
    val headline: TextStyle,
    /** 22/30 Bold — pin title inside the detail screen. */
    val titleLarge: TextStyle,
    /** 18/26 SemiBold — section titles. */
    val title: TextStyle,
    /** 14/20 SemiBold — grid pin titles under the image. */
    val pinTitle: TextStyle,
    /** 16/26 Regular — descriptions, comments; Persian needs the air. */
    val body: TextStyle,
    /** 14/22 Regular — dense body copy. */
    val bodySmall: TextStyle,
    /** 12/18 Medium — author, relative time, counts. */
    val caption: TextStyle,
    /** 11/16 Medium — smallest legal caption (badges). */
    val micro: TextStyle,
    /** 15/20 Bold — every pill button label. */
    val button: TextStyle,
)

/** Builds the ten styles for one family. Sizes are language-independent. */
fun pinbomTextStyles(language: AppLanguage): PinBomTextStyles {
    val family = pinbomFontFamily(language)
    return PinBomTextStyles(
        display = pinbomText(family, 32, 40, FontWeight.Bold, -0.2),
        headline = pinbomText(family, 28, 36, FontWeight.Bold, -0.2),
        titleLarge = pinbomText(family, 22, 30, FontWeight.Bold, -0.1),
        title = pinbomText(family, 18, 26, FontWeight.SemiBold),
        pinTitle = pinbomText(family, 14, 20, FontWeight.SemiBold),
        body = pinbomText(family, 16, 26, FontWeight.Normal),
        bodySmall = pinbomText(family, 14, 22, FontWeight.Normal),
        caption = pinbomText(family, 12, 18, FontWeight.Medium),
        micro = pinbomText(family, 11, 16, FontWeight.Medium),
        button = pinbomText(family, 15, 20, FontWeight.Bold),
    )
}

/**
 * The style set the whole app reads through. Provided by [PinBomTheme] for the
 * selected language; the default keeps Persian so previews and any isolated
 * usage still render the product's voice.
 */
val LocalPinBomTextStyles = staticCompositionLocalOf { pinbomTextStyles(AppLanguage.Persian) }

/** Access to the nine styles of the scale, in the current language's family. */
object PinBomText {
    val display: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.display
    val headline: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.headline
    val titleLarge: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.titleLarge
    val title: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.title
    val pinTitle: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.pinTitle
    val body: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.body
    val bodySmall: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.bodySmall
    val caption: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.caption
    val micro: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.micro
    val button: TextStyle @Composable @ReadOnlyComposable get() = LocalPinBomTextStyles.current.button
}

/** Material components (buttons, fields, sheets) follow the same language. */
fun PinBomTypography(language: AppLanguage): Typography {
    val styles = pinbomTextStyles(language)
    val family = pinbomFontFamily(language)
    return Typography(
        displayLarge = pinbomText(family, 40, 48, FontWeight.Bold, -0.4),
        displayMedium = pinbomText(family, 36, 44, FontWeight.Bold, -0.3),
        displaySmall = styles.display,

        headlineLarge = pinbomText(family, 30, 38, FontWeight.Bold, -0.2),
        headlineMedium = styles.headline,
        headlineSmall = styles.titleLarge,

        titleLarge = styles.titleLarge,
        titleMedium = styles.title,
        titleSmall = styles.pinTitle,

        bodyLarge = styles.body,
        bodyMedium = styles.bodySmall,
        bodySmall = pinbomText(family, 13, 20, FontWeight.Normal),

        labelLarge = styles.button,
        labelMedium = styles.caption,
        labelSmall = styles.micro,
    )
}
