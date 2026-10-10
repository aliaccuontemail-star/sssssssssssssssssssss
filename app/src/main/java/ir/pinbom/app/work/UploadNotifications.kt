package ir.pinbom.app.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import ir.pinbom.app.MainActivity
import ir.pinbom.app.R
import ir.pinbom.app.ui.theme.AppLanguage
import ir.pinbom.app.ui.theme.faDigits

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · upload notifications
 *  One channel, two ids. While a pin uploads, the same notification id carries
 *  the ongoing foreground notification (determinate bar, same percentage the
 *  in-app card shows); when the work ends, a short summary replaces it.
 *
 *  The summary deliberately lives on its own id: WorkManager cancels the
 *  foreground notification by id the moment the work leaves the foreground, so
 *  a summary posted on the *same* id would be wiped a millisecond after it
 *  appeared.
 *
 *  Every string is resolved through the context it is handed, which the worker
 *  builds for the language the user picked (see UploadWorker.localizedContext) —
 *  so the notification speaks the same language as the app.
 * ──────────────────────────────────────────────────────────────────────────── */

object UploadNotifications {

    /** Ongoing upload — the foreground notification. */
    const val ID = 0x8101

    /** Terminal summary: survives WorkManager clearing [ID]. */
    const val ID_SUMMARY = 0x8102

    /** The channel the app used before the rename; deleted so Settings shows one. */
    private const val LEGACY_CHANNEL_ID = "bumo_uploads"

    private const val CHANNEL_ID = "pinbom_uploads"

    /**
     * Progress notifications are only startable from a foreground service, and
     * the type has to be the one the manifest declares for it (dataSync).
     */
    private val SERVICE_TYPE_DATA_SYNC = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC

    private fun channel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        // One-time cleanup: without this an upgraded install would keep both a
        // "PinBom uploads" channel and the stale pre-rename one.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.upload_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.upload_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun tap(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The notification the worker runs under: ongoing, determinate, quiet. */
    fun foregroundNotification(context: Context, percent: Int, stage: UploadStage): Notification {
        channel(context)
        val label = context.getString(stage.labelRes())
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.upload_notification_title))
            .setContentText(label)
            .setSubText(withPercent(context, label, percent))
            .setContentIntent(tap(context))
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun foreground(context: Context, percent: Int, stage: UploadStage): ForegroundInfo =
        ForegroundInfo(ID, foregroundNotification(context, percent, stage), SERVICE_TYPE_DATA_SYNC)

    /** Re-paint the ongoing notification in place — cheaper than re-entering the service. */
    fun update(context: Context, percent: Int, stage: UploadStage) {
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(ID, foregroundNotification(context, percent, stage))
        }
    }

    /** Terminal state: a plain, alerting-once summary that the upload is over. */
    fun finish(context: Context, success: Boolean, text: String) {
        channel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.getString(
                    if (success) R.string.upload_notification_done else R.string.upload_stage_failed
                ),
            )
            .setContentText(text)
            .setContentIntent(tap(context))
            .setAutoCancel(true)
            .setOngoing(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(ID_SUMMARY, notification)
        }
    }

    /** Drops both the ongoing notification and its summary (logout, cancel). */
    fun clear(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).apply {
                cancel(ID)
                cancel(ID_SUMMARY)
            }
        }
    }

    /**
     * "در حال آپلود… · ۴۲٪" — the digits are localised before they reach the
     * format string, so Persian never shows Latin numerals.
     */
    private fun withPercent(context: Context, label: String, percent: Int): String =
        context.getString(R.string.upload_notification_percent, label, context.digits(percent))

    private fun Context.digits(value: Int): String =
        if (resources.configuration.locales[0].language == AppLanguage.Persian.tag) {
            faDigits(value.toString())
        } else {
            value.toString()
        }
}
