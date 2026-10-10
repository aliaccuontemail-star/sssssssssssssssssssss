package ir.pinbom.app.ui.theme

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ir.pinbom.app.R
import java.util.Locale

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · bilingual core (English = LTR, Persian = RTL)
 *
 *  One source of truth for “which language is the app in”:
 *    • `LanguageStore`      — the user's choice in DataStore (survives restarts)
 *    • `AppLanguage`        — the two supported languages and their direction
 *    • `AppLanguageState`   — the live state MainActivity owns
 *    • `PinBomTheme`          — provides LocalResources/LocalConfiguration for the
 *                             selected locale, so `stringResource` is correct
 *                             everywhere, and the layout direction follows the
 *                             language. `LocalContext` stays the Activity, because
 *                             Hilt and the activity owners require it.
 *
 *  The language flip never recreates the activity (`configChanges="locale|layoutDirection"`),
 *  so the back stack, scroll positions and form input all survive the switch.
 *  On Android 13+ the system's per-app language is kept in sync, which is what
 *  makes notifications and other system surfaces use the same language.
 * ──────────────────────────────────────────────────────────────────────────── */

enum class AppLanguage(val tag: String) {
    Persian("fa"),
    English("en");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    /** Persian reads right-to-left; English left-to-right. */
    val isRtl: Boolean get() = this == Persian

    /** The language's own name, always written in its own language and script. */
    val nativeName: String get() = when (this) {
        Persian -> "فارسی"
        English -> "English"
    }

    /** The other one — the language a one-tap toggle would switch to. */
    val other: AppLanguage get() = if (this == Persian) English else Persian

    companion object {
        /** "fa-IR" / "fa_IR" → Persian; anything unknown → null. */
        fun ofTag(tag: String?): AppLanguage? {
            val language = tag?.substringBefore('-')?.substringBefore('_')?.lowercase()
            return entries.firstOrNull { it.tag == language }
        }

        /**
         * First launch only: follow the device when it is `fa` or `en`,
         * otherwise default to Persian. A stored choice always wins.
         */
        fun deviceDefault(configuration: Configuration): AppLanguage =
            ofTag(configuration.locales.get(0)?.language) ?: Persian
    }
}

/**
 * Keeps the system's per-app language (Android 13+) aligned with the in-app
 * choice, so notifications, the recents title and system dialogs follow it too.
 */
fun syncSystemLocale(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    runCatching {
        context.getSystemService(LocaleManager::class.java)
            ?.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
}

/** Stable holder handed to [PinBomTheme]; `select` applies a language immediately. */
@Stable
class AppLanguageState internal constructor(
    val language: AppLanguage,
    /** Configuration for [language] — this is what LocalConfiguration gets. */
    val configuration: Configuration,
    /**
     * A context whose resources are already resolved for [language]. Its
     * [resources] are what `LocalResources` gets, so `stringResource` — and any
     * `LocalResources.current.getString(...)` outside of Compose's resource
     * APIs — speaks the selected language. It is deliberately **not** provided
     * as `LocalContext`: Hilt's ViewModel factory and the activity owners
     * (activity-result, back-press, report-drawn) all resolve the Activity
     * through `LocalContext`, and a configuration context breaks them.
     */
    val context: Context,
) {
    /** Resources of [context] — the language-correct ones. */
    val resources: Resources get() = context.resources
    internal var onSelect: (AppLanguage) -> Unit = {}

    /** True while the app is in Persian. */
    val isPersian: Boolean get() = language == AppLanguage.Persian

    fun select(value: AppLanguage) {
        if (value != language) onSelect(value)
    }
}

/**
 * Builds the locale override for [language] on top of the current configuration
 * (density, orientation, font scale … are all kept).
 */
@Composable
fun rememberAppLanguageState(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
): AppLanguageState {
    val context = LocalContext.current
    val configuration = remember(language, context) {
        Configuration(context.resources.configuration).apply {
            setLocale(language.locale)
            setLayoutDirection(language.locale)
        }
    }
    val localizedContext = remember(configuration) {
        (context.applicationContext ?: context).createConfigurationContext(configuration)
    }
    val state = remember(language, localizedContext) {
        AppLanguageState(language, configuration, localizedContext)
    }
    val currentOnSelect by rememberUpdatedState(onSelect)
    SideEffect { state.onSelect = currentOnSelect }
    return state
}

/** The language the app is currently rendering in. */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.Persian }

