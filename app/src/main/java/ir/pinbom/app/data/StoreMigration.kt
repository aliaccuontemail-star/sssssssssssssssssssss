package ir.pinbom.app.data

import android.content.Context
import java.io.File

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · on-disk store migration
 *
 *  The app's private files were named after the old brand. Their names are
 *  implementation detail, but their *contents* are the user's: the session, the
 *  theme and language choices, the offline cache. So the files are moved rather
 *  than abandoned — an install that updates keeps its session (nobody is signed
 *  out) and its cached feed.
 *
 *  Two rules keep this safe to run on every launch:
 *
 *   1. never overwrite — if the new name already exists, the old file is left
 *      alone and nothing is copied;
 *   2. never fail the launch — every step is best-effort. If a rename does not
 *      work, the app starts exactly as it would have without this file: it
 *      creates the new file empty, and the worst case is a fresh sign-in.
 * ──────────────────────────────────────────────────────────────────────────── */

internal object StoreMigration {

    private const val LEGACY_SESSION = "bumo_session"
    private const val LEGACY_PREFERENCES = "bumo_preferences"
    private const val LEGACY_DATABASE = "bumo-cache.db"

    private const val SESSION = "pinbom_session"
    private const val PREFERENCES = "pinbom_preferences"
    private const val DATABASE = "pinbom-cache.db"

    /** Older builds also wrote per-user theme files; the layout is stable now. */
    fun run(context: Context) {
        runCatching {
            renameDataStore(context, LEGACY_SESSION, SESSION)
            renameDataStore(context, LEGACY_PREFERENCES, PREFERENCES)
            renameDatabase(context, LEGACY_DATABASE, DATABASE)
        }
    }

    /** Preferences DataStore keeps one file per store, under files/datastore/. */
    private fun renameDataStore(context: Context, from: String, to: String) {
        val dir = File(context.filesDir, "datastore")
        move(File(dir, "$from.preferences_pb"), File(dir, "$to.preferences_pb"))
    }

    /** Room keeps the database plus, while WAL is on, its log and index files. */
    private fun renameDatabase(context: Context, from: String, to: String) {
        val source = context.getDatabasePath(from)
        val target = context.getDatabasePath(to)
        move(source, target)
        move(File(source.path + "-wal"), File(target.path + "-wal"))
        move(File(source.path + "-shm"), File(target.path + "-shm"))
    }

    private fun move(from: File, to: File) {
        if (!from.exists() || to.exists()) return
        to.parentFile?.mkdirs()
        from.renameTo(to)
    }
}
