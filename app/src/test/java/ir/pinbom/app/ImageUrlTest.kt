package ir.pinbom.app

import ir.pinbom.app.ui.components.imageUrl
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Image references, in every shape the API can return them.
 *
 * The bug this guards: the server sends "/media/2/2026/10/<hash>.jpg" and a bare
 * "2/2026/10/<hash>.jpg" must both resolve to the same fetchable URL. Appending
 * a bare path to the base URL yields "/2/2026/10/...", which the media route does
 * not serve — the image silently 404s.
 */
class ImageUrlTest {

    private val host = "http://192.168.1.10:8080"

    @Test
    fun `routed path is appended as it is`() {
        assertEquals("$host/media/pins/1/474.jpg", imageUrl(host, "/media/pins/1/474.jpg"))
    }

    @Test
    fun `bare storage path gains the media prefix`() {
        assertEquals("$host/media/2/2026/10/67ec.jpg", imageUrl(host, "2/2026/10/67ec.jpg"))
    }

    @Test
    fun `a trailing slash on the server never doubles up`() {
        assertEquals("$host/media/2/2026/10/67ec.jpg", imageUrl("$host/", "2/2026/10/67ec.jpg"))
        assertEquals("$host/media/2/2026/10/67ec.jpg", imageUrl("$host///", "/media/2/2026/10/67ec.jpg"))
    }

    @Test
    fun `absolute urls pass through untouched`() {
        assertEquals("https://example.test/a.jpg", imageUrl(host, "https://example.test/a.jpg"))
        assertEquals("http://cdn.test/a.jpg", imageUrl(host, "http://cdn.test/a.jpg"))
    }

    @Test
    fun `a stored media path is not prefixed twice`() {
        assertEquals("$host/media/2/2026/10/67ec.jpg", imageUrl(host, "media/2/2026/10/67ec.jpg"))
    }

    @Test
    fun `whitespace and empty references are harmless`() {
        assertEquals("$host/media/2/2026/10/67ec.jpg", imageUrl(host, "  2/2026/10/67ec.jpg  "))
        assertEquals("", imageUrl(host, ""))
        assertEquals("", imageUrl(host, "   "))
    }

    @Test
    fun `an unconfigured server leaves the reference alone`() {
        assertEquals("/media/2/2026/10/67ec.jpg", imageUrl("", "/media/2/2026/10/67ec.jpg"))
    }

    @Test
    fun `the emulator and a lan device both work`() {
        assertEquals("http://10.0.2.2:8080/media/2/2026/10/x.jpg", imageUrl("http://10.0.2.2:8080", "2/2026/10/x.jpg"))
        assertEquals("http://192.168.1.10:8080/media/2/2026/10/x.jpg", imageUrl("http://192.168.1.10:8080", "2/2026/10/x.jpg"))
    }
}
