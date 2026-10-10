package ir.pinbom.app

import ir.pinbom.app.data.remote.ProgressRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * The picture's request body must put *every* byte on the wire.
 *
 * The version this replaces went out a few KB short on every upload (the tail of a
 * wrapper buffer was never flushed): the server received a body that stopped before
 * its closing boundary and answered 400. These tests pin the contract: all bytes,
 * the promised length, the closing boundary, an honest final progress tick, and a
 * replay that starts from zero.
 */
class ProgressRequestBodyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val jpeg = "image/jpeg".toMediaType()

    /** Sizes chosen so that none is a multiple of an 8 KB segment. */
    private val sizes = listOf(1, 100, 8191, 8193, 100_001, 1_234_567)

    private fun fileOf(size: Int): Pair<File, ByteArray> {
        val bytes = ByteArray(size) { (it * 31 + 7).toByte() }
        val file = tmp.newFile()
        file.writeBytes(bytes)
        return file to bytes
    }

    @Test
    fun `every byte reaches the sink`() {
        for (size in sizes) {
            val (file, bytes) = fileOf(size)
            val body = ProgressRequestBody(file, jpeg) { _, _ -> }
            val sink = Buffer()
            body.writeTo(sink)
            assertEquals("size $size", body.contentLength(), sink.size)
            assertArrayEquals("size $size", bytes, sink.readByteArray())
        }
    }

    @Test
    fun `a multipart request carries its promised length and its closing boundary`() {
        for (size in sizes) {
            val (file, _) = fileOf(size)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "x.jpg", ProgressRequestBody(file, jpeg) { _, _ -> })
                .addFormDataPart("title", "عنوان")
                .build()

            val sink = Buffer()
            multipart.writeTo(sink)
            val sent = sink.readByteArray()

            assertEquals("size $size", multipart.contentLength(), sent.size.toLong())
            val closing = "\r\n--${multipart.boundary}--\r\n".toByteArray()
            assertArrayEquals(
                "size $size",
                closing,
                sent.copyOfRange(sent.size - closing.size, sent.size),
            )
        }
    }

    @Test
    fun `progress ends exactly at the total and never goes backwards`() {
        val (file, _) = fileOf(1_234_567)
        val ticks = mutableListOf<Pair<Long, Long>>()
        ProgressRequestBody(file, jpeg) { written, total -> ticks += written to total }
            .writeTo(Buffer())

        assertTrue(ticks.isNotEmpty())
        assertEquals(1_234_567L to 1_234_567L, ticks.last())
        assertTrue(ticks.zipWithNext().all { (a, b) -> b.first >= a.first })
        assertTrue(ticks.all { it.first <= it.second })
    }

    @Test
    fun `a replay starts again from zero and sends the same bytes`() {
        val (file, bytes) = fileOf(100_001)
        val ticks = mutableListOf<Long>()
        val body = ProgressRequestBody(file, jpeg) { written, _ -> ticks += written }

        val first = Buffer().also { body.writeTo(it) }.readByteArray()
        ticks.clear()
        val second = Buffer().also { body.writeTo(it) }.readByteArray()

        assertArrayEquals(bytes, first)
        assertArrayEquals(bytes, second)
        assertTrue("the second pass must not start at 100 %", ticks.first() < 100_001L)
        assertEquals(false, body.isOneShot())
    }

    @Test
    fun `a file that shrinks mid-flight fails loudly instead of sending a short body`() {
        val (file, _) = fileOf(100_001)
        val body = ProgressRequestBody(file, jpeg) { _, _ -> }
        file.writeBytes(ByteArray(1_000))

        try {
            body.writeTo(Buffer())
            fail("a short file must not be sent as if it were complete")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("1000 of 100001"))
        }
    }
}
