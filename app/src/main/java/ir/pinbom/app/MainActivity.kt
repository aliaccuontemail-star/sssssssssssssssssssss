package ir.pinbom.app

import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ir.pinbom.app.data.PinBomRepository
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.ui.components.PinBomBootScreen
import ir.pinbom.app.ui.components.NavBarStyle
import ir.pinbom.app.ui.nav.AppNavGraph
import ir.pinbom.app.ui.screens.AuthScreen
import ir.pinbom.app.ui.screens.InitialSetupScreen
import ir.pinbom.app.ui.screens.ServerSetupScreen
import ir.pinbom.app.ui.theme.AppLanguage
import ir.pinbom.app.ui.theme.BrandRed
import androidx.compose.ui.graphics.Color
import ir.pinbom.app.ui.theme.PinBomRoot
import ir.pinbom.app.ui.theme.PinBomTheme
import ir.pinbom.app.ui.theme.Motion
import ir.pinbom.app.ui.theme.syncSystemLocale
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var session: SessionManager

    @Inject
    lateinit var repo: PinBomRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        /* The splash must be installed before super.onCreate() so the launch
         * theme is swapped to Theme.PinBom before the first frame is drawn. */
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        /* Brand exit: the glyph scales up a hair while fading out, so the splash
         * dissolves into the app instead of hard-cutting. */
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(240L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { provider.remove() }
                .start()
        }

        setContent {
            var boot by remember { mutableStateOf(false) }
            var serverReady by remember { mutableStateOf(false) }
            var loggedIn by remember { mutableStateOf(false) }
            /* Nothing stored yet? Then the app follows the phone: a dark phone
             * gets the dark theme, and there is no window the app disagrees with
             * before the user has said anything. An explicit choice in Settings
             * is stored and always wins. */
            val systemDark = remember {
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES
            }
            var dark by remember { mutableStateOf(systemDark) }
            var navStyle by remember { mutableStateOf(NavBarStyle.Default) }
            var language by remember { mutableStateOf<AppLanguage?>(null) }
            var accentColor by remember { mutableStateOf(BrandRed) }
            var needsInitialSetup by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            /* What the activity came up with: the device language, or the system's
             * per-app locale. Used until DataStore answers, so the first frame is
             * never in the wrong language. */
            val deviceLanguage = remember { AppLanguage.deviceDefault(resources.configuration) }

            LaunchedEffect(Unit) {
                /* Four independent reads share one wait: the theme, the language
                 * and the bar style are three separate files to open (all in the
                 * same DataStore), and the session is a fourth. Started together,
                 * the first frame waits for the slowest one instead of for their
                 * sum. None of them blocks the main thread while it runs. */
                val theme = async { ThemeStore(this@MainActivity).read() }
                val stored = async { LanguageStore(this@MainActivity).read() }
                val style = async { NavBarStyleStore(this@MainActivity).read() }
                val accent = async { AccentColorStore(this@MainActivity).read() }
                val setupComplete = async { OnboardingStore(this@MainActivity).isComplete() }
                session.load()
                // Server URL is shipped with the app; never show the server picker on
                // a fresh install or when upgrading from an older saved address.
                serverReady = true

                dark = theme.await() ?: systemDark
                navStyle = style.await()
                accentColor = accent.await() ?: BrandRed
                val resolved = stored.await() ?: deviceLanguage
                language = resolved
                syncSystemLocale(this@MainActivity, resolved)
                loggedIn = session.accessToken != null
                needsInitialSetup = loggedIn && !setupComplete.await()
                boot = true
            }

            val current = language ?: deviceLanguage

            /* Language changes are crossfaded through the app background: the veil
             * rises *before* the swap, so the new language is revealed by a fade —
             * no white flash, nothing recreated, the back stack stays put. */
            val veil = remember { Animatable(0f) }

            PinBomTheme(
                darkTheme = dark,
                language = current,
                accentColor = accentColor,
                onSelectLanguage = { picked ->
                    scope.launch {
                        veil.animateTo(0.55f, tween(durationMillis = 90, easing = Motion.standard))
                        language = picked
                        LanguageStore(this@MainActivity).set(picked)
                        syncSystemLocale(this@MainActivity, picked)
                        veil.animateTo(0f, tween(durationMillis = Motion.medium, easing = Motion.standard))
                    }
                },
            ) {
                val colors = PinBomTheme.colors

                /* One themed surface under the entire app — boot screen, server
                 * setup, auth and the shell alike. Screens without a background
                 * of their own are the norm here, and before this surface the
                 * window behind Compose (which follows the *system* theme while
                 * the app follows the user's choice) showed through them. Every
                 * Text without an explicit colour also now inherits the theme's
                 * text colour instead of the platform's black. */
                PinBomRoot {
                    Box(Modifier.fillMaxSize()) {
                        /* Startup instrumentation. `reportFullyDrawn()` is the system's
                         * own “the app is usable now” marker: `adb shell am start -W`
                         * prints it and the Play console reads it. The log line below
                         * is debug-only and prints a real measurement — process start
                         * to the shell's first composition — never an estimate. */
                        var fullyDrawn by remember { mutableStateOf(false) }

                        when {
                            // Never a blank white screen: the brand mark pops in while
                            // the session is read from DataStore.
                            !boot -> PinBomBootScreen()
                            !serverReady -> ServerSetupScreen(session) { serverReady = true }
                            !loggedIn -> AuthScreen(
                                onSuccess = { loggedIn = true },
                                onRegistered = {
                                    scope.launch {
                                        OnboardingStore(this@MainActivity).setComplete(false)
                                        needsInitialSetup = true
                                        loggedIn = true
                                    }
                                },
                            )
                            needsInitialSetup -> InitialSetupScreen(
                                dark = dark,
                                onDarkChange = { picked ->
                                    dark = picked
                                    scope.launch { ThemeStore(this@MainActivity).set(picked) }
                                },
                                accentColor = accentColor,
                                onAccentChange = { picked ->
                                    accentColor = picked
                                    scope.launch { AccentColorStore(this@MainActivity).set(picked) }
                                },
                                onLanguageChange = { picked ->
                                    language = picked
                                    scope.launch {
                                        LanguageStore(this@MainActivity).set(picked)
                                        syncSystemLocale(this@MainActivity, picked)
                                    }
                                },
                                onComplete = {
                                    scope.launch { OnboardingStore(this@MainActivity).setComplete(true) }
                                    needsInitialSetup = false
                                },
                            )
                            else -> {
                                LaunchedEffect(Unit) {
                                    if (!fullyDrawn) {
                                        fullyDrawn = true
                                        reportFullyDrawn()
                                        if (BuildConfig.DEBUG) {
                                            Log.d(
                                                "PinBomPerf",
                                                "shell composed " +
                                                    "${SystemClock.uptimeMillis() - PinBomApplication.startedAt}ms " +
                                                    "after process start",
                                            )
                                        }
                                    }
                                }

                                val nav = rememberNavController()
                                val deepLink = intent?.data
                                LaunchedEffect(deepLink) {
                                    // `bumo` is the scheme the first builds used; a
                                    // link shared before the rename still resolves.
                                    when (deepLink?.scheme) {
                                        "pinbom", "bumo" -> when (deepLink.host) {
                                            "pin" -> deepLink.pathSegments.firstOrNull()?.toLongOrNull()
                                                ?.let { nav.navigate("pin/$it") }

                                            "create" -> nav.navigate("create")
                                            "search" -> nav.navigate("search")
                                        }
                                    }
                                }
                                AppNavGraph(
                                    nav = nav,
                                    session = session,
                                    dark = dark,
                                    accentColor = accentColor,
                                    onAccentChange = { picked ->
                                        accentColor = picked
                                        scope.launch { AccentColorStore(this@MainActivity).set(picked) }
                                    },
                                    navStyle = navStyle,
                                    onNavStyleChange = { picked ->
                                        navStyle = picked
                                        scope.launch { NavBarStyleStore(this@MainActivity).set(picked) }
                                    },
                                    onDarkChange = {
                                        dark = it
                                        scope.launch { ThemeStore(this@MainActivity).set(it) }
                                    },
                                    onLogout = {
                                        scope.launch {
                                            repo.logout()
                                            loggedIn = false
                                        }
                                    }
                                )
                            }
                        }

                        /* The crossfade veil: painted in the app's own background, so
                         * even the fade itself is theme-correct. */
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = veil.value }
                                .background(colors.background),
                        )
                    }
                }
            }
        }
    }
}
