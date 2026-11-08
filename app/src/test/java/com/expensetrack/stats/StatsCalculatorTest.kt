package com.expensetrack.stats

import com.expensetrack.data.Expense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 11, 8)

    private fun expense(cents: Long, at: LocalDateTime) =
        Expense(amountCents = cents, timestamp = at.atZone(zone).toInstant().toEpochMilli(), source = "test")

    @Test
    fun `week stats bucket by weekday and average over elapsed days`() {
        val week = StatsPeriod.of(Granularity.WEEK, today)
        val list = listOf(
            expense(1000, LocalDateTime.of(2026, 11, 2, 8, 0)),
            expense(500, LocalDateTime.of(2026, 11, 2, 23, 59)),
            expense(2500, LocalDateTime.of(2026, 11, 8, 12, 0)),
            expense(9999, LocalDateTime.of(2026, 11, 9, 0, 0)), // next week, ignored
        )
        val stats = StatsCalculator.compute(week, list, today, zone)

        assertEquals(4000, stats.totalCents)
        assertEquals(3, stats.count)
        assertEquals(4000 / 7, stats.dailyAverageCents)
        assertEquals(2500, stats.maxSingleCents)
        assertEquals(listOf(1500L, 0, 0, 0, 0, 0, 2500), stats.buckets.map { it.totalCents })
        assertEquals(StatsPeriod.of(Granularity.DAY, LocalDate.of(2026, 11, 2)), stats.buckets[0].target)
    }

    @Test
    fun `year stats have twelve month buckets`() {
        val year = StatsPeriod.of(Granularity.YEAR, today)
        val list = listOf(
            expense(100, LocalDateTime.of(2026, 1, 31, 10, 0)),
            expense(300, LocalDateTime.of(2026, 11, 1, 10, 0)),
            expense(200, LocalDateTime.of(2026, 11, 8, 10, 0)),
        )
        val stats = StatsCalculator.compute(year, list, today, zone)

        assertEquals(12, stats.buckets.size)
        assertEquals(100, stats.buckets[0].totalCents)
        assertEquals(500, stats.buckets[10].totalCents)
        assertEquals("11", stats.buckets[10].label)
        assertEquals(mapOf(LocalDate.of(2026, 1, 31) to 100L, LocalDate.of(2026, 11, 1) to 300L,
            LocalDate.of(2026, 11, 8) to 200L), stats.dailyTotals)
    }

    @Test
    fun `empty period yields zeros`() {
        val stats = StatsCalculator.compute(StatsPeriod.of(Granularity.DAY, today), emptyList(), today, zone)
        assertEquals(0, stats.totalCents)
        assertEquals(0, stats.dailyAverageCents)
        assertEquals(emptyList<Bucket>(), stats.buckets)
    }
}
