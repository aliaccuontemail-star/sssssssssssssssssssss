package ir.pinbom.app.work

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.pinbom.app.LanguageStore
import ir.pinbom.app.R
import ir.pinbom.app.data.PinBomRepository
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.ui.theme.AppLanguage
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · آپلود پین
 *  Reads the same six input keys it always has (uri, title, description, tags,
 *  server, board_id) plus one new optional `file`: the cached copy the picker
 *  handed us, which is what actually gets uploaded. Runs as a foreground
 *  worker with a determinate notification, mirrors every tick into
 *  `setProgress` so the in-app card can follow it via WorkInfo, and classifies
 *  failures so a flaky network retries while a bad request fails at once.
 *
 *  A foreground notification the system refuses is not a failure: the worker logs
 *  it and carries on uploading in the background. Every failure the user is shown
 *  says what actually happened — the HTTP status and the server's own words, or
 *  the exception's class and message — and is also written to logcat under the
 *  `UploadWorker` tag with its full stack trace.
 *
 *  Notifications and failure text are resolved through a context built for the
 *  language the user picked, so background work never answers in the wrong one.
 * ──────────────────────────────────────────────────────────────────────────── */

/** Where each byte-counter lives in the worker's progress / output data. */
object UploadKeys {
    const val URI = "uri"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val TAGS = "tags"
    const val SERVER = "server"
    const val BOARD_ID = "board_id"

    /** New: absolute path of the cached copy inside cacheDir/uploads. */
    const val FILE = "file"

    /* Progress */
    const val PROGRESS = "progress"
    const val STAGE = "stage"
    const val WRITTEN = "written"
    const val TOTAL = "total"

    /* Output */
    const val ERROR = "error"
}

enum class UploadStage(val key: String) {
    Preparing("preparing"),
    Uploading("uploading"),
    Finishing("finishing"),
    Done("done"),
    Failed("failed"),
    Cancelled("cancelled"),
    ;

    companion object {
        fun of(key: String?): UploadStage = entries.firstOrNull { it.key == key } ?: Preparing
    }
}

