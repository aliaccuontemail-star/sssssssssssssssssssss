package ir.pinbom.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.round

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · number & time formatting (the engine)
 *
 *  Pure logic only — no words, no composables. The *language-aware* API the app
 *  calls lives in `Localization.kt` (Localizer.number / Localizer.compact /
 *  Localizer.relativeTime): Persian gets ۱۲٬۴۵۰ and ۱٫۲ هزار, English gets
 *  12,450 and 1.2K. The unit words themselves come from `strings.xml`, so
 *  nothing user-visible is hard-coded here.
 * ──────────────────────────────────────────────────────────────────────────── */

private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
private const val FA_THOUSANDS = '\u066C' // ٬ Arabic thousands separator, used in fa-IR
private const val FA_DECIMAL = '\u066B'   // ٫ Arabic decimal separator

/** "1402" → "۱۴۰۲". Non-digits pass through. */
fun faDigits(value: String): String = buildString(value.length) {
    for (char in value) {
        append(if (char in '0'..'9') FA_DIGITS[char - '0'] else char)
    }
}

private fun group(value: Long, separator: Char): String {
    val negative = value < 0
    val body = abs(value).toString()
    val out = StringBuilder(body.length + 4)
    for ((index, char) in body.withIndex()) {
        if (index > 0 && (body.length - index) % 3 == 0) out.append(separator)
        out.append(char)
    }
    return if (negative) "-$out" else out.toString()
}

/** -12450 → "۱۲٬۴۵۰" (the form Persian readers expect). */
fun faGroup(value: Long): String = faDigits(group(value, FA_THOUSANDS))

/** -12450 → "12,450". */
fun enGroup(value: Long): String = group(value, ',')

/**
 * One decimal at most, and none past ten: 1.2 → "1.2", 12.6 → "13", 3.0 → "3".
 * Digits are still Latin here — the caller localises them.
 */
fun scaledTenths(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val magnitude = abs(value)
    val digits = if (magnitude < 10.0) {
        val tenths = round(magnitude * 10).toLong()
        val whole = tenths / 10
        val fraction = tenths % 10
        if (fraction == 0L) whole.toString() else "$whole.$fraction"
    } else {
        round(magnitude).toLong().toString()
    }
    return sign + digits
}

enum class MagnitudeUnit { None, Thousand, Million, Billion }

/** Splits a count into (scaled value, unit) so the wording can come from resources. */
fun magnitudeOf(value: Long): Pair<Double, MagnitudeUnit> = when {
    abs(value) < 1_000L -> value.toDouble() to MagnitudeUnit.None
    abs(value) < 1_000_000L -> value / 1_000.0 to MagnitudeUnit.Thousand
    abs(value) < 1_000_000_000L -> value / 1_000_000.0 to MagnitudeUnit.Million
    else -> value / 1_000_000_000.0 to MagnitudeUnit.Billion
}

/** Decimal separator of the language, applied to a `scaledTenths` string. */
fun localizeDecimal(value: String, persian: Boolean): String =
    if (persian) faDigits(value).replace('.', FA_DECIMAL) else value

/* ── Time ─────────────────────────────────────────────────────────────────── */

private val ISO_PATTERNS = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd",
)

/** Accepts ISO-8601 (with or without zone) or epoch seconds/milliseconds. */
fun parseTimestamp(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    raw.trim().toLongOrNull()?.let { epoch ->
        return if (epoch < 100_000_000_000L) epoch * 1000L else epoch
    }
    for (pattern in ISO_PATTERNS) {
        val hasZone = pattern.contains('X') || pattern.contains('Z')
        val format = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = true
            if (!hasZone) timeZone = TimeZone.getTimeZone("UTC")
        }
        runCatching { format.parse(raw) }.getOrNull()?.let { return it.time }
    }
    return null
}

/** Buckets a timestamp so the wording can be resolved from resources. */
sealed interface RelativeTime {
    data object Now : RelativeTime
    data class Minutes(val value: Long) : RelativeTime
    data class Hours(val value: Long) : RelativeTime
    data object Yesterday : RelativeTime
    data class Days(val value: Long) : RelativeTime
    data class Months(val value: Long) : RelativeTime
    data class Years(val value: Long) : RelativeTime
    data object Unknown : RelativeTime
}

fun relativeTimeOf(iso: String?, now: Long = System.currentTimeMillis()): RelativeTime {
    val millis = parseTimestamp(iso) ?: return RelativeTime.Unknown
    val seconds = ((now - millis).coerceAtLeast(0L)) / 1000L
    return when {
        seconds < 45 -> RelativeTime.Now
        seconds < 3_600 -> RelativeTime.Minutes(seconds / 60)
        seconds < 86_400 -> RelativeTime.Hours(seconds / 3_600)
        seconds < 172_800 -> RelativeTime.Yesterday
        seconds < 2_592_000 -> RelativeTime.Days(seconds / 86_400)
        seconds < 31_536_000 -> RelativeTime.Months((seconds / 2_592_000).coerceAtLeast(1L))
        else -> RelativeTime.Years((seconds / 31_536_000).coerceAtLeast(1L))
    }
}

/** A clock that ticks, so relative timestamps stay honest without recomposing everything. */
@Composable
fun rememberNow(refreshMillis: Long = 30_000L): Long {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(refreshMillis)
            value = System.currentTimeMillis()
        }
    }
    return now
}
