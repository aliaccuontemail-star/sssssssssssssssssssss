package ir.pinbom.app.work

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · the picker's image, made uploadable
 *
 *  What a phone gallery hands us is not what an upload endpoint wants: a 50 MP
 *  HEIC with a rotation flag, a PNG with an alpha channel, an AVIF from a
 *  screenshot, a WebP from a download. This turns any of them into one thing —
 *  a baseline JPEG, upright, longest side at most [MAX_EDGE] pixels, quality
 *  [QUALITY], written into `cacheDir/uploads`.
 *
 *  Why in front of the queue and not inside the request: the worker sends a
 *  *file*, so the bytes on disk must already be the bytes that go on the wire.
 *  A file-backed body is re-readable (OkHttp replays it after a token refresh),
 *  streams from disk instead of copying the whole picture into memory, and
 *  carries the length the server needs for a `Content-Length` multipart. The
 *  decode also happens on the device that has the codecs for the format, once,
 *  rather than on every retry.
 *
 *  Orientation: `ImageDecoder` applies the EXIF rotation itself on API 28+; the
 *  older path rotates by hand from the framework's EXIF reader. Neither path
 *  upscales — a small picture stays a small picture.
 * ──────────────────────────────────────────────────────────────────────────── */

object ImagePreparer {

    /** Longest side of the uploaded JPEG, in pixels. */
    const val MAX_EDGE: Int = 2048

    /** The server's hard per-file limit (10 MiB); multipart overhead is separate. */
    const val MAX_UPLOAD_BYTES: Long = 10L * 1024L * 1024L

    /** Initial JPEG quality; reduced only when needed to stay under the upload cap. */
    const val QUALITY: Int = 88

    /** The one place that decides where prepared uploads live. */
    fun uploadDir(context: Context): File = File(context.cacheDir, DIR).apply { mkdirs() }

    /**
     * Decodes [uri], straightens it, fits it inside [MAX_EDGE] and re-encodes it
     * as JPEG at [dir]/[name]. Returns the file it wrote.
     *
     * Throws [IOException] — never returns null — so the caller can tell the user
     * “this picture cannot be read” instead of queueing work that must fail.
     * Blocking: call it off the main thread.
     */
    fun prepare(context: Context, uri: Uri, dir: File, name: String): File {
        val decoded = decode(context, uri)
        val upright = scale(decoded)
        val target = File(dir, name)
        dir.mkdirs()
        try {
            var quality = QUALITY
            do {
                target.outputStream().use { out ->
                    if (!upright.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                        throw IOException("JPEG encoder refused the bitmap")
                    }
                }
                if (target.length() <= MAX_UPLOAD_BYTES) break
                quality -= 8
            } while (quality >= 40)
            if (target.length() > MAX_UPLOAD_BYTES) {
                throw IOException("Image exceeds the 10 MiB upload limit after compression")
            }
        } catch (e: IOException) {
            runCatching { target.delete() }
            throw e
        } finally {
            if (upright !== decoded) runCatching { upright.recycle() }
            runCatching { decoded.recycle() }
        }
        return target
    }

    /** The file name that goes on the wire: our own, and always a real `.jpg`. */
    fun fileNameFor(id: String): String = "$id.jpg"

    /* ── decoding ─────────────────────────────────────────────────────────── */

    private fun decode(context: Context, uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri)
        } else {
            decodeWithBitmapFactory(context, uri)
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return try {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_EDGE) decoder.setTargetSampleSize(sampleSize(longest, MAX_EDGE))
                /* Software: the bitmap is re-encoded straight away, and a hardware
                 * bitmap cannot be read back by the JPEG encoder cheaply. */
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (e: IOException) {
            throw IOException("cannot decode $uri", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("cannot decode $uri", e)
        } catch (e: SecurityException) {
            throw IOException("no access to $uri", e)
        }
    }

    /**
     * API 24–27: bounds first, then a power-of-two subsample, then the EXIF
     * rotation by hand. HEIC and AVIF need API 28's decoder — on an older phone
     * those formats end up as the “cannot read this picture” message, which is
     * the honest answer.
     */
    private fun decodeWithBitmapFactory(context: Context, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("no stream for $uri")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("not an image: $uri")

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(maxOf(bounds.outWidth, bounds.outHeight), MAX_EDGE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("cannot decode $uri")

        val degrees = exifRotation(context, uri)
        if (degrees == 0) return raw
        return try {
            Bitmap.createBitmap(
                raw,
                0,
                0,
                raw.width,
                raw.height,
                Matrix().apply { postRotate(degrees.toFloat()) },
                true,
            ).also { if (it !== raw) raw.recycle() }
        } catch (e: OutOfMemoryError) {
            raw.recycle()
            throw IOException("out of memory rotating $uri", e)
        }
    }

    private fun exifRotation(context: Context, uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            /* The framework reader: present since API 5, and the one API-24
             * devices actually have (androidx.exifinterface would be a new
             * dependency for a path only two API levels take). */
            @Suppress("DEPRECATION")
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    /* ── geometry ─────────────────────────────────────────────────────────── */

    /** Never upscales: a picture already inside [MAX_EDGE] is returned as it is. */
    private fun scale(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val ratio = MAX_EDGE.toFloat() / longest
        val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /** Largest power of two that keeps the longest side at or above [target]. */
    private fun sampleSize(longest: Int, target: Int): Int {
        var sample = 1
        while (longest / (sample * 2) >= target) sample *= 2
        return sample
    }

    private const val DIR = "uploads"
}
