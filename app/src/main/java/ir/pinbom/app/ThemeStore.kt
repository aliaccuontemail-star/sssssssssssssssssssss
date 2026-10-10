package ir.pinbom.app

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ir.pinbom.app.ui.components.NavBarStyle
import ir.pinbom.app.ui.theme.AppLanguage
import kotlinx.coroutines.flow.first

/* The user preferences live in the same DataStore file so a switch never races
 * another: theme (dark/light), language (fa/en) and the navigation-bar style. */

private val Context.pinbomPrefs by preferencesDataStore("pinbom_preferences")

class ThemeStore(private val context: Context) {
    private val darkKey = booleanPreferencesKey("dark")

    /**
     * `null` means the user never picked a theme, which is the app's cue to
     * *follow the system* (see MainActivity). Only an explicit choice in
     * Settings is ever stored, so this stays null until then.
     */
    suspend fun read(): Boolean? = context.pinbomPrefs.data.first()[darkKey]

    suspend fun set(v: Boolean) {
        context.pinbomPrefs.edit { it[darkKey] = v }
    }
}

/**
 * The user's language choice. `null` means “never picked one”, which is when the
 * app follows the device language (fa/en) and otherwise falls back to Persian.
 * Once the user picks in Settings, that choice always wins.
 */
class LanguageStore(private val context: Context) {
    private val languageKey = stringPreferencesKey("language")

    suspend fun read(): AppLanguage? =
        AppLanguage.ofTag(context.pinbomPrefs.data.first()[languageKey])

    suspend fun set(language: AppLanguage) {
        context.pinbomPrefs.edit { it[languageKey] = language.tag }
    }
}

/** Persisted user-selected button accent. Invalid stored data safely falls back to the brand default. */
class AccentColorStore(private val context: Context) {
    private val accentKey = stringPreferencesKey("button_accent")

    suspend fun read(): Color? {
        val raw = context.pinbomPrefs.data.first()[accentKey] ?: return null
        return runCatching { Color(AndroidColor.parseColor(raw)) }.getOrNull()
    }

    suspend fun set(color: Color) {
        context.pinbomPrefs.edit {
            it[accentKey] = String.format(java.util.Locale.US, "#%08X", color.toArgb())
        }
    }
}

/** Existing installs default to complete; a successful new registration opts into setup. */
class OnboardingStore(private val context: Context) {
    private val completeKey = booleanPreferencesKey("initial_setup_complete")

    suspend fun isComplete(): Boolean = context.pinbomPrefs.data.first()[completeKey] ?: true

    suspend fun setComplete(complete: Boolean) {
        context.pinbomPrefs.edit { it[completeKey] = complete }
    }
}

/** The navigation-bar style persists beside theme, language, and button accent. */
class NavBarStyleStore(private val context: Context) {
    private val styleKey = stringPreferencesKey("nav_bar_style")

    suspend fun read(): NavBarStyle =
        NavBarStyle.ofTag(context.pinbomPrefs.data.first()[styleKey])

    suspend fun set(style: NavBarStyle) {
        context.pinbomPrefs.edit { it[styleKey] = style.name }
    }
}
