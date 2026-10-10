package ir.pinbom.app

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import ir.pinbom.app.data.StoreMigration
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · application
 *
 *  Two responsibilities: hand WorkManager its Hilt worker factory (the upload
 *  worker is injected), and give Coil one image loader for the whole app.
 *
 *  The loader matters for feel, not for looks. Coil creates no disk cache by
 *  itself: with the default singleton, every picture was fetched again after
 *  each process start, so a cold feed paid for its images twice — once from the
 *  network, once in decode. Now decoded bitmaps live in a quarter of the heap
 *  and the encoded files in a bounded cache directory, and every request in the
 *  app inherits a 150ms crossfade instead of the 100ms default.
 * ──────────────────────────────────────────────────────────────────────────── */

@HiltAndroidApp
class PinBomApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Before anything can touch a store: the files this app used to write
        // carry the same data under the previous brand's names.
        StoreMigration.run(this)
        startedAt = SystemClock.uptimeMillis()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    /* A quarter of the process heap, counted against the real
                     * limit instead of a guessed megabyte figure. */
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(IMAGE_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(IMAGE_CACHE_BYTES)
                    .build()
            }
            .crossfade(CROSSFADE_MS)
            .build()

    companion object {
        /** Uptime at process start, for the debug-only startup log. */
        @Volatile
        var startedAt: Long = 0L
            private set

        const val IMAGE_CACHE_DIR = "image_cache"
        const val IMAGE_CACHE_BYTES = 64L * 1024L * 1024L
        const val CROSSFADE_MS = 150
    }
}