/**
 * The live language state, provided by [PinBomTheme]. Screens that can switch the
 * language (the Settings row, the compact pill on the auth screens) read it and
 * call `select(...)`; `null` means “no theme above”, so the control stays inert.
 */
val LocalAppLanguageState = staticCompositionLocalOf<AppLanguageState?> { null }

/* ── Formatting ───────────────────────────────────────────────────────────── */

/**
 * Language-aware numbers and time. Persian gets Persian digits and Persian
 * separators (۱۲٬۴۵۰ · ۱٫۲ هزار · ۳ ساعت پیش); English gets Latin ones
 * (12,450 · 1.2K · 3h ago). The unit words come from `strings.xml`, so they
 * flip with the locale automatically — nothing is hard-coded.
 *
 * Server-provided text is never reformatted; this only formats numbers the app
 * itself computes (counts, times, dates).
 */
object Localizer {
    val language: AppLanguage
        @Composable @ReadOnlyComposable get() = LocalAppLanguage.current

    val isPersian: Boolean
        @Composable @ReadOnlyComposable get() = LocalAppLanguage.current == AppLanguage.Persian

    /** 12450 → "۱۲٬۴۵۰" / "12,450" */
    @Composable
    @ReadOnlyComposable
    fun number(value: Long): String = if (isPersian) faGroup(value) else enGroup(value)

    @Composable
    @ReadOnlyComposable
    fun number(value: Int): String = number(value.toLong())

    /** 1200 → "۱٫۲ هزار" / "1.2K" · 1_500_000 → "۱٫۵ میلیون" / "1.5M" */
    @Composable
    @ReadOnlyComposable
    fun compact(value: Long): String {
        val (scaled, unit) = magnitudeOf(value)
        val persian = isPersian
        val digits = localizeDecimal(scaledTenths(scaled), persian)
        val word = when (unit) {
            MagnitudeUnit.None -> return if (persian) faGroup(value) else enGroup(value)
            MagnitudeUnit.Thousand -> stringResource(R.string.unit_thousand)
            MagnitudeUnit.Million -> stringResource(R.string.unit_million)
            MagnitudeUnit.Billion -> stringResource(R.string.unit_billion)
        }
        /* Persian counts read as two words ("۱٫۲ هزار"); the Latin compact form
         * is glued to the suffix ("1.2K"). */
        return if (persian) "$digits $word" else digits + word
    }

    @Composable
    @ReadOnlyComposable
    fun compact(value: Int): String = compact(value.toLong())

    /** «۳ ساعت پیش» / "3 h ago" — wording from resources, digits from the language. */
    @Composable
    @ReadOnlyComposable
    fun relativeTime(iso: String?, now: Long = System.currentTimeMillis()): String =
        when (val relative = relativeTimeOf(iso, now)) {
            RelativeTime.Now -> stringResource(R.string.time_now)
            is RelativeTime.Minutes -> stringResource(R.string.time_minutes_ago, number(relative.value))
            is RelativeTime.Hours -> stringResource(R.string.time_hours_ago, number(relative.value))
            RelativeTime.Yesterday -> stringResource(R.string.time_yesterday)
            is RelativeTime.Days -> stringResource(R.string.time_days_ago, number(relative.value))
            is RelativeTime.Months -> stringResource(R.string.time_months_ago, number(relative.value))
            is RelativeTime.Years -> stringResource(R.string.time_years_ago, number(relative.value))
            RelativeTime.Unknown -> ""
        }
}
