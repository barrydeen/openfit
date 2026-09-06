package dev.openfit.app.coach

import dev.openfit.app.llm.ToolSpec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A tool the coach agent can invoke. Implementations must return a string result. */
interface CoachTool {
    val spec: ToolSpec
    suspend fun run(args: JsonObject): String
}

/** Safe accessors for tool arguments. */
object CoachArgs {
    fun string(json: JsonObject, key: String): String? =
        (json[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    fun double(json: JsonObject, key: String): Double? =
        (json[key] as? JsonPrimitive)?.doubleOrNull

    fun int(json: JsonObject, key: String): Int? =
        (json[key] as? JsonPrimitive)?.intOrNull

    fun long(json: JsonObject, key: String): Long? =
        (json[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

    fun boolean(json: JsonObject, key: String): Boolean =
        (json[key] as? JsonPrimitive)?.booleanOrNull ?: false
}

/** Parses YYYY-MM-DD dates into epoch millis ranges. */
object CoachDates {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Resolves [start]/[end] (YYYY-MM-DD, optional) into an inclusive [fromMillis, toMillis) range. */
    fun range(start: String?, end: String?): Pair<Long, Long> {
        val today = LocalDate.now()
        val startDate = start?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() } ?: today
        val endDate = end?.let { runCatching { LocalDate.parse(it, formatter) }.getOrNull() } ?: startDate
        val from = minOf(startDate, endDate)
        val to = maxOf(startDate, endDate)
        val fromMillis = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMillis = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return fromMillis to toMillis
    }

    fun toIso(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

    fun timestampFor(date: String?, fallback: Long): Long {
        if (date == null) return fallback
        val parsed = runCatching { LocalDate.parse(date, formatter) }.getOrNull() ?: return fallback
        return parsed.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

/** Formats a number compactly (whole when round, else one decimal). */
fun formatNum(value: Double): String {
    val rounded = BigDecimal(value).setScale(1, RoundingMode.HALF_UP)
    return if (rounded.stripTrailingZeros().scale() <= 0) {
        rounded.setScale(0, RoundingMode.HALF_UP).toPlainString()
    } else {
        rounded.toPlainString()
    }
}
