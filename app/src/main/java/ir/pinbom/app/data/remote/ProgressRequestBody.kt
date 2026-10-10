package ir.pinbom.app.data.remote

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.IOException

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · the upload body
 *
 *  A file-backed RequestBody that reports how many bytes it has handed to the
 *  socket. It is the one part of the multipart request that carries the picture.
 *
 *  Why it is written the way it is
 *  ───────────────────────────────
 *  The previous version wrapped the sink in a counting ForwardingSink and then
 *  wrapped *that* in a fresh buffer() before delegating. A buffer() only passes
 *  on whole segments; whatever is left in its last, partly filled segment stays
 *  there until somebody flush()es it, and nobody did. So every upload went out a
 *  few KB short of the Content-Length it had promised: OkHttp refused to finish
 *  the request ("unexpected end of stream"), the server saw a body that stopped
 *  before its closing boundary and answered 400, and the worker — treating an
 *  IOException as a flaky network — retried, which is the progress bar running to
 *  the end and dropping back to zero.
 *
 *  This version has no intermediate buffer at all. It reads the file in small
 *  chunks and writes each chunk straight into the sink OkHttp gave it, so there is
 *  nothing to flush and nothing that can be left behind. It also refuses to lie
 *  about length: the length is read once, up front, and the body writes exactly
 *  that many bytes or fails loudly.
 *
 *  Contract the rest of the app relies on
 *  ──────────────────────────────────────
 *    • contentLength() is the file's length and never changes for this instance,
 *      so the multipart request carries a real Content-Length;
 *    • isOneShot() is false and writeTo() reopens the file every time, so OkHttp
 *      can replay the body after a 401 → token refresh → retry;
 *    • progress is reported at most every ~50 ms, never for less than an 8 KB
 *      slice, immediately whenever the whole percent changes, and always once more
 *      at the very end, so a finished body is never stuck at 99.
 *    • the counters live inside writeTo(), so a replay starts again from zero.
 * ──────────────────────────────────────────────────────────────────────────── */

class ProgressRequestBody(
    private val file: File,
    private val mediaType: MediaType?,
    private val onProgress: (written: Long, total: Long) -> Unit,
) : RequestBody() {

    /** Fixed when the body is built: what the multipart header will promise. */
    private val length: Long = file.length()

    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = length

    override fun isOneShot(): Boolean = false

    override fun writeTo(sink: BufferedSink) {
        val total = length
        var written = 0L
        var lastReported = 0L
        var lastPercent = -1
        var lastAt = 0L

        file.source().use { source ->
            val chunk = Buffer()
            while (written < total) {
                val read = source.read(chunk, minOf(CHUNK, total - written))
                if (read == -1L) {
                    /* The file got shorter than it was when the length was taken.
                     * Sending less than promised can only end in a server-side
                     * 400, so say what actually happened instead. */
                    throw IOException("${file.name} ended after $written of $total bytes")
                }
                sink.write(chunk, read)
                written += read

                val percent = if (total > 0L) ((written * 100L) / total).toInt() else 0
                val fresh = written - lastReported >= SEGMENT
                val now = nowMillis()
                val due = lastAt == 0L || now - lastAt >= THROTTLE_MS
                val finished = written >= total

                if (finished || (fresh && (due || percent != lastPercent))) {
                    lastReported = written
                    lastPercent = percent
                    lastAt = now
                    onProgress(written, total)
                }
            }
        }
    }

    private fun nowMillis(): Long = System.nanoTime() / 1_000_000L

    private companion object {
        /** How much is read and written per step. */
        const val CHUNK = 8 * 1024L

        /** Smallest slice worth waking the UI for. */
        const val SEGMENT = 8 * 1024L

        /** Fastest we will ever report, in milliseconds. */
        const val THROTTLE_MS = 50L
    }
}
