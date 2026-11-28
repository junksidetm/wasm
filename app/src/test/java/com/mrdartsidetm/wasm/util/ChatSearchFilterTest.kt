package com.mrdartsidetm.wasm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Unit tests for per-conversation DMY/MDY inference, narrow no-break space timestamp parsing,
 * Instagram date parsing, and exact/range date + time filters.
 */
class ChatSearchFilterTest {

    @Test
    fun testDateOrderInferenceForDayFirstAndMonthFirstExports() {
        val abhijeetTimestamps = listOf(
            "29/11/2026, 12:02\u202Fam",
            "29/11/2026, 12:04\u202Fam"
        )
        assertEquals(DateOrderPreference.DAY_FIRST, ChatSearchFilter.inferDateOrder(abhijeetTimestamps))

        val deepanjaliTimestamps = listOf(
            "7/24/23, 3:32\u202FPM",
            "8/14/23, 1:21\u202FPM"
        )
        assertEquals(DateOrderPreference.MONTH_FIRST, ChatSearchFilter.inferDateOrder(deepanjaliTimestamps))
    }

    @Test
    fun testWhatsAppTimestampParsingHandlesNarrowNoBreakSpaceAnd12HourMidnight() {
        val parsedAbhijeet = ChatSearchFilter.parseWhatsAppDateTime(
            rawTimestamp = "29/11/2026, 12:04\u202Fam",
            order = DateOrderPreference.DAY_FIRST
        )
        assertNotNull(parsedAbhijeet)
        assertEquals(LocalDate.of(2026, 11, 29), parsedAbhijeet!!.first)
        assertEquals(LocalTime.of(0, 4), parsedAbhijeet.second)

        val parsedDeepanjali = ChatSearchFilter.parseWhatsAppDateTime(
            rawTimestamp = "7/24/23, 3:32\u202FPM",
            order = DateOrderPreference.MONTH_FIRST
        )
        assertNotNull(parsedDeepanjali)
        assertEquals(LocalDate.of(2023, 7, 24), parsedDeepanjali!!.first)
        assertEquals(LocalTime.of(15, 32), parsedDeepanjali.second)
    }

    @Test
    fun testInstagramDateTimeParsingFromEpochAndFormattedString() {
        val targetDate = LocalDate.of(2026, 9, 14)
        val targetTime = LocalTime.of(21, 15)
        val epoch = targetDate.atTime(targetTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val fromEpoch = ChatSearchFilter.parseInstagramDateTime(epoch, "ignored")
        assertEquals(targetDate to targetTime, fromEpoch)

        val fromRaw = ChatSearchFilter.parseInstagramDateTime(0L, "Sep 14, 2026 9:15 PM")
        assertNotNull(fromRaw)
        assertEquals(targetDate, fromRaw!!.first)
        assertEquals(targetTime, fromRaw.second)
    }

    @Test
    fun testExactAndRangeDateAndTimeFilters() {
        val d1 = LocalDate.of(2026, 11, 28)
        val d2 = LocalDate.of(2026, 11, 29)
        val d3 = LocalDate.of(2026, 11, 30)
        val tMorning = LocalTime.of(9, 30)
        val tNoon = LocalTime.of(12, 4)

        // Exact day
        val exactDay = ChatDateTimeFilter(startDate = d2, endDate = d2)
        assertTrue(ChatSearchFilter.matchesFilter(d2, tNoon, exactDay))
        assertFalse(ChatSearchFilter.matchesFilter(d1, tNoon, exactDay))

        // Date range
        val dateRange = ChatDateTimeFilter(startDate = d1, endDate = d2)
        assertTrue(ChatSearchFilter.matchesFilter(d1, tMorning, dateRange))
        assertTrue(ChatSearchFilter.matchesFilter(d2, tNoon, dateRange))
        assertFalse(ChatSearchFilter.matchesFilter(d3, tMorning, dateRange))

        // Exact time (minute precision)
        val exactTime = ChatDateTimeFilter(startTime = LocalTime.of(12, 4), endTime = null)
        assertTrue(ChatSearchFilter.matchesFilter(d2, LocalTime.of(12, 4, 42), exactTime))
        assertFalse(ChatSearchFilter.matchesFilter(d2, LocalTime.of(12, 5), exactTime))

        // Same-day and midnight-crossing time ranges
        val dayTimeRange = ChatDateTimeFilter(startTime = LocalTime.of(9, 0), endTime = LocalTime.of(13, 0))
        assertTrue(ChatSearchFilter.matchesFilter(d2, tMorning, dayTimeRange))
        assertTrue(ChatSearchFilter.matchesFilter(d2, tNoon, dayTimeRange))
        assertFalse(ChatSearchFilter.matchesFilter(d2, LocalTime.of(14, 0), dayTimeRange))

        val midnightRange = ChatDateTimeFilter(startTime = LocalTime.of(22, 0), endTime = LocalTime.of(2, 0))
        assertTrue(ChatSearchFilter.matchesFilter(d2, LocalTime.of(23, 30), midnightRange))
        assertTrue(ChatSearchFilter.matchesFilter(d2, LocalTime.of(1, 15), midnightRange))
        assertFalse(ChatSearchFilter.matchesFilter(d2, LocalTime.of(12, 0), midnightRange))
    }
}