/** The wording of a stage — one source of truth for the card and the notification. */
fun UploadStage.labelRes(): Int = when (this) {
    UploadStage.Preparing -> R.string.upload_stage_preparing
    UploadStage.Uploading -> R.string.upload_stage_uploading
    UploadStage.Finishing -> R.string.upload_stage_finishing
    UploadStage.Done -> R.string.upload_stage_done
    UploadStage.Failed -> R.string.upload_stage_failed
    UploadStage.Cancelled -> R.string.upload_stage_cancelled
}

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: PinBomRepository,
    private val session: SessionManager,
) : CoroutineWorker(appContext, params) {

    /**
     * Strings for the notification and every failure message. Set once in
     * [doWork]; falls back to the plain application context.
     */
    private var strings: Context = applicationContext

    override suspend fun getForegroundInfo(): ForegroundInfo =
        UploadNotifications.foreground(strings, 0, UploadStage.Preparing)

    override suspend fun doWork(): Result {
        strings = localizedContext()
        session.load()

        val title = inputData.getString(UploadKeys.TITLE).orEmpty()

        /* The prepared JPEG the screen queued is the thing that goes on the wire.
         * The plain uri is still honoured — a job queued by an older build can be
         * sitting in WorkManager when this version first runs — and gets exactly the
         * same treatment (decode, straighten, fit, re-encode) as it starts. */
        val cached = inputData.getString(UploadKeys.FILE)?.let(::File)?.takeIf { it.length() > 0L }
        val sourceUri = inputData.getString(UploadKeys.URI)?.takeIf { it.isNotBlank() }?.let(Uri::parse)

        /* The card is on screen from here on, even before a byte moves. */
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        publish(scope, 0, UploadStage.Preparing, 0L, cached?.length() ?: 0L)
        runCatching { setForeground(getForegroundInfo()) }
            .onFailure {
                /* The upload is the user's picture, the notification is a courtesy:
                 * if the system will not let us show it, keep uploading anyway. */
                Log.e(TAG, "could not enter the foreground · continuing in the background", it)
            }

        /* Said out loud rather than left to a connection error five retries deep. */
        if (session.snapshotServer().isBlank()) return fail(R.string.upload_error_no_server)
        if (session.accessToken.isNullOrBlank()) return fail(R.string.upload_error_unauthorized)

        val file = cached ?: prepare(sourceUri) ?: return fail(R.string.upload_error_unreadable)
        val total = file.length()

        return try {
            repo.upload(
                file,
                title,
                inputData.getString(UploadKeys.DESCRIPTION).orEmpty(),
                inputData.getString(UploadKeys.TAGS).orEmpty(),
                inputData.getString(UploadKeys.BOARD_ID).orEmpty(),
            ) { percent ->
                if (percent >= 99) {
                    /* Bytes are in: everything left is the server's own work. */
                    publish(scope, 99, UploadStage.Finishing, total, total)
                } else {
                    publish(scope, percent, UploadStage.Uploading, total * percent / 100L, total)
                }
            }

            runCatching { file.delete() }
            awaitProgress(100, UploadStage.Done, total, total)
            UploadNotifications.finish(
                strings,
                success = true,
                text = strings.getString(R.string.upload_notification_done),
            )
            Result.success(workDataOf(UploadKeys.PROGRESS to 100, UploadKeys.STAGE to UploadStage.Done.key))
        } catch (t: Throwable) {
            handleFailure(t, file)
        } finally {
            scope.cancel()
        }
    }

    /**
     * 4xx, [SecurityException] and an unreadable image are the user's or the
     * server's verdict: retrying would only fail again. Timeouts, I/O and 5xx/429
     * are the network's: they are worth [MAX_ATTEMPTS] tries.
     */
    private suspend fun handleFailure(t: Throwable, cached: File?): Result {
        val retryable = t is IOException || (t is HttpException && (t.code() >= 500 || t.code() == 429))

        /* The error body is a one-shot buffer: read it exactly once, then log it
         * and hand the very same text to the card. */
        val message = if (t is HttpException) {
            val body = serverText(runCatching { t.response()?.errorBody()?.string() }.getOrNull().orEmpty())
            /* The status and the server's own words, together: “HTTP 415” alone
             * tells the user nothing about which picture the server refused. */
            val said = "HTTP ${t.code()}" + if (body.isBlank()) "" else " · $body"
            Log.e(TAG, "upload failed · $said", t)
            said
        } else {
            val said = describe(t)
            Log.e(TAG, "upload failed · ${t::class.java.simpleName} · $said", t)
            said
        }

        val attemptsLeft = runAttemptCount < MAX_ATTEMPTS
        if (retryable && attemptsLeft) Log.w(TAG, "retrying · attempt ${runAttemptCount + 1} of $MAX_ATTEMPTS")
        return if (retryable && attemptsLeft) {
            awaitProgress(0, UploadStage.Preparing, 0L, 0L)
            Result.retry()
        } else {
            /* The cached copy stays put on failure so “Retry” can re-send it; the
             * card deletes it when the user dismisses the error. */
            awaitProgress(0, UploadStage.Failed, 0L, 0L, message)
            UploadNotifications.finish(strings, success = false, text = message)
            Result.failure(workDataOf(UploadKeys.ERROR to message))
        }
    }

    /**
     * What the client itself detected, said in the app's own language. Server
     * wording never passes through here — it is shown exactly as it arrived.
     */
    /**
     * What the client itself detected, said in the app's own language for the two
     * conditions worth wording (a locked gallery, a picture the decoder refused),
     * and otherwise as the exception class plus its own message — the honest,
     * specific version of “the upload failed”.
     */
    private fun describe(t: Throwable): String = when {
        t is SecurityException -> strings.getString(R.string.upload_error_unreadable)
        t is IllegalStateException -> strings.getString(R.string.upload_error_unreadable)
        else -> listOfNotNull(t::class.java.simpleName, t.message?.takeIf { it.isNotBlank() })
            .joinToString(": ")
    }

    /**
     * The sentence inside a server error body, when there is one.
     *
     * Servers answer a rejected upload either in prose (“unsupported image
     * format”) or in `{"detail": "…"}`. The braces are stripped, the sentence is
     * kept exactly as it arrived — never re-worded, never translated — and a
     * plain-text body is passed through untouched.
     */
    private fun serverText(body: String): String {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("{")) {
            val inner = runCatching {
                val obj = json.parseToJsonElement(trimmed).jsonObject
                listOf("detail", "message", "error")
                    .firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.contentOrNull }
            }.getOrNull()
            if (!inner.isNullOrBlank()) return inner.trim()
        }
        return trimmed.take(MAX_BODY_CHARS)
    }

    /**
     * The uri fallback: the same preparation the screen does before queueing, so a
     * job that predates it still sends a valid JPEG rather than raw bytes.
     */
    private fun prepare(uri: Uri?): File? {
        uri ?: return null
        return runCatching {
            ImagePreparer.prepare(
                context = applicationContext,
                uri = uri,
                dir = ImagePreparer.uploadDir(applicationContext),
                name = ImagePreparer.fileNameFor(id.toString()),
            )
        }
            .onFailure { Log.e(TAG, "could not prepare $uri", it) }
            .getOrNull()
    }

    private fun fail(reasonRes: Int): Result {
        val reason = strings.getString(reasonRes)
        Log.e(TAG, "upload rejected · $reason")
        UploadNotifications.finish(strings, success = false, text = reason)
        return Result.failure(workDataOf(UploadKeys.ERROR to reason))
    }

    /**
     * The application context with the user's language applied. Android 13+
     * already follows the per-app locale for notifications; this keeps older
     * releases — and everything the worker words itself — in step too.
     */
    private suspend fun localizedContext(): Context {
        val language = LanguageStore(applicationContext).read()
            ?: AppLanguage.deviceDefault(applicationContext.resources.configuration)
        val configuration = Configuration(applicationContext.resources.configuration).apply {
            setLocale(language.locale)
            setLayoutDirection(language.locale)
        }
        return applicationContext.createConfigurationContext(configuration)
    }

    /** Fire-and-forget tick: the notification repaints now, WorkInfo follows. */
    private fun publish(scope: CoroutineScope, percent: Int, stage: UploadStage, written: Long, total: Long) {
        UploadNotifications.update(strings, percent, stage)
        scope.launch {
            setProgress(
                workDataOf(
                    UploadKeys.PROGRESS to percent,
                    UploadKeys.STAGE to stage.key,
                    UploadKeys.WRITTEN to written,
                    UploadKeys.TOTAL to total,
                ),
            )
        }
    }

    /** Milestones are awaited so the last frame before `Result` is never skipped. */
    private suspend fun awaitProgress(
        percent: Int,
        stage: UploadStage,
        written: Long,
        total: Long,
        error: String? = null,
    ) {
        UploadNotifications.update(strings, percent, stage)
        val progress = Data.Builder()
            .putInt(UploadKeys.PROGRESS, percent)
            .putString(UploadKeys.STAGE, stage.key)
            .putLong(UploadKeys.WRITTEN, written)
            .putLong(UploadKeys.TOTAL, total)
        if (error != null) progress.putString(UploadKeys.ERROR, error)
        setProgress(progress.build())
    }

    private companion object {
        const val TAG = "UploadWorker"
        const val MAX_ATTEMPTS = 5

        /** Enough of a server's own wording to be useful, short enough for a card. */
        const val MAX_BODY_CHARS = 300

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }
}
