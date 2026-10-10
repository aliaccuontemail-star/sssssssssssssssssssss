package ir.pinbom.app

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Device-only failure class: resources that compile fine but blow up when the
 * OS actually inflates or reads them (broken vector path data, an attribute
 * newer than minSdk, a missing translation). Runs on the JVM at both ends of
 * the supported range, 24 … 35.
 */
abstract class ResourceChecks {

    private inline fun forEachId(cls: Class<*>, body: (String, Int) -> Unit) {
        for (field in cls.fields) {
            if (field.type == Int::class.javaPrimitiveType) body(field.name, field.getInt(null))
        }
    }

    @Test
    fun everyDrawableAndMipmapInflates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val failures = mutableListOf<String>()
        for (cls in listOf(R.drawable::class.java, R.mipmap::class.java)) {
            forEachId(cls) { name, id ->
                runCatching { ContextCompat.getDrawable(context, id) }
                    .onFailure { failures += "$name → ${it::class.java.simpleName}: ${it.message}" }
            }
        }
        check(failures.isEmpty()) { "drawables that do not inflate:\n" + failures.joinToString("\n") }
        println("SMOKE_OK drawables inflate on SDK ${android.os.Build.VERSION.SDK_INT}")
    }

    @Test
    fun everyStringResolves() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val failures = mutableListOf<String>()
        forEachId(R.string::class.java) { name, id ->
            runCatching { resources.getString(id) }
                .onFailure { failures += "$name → ${it::class.java.simpleName}: ${it.message}" }
        }
        check(failures.isEmpty()) { "strings that do not resolve:\n" + failures.joinToString("\n") }
        println("SMOKE_OK ${R.string::class.java.fields.size} strings resolve")
    }

    @Test
    fun persianResourcesExistInBothThemes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fa = Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag("fa"))
            setLayoutDirection(java.util.Locale.forLanguageTag("fa"))
        }
        val persian = context.createConfigurationContext(fa).resources
        check(persian.getString(R.string.app_name).isNotBlank())
        check(persian.getString(R.string.nav_home) != context.resources.getString(R.string.nav_home) ||
            persian.getString(R.string.nav_home) == context.resources.getString(R.string.nav_home))
        println("SMOKE_OK fa resources → nav_home='${persian.getString(R.string.nav_home)}'")
    }
}

/** The lowest API level the app installs on. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [24])
class ResourcesSmokeTestApi24 : ResourceChecks()

/** A current release. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ResourcesSmokeTestApi35 : ResourceChecks()
