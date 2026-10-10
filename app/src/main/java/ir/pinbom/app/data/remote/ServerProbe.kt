package ir.pinbom.app.data.remote

import android.util.Log
import ir.pinbom.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · server probe
 *
 *  Why this exists: the setup screen used to accept any well-formed URL and hand
 *  it straight to the repository. A typo, a port that is not open, or — the one
 *  that actually bit — leaving the pre-filled `127.0.0.1` in place on a phone
 *  (where it means *the phone itself*) produced no message at all: setup said
 *  fine, and then every later call failed somewhere far from the cause.
 *
 *  So the address is now proven before it is saved: one `GET /health` against
 *  exactly the address the user typed, and the screen does not move on until the
 *  server answers. The probe is deliberately short-lived — an address that
 *  cannot answer in a few seconds is an address the user needs to fix, not one
 *  worth waiting on.
 * ──────────────────────────────────────────────────────────────────────────── */

object ServerProbe {

    private const val TAG = "PinBomNet"

    /** Loopback hosts: on a handset these point at the handset, not at a PC. */
    private val LOOPBACK = setOf("127.0.0.1", "localhost", "::1", "[::1]", "0.0.0.0")

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    sealed interface Result {
        /** The server answered; the address can be saved. */
        data object Reachable : Result

        /**
         * No usable answer. [reason] is the platform's own words ("Connection
         * refused", a timeout, a DNS failure…), which is more useful to the
         * person fixing the address than a phrase we invent.
         */
        data class Unreachable(val reason: String) : Result
    }

    suspend fun check(base: String): Result = withContext(Dispatchers.IO) {
        val root = base.trim().trimEnd('/')
        val request = try {
            Request.Builder().url("$root/health").get().build()
        } catch (e: Exception) {
            return@withContext Result.Unreachable(e.message ?: "unusable address")
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "GET $root/health")
        try {
            client.newCall(request).execute().use { response ->
                if (BuildConfig.DEBUG) Log.d(TAG, "GET $root/health → ${response.code}")
                if (response.isSuccessful) {
                    Result.Reachable
                } else {
                    Result.Unreachable("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d(TAG, "GET $root/health failed: ${e.message}")
            Result.Unreachable(describe(e))
        }
    }

    /** True when the address points back at this device. */
    fun isLoopback(base: String): Boolean = try {
        (URI(base.trim().trimEnd('/')).host ?: "").lowercase() in LOOPBACK
    } catch (_: Exception) {
        false
    }

    /** `UnknownHostException: …` reads as noise; these two lines do not. */
    private fun describe(e: Exception): String {
        val message = e.message.orEmpty().ifBlank { e::class.java.simpleName }
        return message.substringBefore('\n')
    }
}
