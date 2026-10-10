package ir.pinbom.app.data.remote

import okhttp3.HttpUrl
import java.net.URI

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · where a call is actually sent
 *
 *  Retrofit is built against a placeholder base URL and every request is
 *  re-addressed to the configured fixed server. This function is extracted from
 *  the interceptor so routing can be tested directly.
 *
 *  Rules, in full:
 *
 *   · no server configured        → no rewritten URL is returned;
 *   · server without a host       → no rewritten URL is returned;
 *   · otherwise                  → scheme, host and port come from the configured
 *                                  server and the request path/query are preserved.
 *
 *  The port matters most: leaving the pre-filled address in place on a phone
 *  pointed every call at the phone itself, and a server on a non-default port
 *  looked unreachable when the port was simply being dropped.
 * ──────────────────────────────────────────────────────────────────────────── */

internal fun resolveServerUrl(base: String?, url: HttpUrl): HttpUrl? {
    val raw = base?.trim()?.trimEnd('/')
    if (raw.isNullOrBlank()) return null

    val target = try {
        URI(raw)
    } catch (_: Exception) {
        return null
    }
    val host = target.host
    if (host.isNullOrBlank()) return null

    val prefix = target.rawPath.orEmpty().trimEnd('/')

    return url.newBuilder()
        .scheme(target.scheme ?: url.scheme)
        .host(host)
        .apply { if (target.port > 0) port(target.port) }
        .apply { if (prefix.isNotEmpty()) encodedPath(prefix + url.encodedPath) }
        .build()
}
