package ir.pinbom.app

import android.content.Context
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.ui.theme.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.annotation.Config

/**
 * Launch guard for the real [MainActivity].
 *
 * These are the paths that have to survive a cold start on a device: the auth
 * screen, the screens that ask Hilt for a ViewModel, the pin picker's
 * activity-result launcher and the live language switch. Every one of them used
 * to die on launch (see CRASHFIX-NOTES.md), so they run here on the JVM in
 * seconds instead of waiting for a phone.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class StartupSmokeTest {

    /** Seeds (or clears) the session *before* the activity launches. */
    class Seed(private val signedIn: Boolean, private val language: AppLanguage) : TestRule {
        override fun apply(base: Statement, description: Description): Statement =
            object : Statement() {
                override fun evaluate() {
                    val context = ApplicationProvider.getApplicationContext<Context>()
                    runBlocking {
                        val session = SessionManager(context)
                        if (signedIn) {
                            session.setServer("http://127.0.0.1:9/")
                            session.setTokens("header.payload.signature", "refresh")
                        }
                        LanguageStore(context).set(language)
                    }
                    base.evaluate()
                }
            }
    }

    @get:Rule(order = 0)
    val seed = Seed(signedIn = true, language = AppLanguage.Persian)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    /** The bottom bar is icon-only, so its labels live in content descriptions. */
    private fun onScreen(candidate: String): Boolean {
        val asText = runCatching {
            compose.onAllNodesWithText(candidate, substring = true).fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        if (asText.isNotEmpty()) return true
        val asLabel = runCatching {
            compose.onAllNodesWithContentDescription(candidate, substring = true).fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        return asLabel.isNotEmpty()
    }

    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(1_200)
        compose.waitForIdle()
    }

    /** What is on screen right now — for readable failure messages. */
    private fun visible() = runCatching {
        val dump = compose.onRoot(useUnmergedTree = true).printToString(maxDepth = 40)
        Regex("(?:Text|ContentDescription|EditableText) = '\\[?([^']*?)\\]?'")
            .findAll(dump).map { it.groupValues[1] }.filter { it.isNotBlank() }
            .toList().joinToString(" · ")
    }.getOrDefault("<no tree>")

    private fun assertAny(tag: String, vararg candidates: String): String {
        settle()
        val found = candidates.filter { onScreen(it) }
        check(found.isNotEmpty()) {
            "$tag: none of ${candidates.toList()} is on screen; saw: ${visible().take(900)}"
        }
        println("SMOKE_OK $tag → ${found.joinToString()}")
        return found.first()
    }

    private fun clickAny(vararg candidates: String) {
        settle()
        for (candidate in candidates) {
            for (click in listOf<(String) -> Unit>(
                { compose.onAllNodesWithText(it)[0].performClick() },
                { compose.onAllNodesWithContentDescription(it)[0].performClick() },
            )) {
                if (runCatching { click(candidate) }.isSuccess) return
            }
        }
        error("nothing to click among ${candidates.toList()}; saw: ${visible().take(900)}")
    }

    @Test
    fun signedInHome() {
        assertAny("home", "خانه", "Home")
    }

    @Test
    fun openCreateScreen() {
        assertAny("home", "خانه", "Home")
        clickAny("ساخت", "Create")
        assertAny("create", "ساخت پین", "انتخاب تصویر", "آپلود", "Create pin", "Choose image")
    }

    @Test
    fun openNotificationsAndProfile() {
        assertAny("home", "خانه", "Home")
        clickAny("اعلان‌ها", "اطلاع", "Notifications")
        assertAny("notifications", "اعلان", "Notifications")
    }

    /** The settings sheets live in a LazyColumn: scroll them into view first. */
    private fun scrollToLanguageRow() {
        settle()
        runCatching {
            compose.onAllNodes(hasScrollAction())[0]
                .performScrollToNode(hasText("زبان", substring = true))
        }.onFailure {
            runCatching {
                compose.onAllNodes(hasScrollAction())[0]
                    .performScrollToNode(hasText("Language", substring = true))
            }
        }
        settle()
    }

    @Test
    fun openSettings() {
        assertAny("home", "خانه", "Home")
        clickAny("تنظیمات", "Settings")
        assertAny("settings", "پروفایل", "تم تاریک", "Profile", "Dark theme")
        scrollToLanguageRow()
        assertAny("languageRow", "زبان", "Language")
    }

    @Test
    fun switchingLanguageKeepsTheScreen() {
        assertAny("home", "خانه", "Home")
        clickAny("تنظیمات", "Settings")
        scrollToLanguageRow()
        assertAny("languageRow", "زبان", "Language")
        clickAny("زبان", "Language")
        assertAny("languageSheet", "فارسی", "English")
        clickAny("English")
        // Same screen, new language: the switch must not recreate or lose it.
        assertAny("switched", "Language", "زبان", "Settings", "تنظیمات")
    }
}

/** Cold start with no account: the very first screen a new install shows. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FirstRunSmokeTest {

    @get:Rule(order = 0)
    val seed = StartupSmokeTest.Seed(signedIn = false, language = AppLanguage.Persian)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstallShowsTheAuthScreenInPersian() {
        compose.waitForIdle()
        Thread.sleep(1_500)
        compose.waitForIdle()
        val dump = runCatching {
            compose.onRoot(useUnmergedTree = true).printToString(maxDepth = 40)
        }.getOrDefault("")
        val texts = Regex("(?:Text|EditableText) = '\\[?([^']*?)\\]?'")
            .findAll(dump).map { it.groupValues[1] }.toList()
        check(texts.any { it.any { ch -> ch in 'ا'..'ی' } }) {
            "auth screen: no Persian text on screen; saw: ${texts.joinToString(" · ").take(900)}"
        }
        println("SMOKE_OK firstRun → ${texts.joinToString(" · ").take(400)}")
    }
}
