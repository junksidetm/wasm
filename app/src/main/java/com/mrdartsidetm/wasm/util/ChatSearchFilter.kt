package com.mrdartsidetm.wasm.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.regex.Pattern

/**
 * Filter criteria supporting keyword search, exact date, date range, exact time, and time range.
 */
data class ChatDateTimeFilter(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null
) {
    val isActive: Boolean
        get() = startDate != null || endDate != null || startTime != null || endTime != null
}

enum class DateOrderPreference {
    DAY_FIRST,
    MONTH_FIRST
}

/**
 * Infers `DD/MM` vs `MM/DD` per conversation and filters WhatsApp / Instagram messages
 * across exact dates, date ranges, exact times, and time ranges.
 */
object ChatSearchFilter {

    private val numericTimestampPattern = Pattern.compile(
        """^\s*(\d{1,2})[/.-](\d{1,2})[/.-](\d{2,4})(?:,\s*|\s+)(\d{1,2}):(\d{2})(?::(\d{2}))?(?:\s*([aApP][mM]))?\s*$"""
    )

    private val instagramTimeFormats: List<DateTimeFormatter> = listOf(
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d, yyyy h:mm a").toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM dd, yyyy h:mm a").toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d, yyyy, h:mm a").toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yyyy, HH:mm").toFormatter(Locale.ENGLISH),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )

    /**
     * Scans timestamps in a conversation to determine whether ambiguous dates like `7/12/23`
     * should be interpreted as `day/month` (Indian/UK WhatsApp) or `month/day` (US WhatsApp).
     */
    fun inferDateOrder(timestamps: List<String>): DateOrderPreference {
        for (raw in timestamps) {
            val clean = WhatsAppParser.sanitizeLine(raw).trim()
            val m = numericTimestampPattern.matcher(clean)
            if (m.find()) {
                val p1 = m.group(1)?.toIntOrNull() ?: continue
                val p2 = m.group(2)?.toIntOrNull() ?: continue
                if (p1 > 12) return DateOrderPreference.DAY_FIRST
                if (p2 > 12) return DateOrderPreference.MONTH_FIRST
            }
        }
        return DateOrderPreference.DAY_FIRST
    }

    fun parseWhatsAppDateTime(
        rawTimestamp: String,
        order: DateOrderPreference = DateOrderPreference.DAY_FIRST
    ): Pair<LocalDate, LocalTime>? {
        val clean = WhatsAppParser.sanitizeLine(rawTimestamp).trim()
        val m = numericTimestampPattern.matcher(clean)
        if (!m.find()) return null

        val p1 = m.group(1)?.toIntOrNull() ?: return null
        val p2 = m.group(2)?.toIntOrNull() ?: return null
        val yRaw = m.group(3)?.toIntOrNull() ?: return null
        val year = if (yRaw < 100) 2000 + yRaw else yRaw

        val (day, month) = if (order == DateOrderPreference.MONTH_FIRST) p2 to p1 else p1 to p2
        val date = try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            return null
        }

        val hRaw = m.group(4)?.toIntOrNull() ?: return null
        val min = m.group(5)?.toIntOrNull() ?: return null
        val sec = m.group(6)?.toIntOrNull() ?: 0
        val amPm = m.group(7)?.lowercase(Locale.ROOT)

        val hour = when {
            amPm == "am" -> if (hRaw == 12) 0 else hRaw
            amPm == "pm" -> if (hRaw == 12) 12 else (hRaw + 12) % 24
            else -> hRaw
        }
        val time = try {
            LocalTime.of(hour.coerceIn(0, 23), min.coerceIn(0, 59), sec.coerceIn(0, 59))
        } catch (_: Exception) {
            return null
        }
        return date to time
    }

    fun parseInstagramDateTime(epochMillis: Long, rawTimestamp: String): Pair<LocalDate, LocalTime>? {
        if (epochMillis > 0L) {
            val zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
            return zdt.toLocalDate() to zdt.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
        }
        val clean = WhatsAppParser.sanitizeLine(rawTimestamp).trim()
        for (fmt in instagramTimeFormats) {
            try {
                val ldt = java.time.LocalDateTime.parse(clean, fmt)
                return ldt.toLocalDate() to ldt.toLocalTime()
            } catch (_: Exception) {
            }
        }
        return parseWhatsAppDateTime(clean, DateOrderPreference.MONTH_FIRST)
    }

    fun matchesFilter(
        date: LocalDate?,
        time: LocalTime?,
        filter: ChatDateTimeFilter
    ): Boolean {
        if (!filter.isActive) return true

        val startD = filter.startDate
        val endD = filter.endDate ?: startD
        if (startD != null) {
            if (date == null) return false
            val low = if (endD != null && endD.isBefore(startD)) endD else startD
            val high = if (endD != null && endD.isBefore(startD)) startD else (endD ?: startD)
            if (date.isBefore(low) || date.isAfter(high)) return false
        }

        val startT = filter.startTime?.truncatedTo(ChronoUnit.MINUTES)
        val endT = filter.endTime?.truncatedTo(ChronoUnit.MINUTES)
        if (startT != null) {
            if (time == null) return false
            val msgMin = time.truncatedTo(ChronoUnit.MINUTES)
            if (endT == null) {
                // Exact time selection compares hour:minute accurately
                if (msgMin != startT) return false
            } else if (!endT.isBefore(startT)) {
                if (msgMin.isBefore(startT) || msgMin.isAfter(endT)) return false
            } else {
                // Midnight-spanning range e.g. 22:00 .. 02:00
                if (msgMin.isBefore(startT) && msgMin.isAfter(endT)) return false
            }
        }
        return true
    }
}
