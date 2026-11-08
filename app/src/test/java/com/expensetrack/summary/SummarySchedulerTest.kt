package com.expensetrack.summary

import com.expensetrack.data.DaySummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SummarySchedulerTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val seven = LocalTime.of(19, 0)

    @Test
    fun `before the summary time it fires today`() {
        val now = ZonedDateTime.of(2026, 11, 7, 10, 15, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 7, 19, 0, 0, 0, zone), SummaryScheduler.nextTrigger(now, seven))
    }

    @Test
    fun `at or after the summary time it fires tomorrow`() {
        val exactly = ZonedDateTime.of(2026, 11, 7, 19, 0, 0, 0, zone)
        val later = ZonedDateTime.of(2026, 11, 7, 23, 59, 0, 0, zone)
        val tomorrow = ZonedDateTime.of(2026, 11, 8, 19, 0, 0, 0, zone)
        assertEquals(tomorrow, SummaryScheduler.nextTrigger(exactly, seven))
        assertEquals(tomorrow, SummaryScheduler.nextTrigger(later, seven))
    }

    @Test
    fun `message reflects total and count`() {
        assertEquals("今天共消费 ¥63.90（2 笔）", SummaryNotifier.message(DaySummary(6390, 2)))
        assertEquals("今天还没有记录到消费", SummaryNotifier.message(DaySummary(0, 0)))
    }
}
