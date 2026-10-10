package ir.pinbom.app

import ir.pinbom.app.data.remote.ServerProbe
import ir.pinbom.app.data.remote.resolveServerUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a request ends up once the user has chosen a server.
 *
 * Register and login were the calls that made this visible: on a fresh install the
 * very first request of the app is the one that has to land on the address typed
 * on the setup screen, and nothing about that address may be dropped on the way.
 */
class ServerAddressTest {

    /** What Retrofit addresses calls to before the real server is known. */
    private val placeholder = "http://127.0.0.1/v1/auth/register".toHttpUrl()

    @Test
    fun `packaged server is the fixed PinBom API endpoint`() {
        assertEquals("http://185.26.33.188:8080", BuildConfig.DEFAULT_BASE_URL.trimEnd('/'))
    }

    @Test
    fun `host, port and scheme come from the configured server`() {
        val resolved = resolveServerUrl("http://192.168.1.10:8080", placeholder)
        assertEquals("http://192.168.1.10:8080/v1/auth/register", resolved.toString())
    }

    @Test
    fun `https keeps its default port when the address has none`() {
        val resolved = resolveServerUrl("https://pins.example.com", placeholder)
        assertEquals("https://pins.example.com/v1/auth/register", resolved.toString())
    }

    @Test
    fun `a trailing slash or stray spaces make no difference`() {
        assertEquals(
            "http://192.168.1.10:8080/v1/auth/register",
            resolveServerUrl("  http://192.168.1.10:8080/  ", placeholder).toString(),
        )
    }

    @Test
    fun `a server behind a path prefix keeps that prefix`() {
        val resolved = resolveServerUrl("http://example.com/pinbom", placeholder)
        assertEquals("http://example.com/pinbom/v1/auth/register", resolved.toString())
    }

    @Test
    fun `query strings survive the rewrite`() {
        val feed = "http://127.0.0.1/v1/feed/home?limit=24&cursor=16.5%3A15".toHttpUrl()
        val resolved = resolveServerUrl("http://10.0.2.2:8080", feed)
        assertEquals("http://10.0.2.2:8080/v1/feed/home?limit=24&cursor=16.5%3A15", resolved.toString())
    }

    @Test
    fun `no server configured leaves the request alone`() {
        assertNull(resolveServerUrl(null, placeholder))
        assertNull(resolveServerUrl("", placeholder))
        assertNull(resolveServerUrl("   ", placeholder))
    }

    @Test
    fun `an address without a host is refused rather than guessed`() {
        assertNull(resolveServerUrl("http://", placeholder))
        assertNull(resolveServerUrl("not a url at all", placeholder))
    }

    @Test
    fun `loopback addresses are recognised, remote ones are not`() {
        assertTrue(ServerProbe.isLoopback("http://127.0.0.1:8080"))
        assertTrue(ServerProbe.isLoopback("http://localhost:8080"))
        // The emulator's alias for the host machine is *not* the device itself.
        assertFalse(ServerProbe.isLoopback("http://10.0.2.2:8080"))
        assertFalse(ServerProbe.isLoopback("http://192.168.1.10:8080"))
    }
}
