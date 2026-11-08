package com.expensetrack.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsPeriodTest {
    private val sunday = LocalDate.of(2026, 11, 8)

    @Test
    fun `week starts on monday and spans seven days`() {
        val week = StatsPeriod.of(Granularity.WEEK, sunday)
        assertEquals(LocalDate.of(2026, 11, 2), week.start)
        assertEquals(LocalDate.of(2026, 11, 9), week.endExclusive)
    }

    @Test
    fun `periods built from different dates inside them are equal`() {
        assertEquals(StatsPeriod.of(Granularity.MONTH, sunday), StatsPeriod.of(Granularity.MONTH, LocalDate.of(2026, 11, 30)))
        assertEquals(StatsPeriod.of(Granularity.YEAR, sunday).start, LocalDate.of(2026, 1, 1))
    }

    @Test
    fun `shift moves by whole periods across year boundaries`() {
        val dec = StatsPeriod.of(Granularity.MONTH, LocalDate.of(2026, 12, 15)).shift(1)
        assertEquals(LocalDate.of(2027, 1, 1), dec.start)
        assertEquals(LocalDate.of(2026, 10, 26), StatsPeriod.of(Granularity.WEEK, sunday).shift(-1).start)
    }

    @Test
    fun `contains is half open`() {
        val month = StatsPeriod.of(Granularity.MONTH, sunday)
        assertTrue(LocalDate.of(2026, 11, 1) in month)
        assertTrue(LocalDate.of(2026, 11, 30) in month)
        assertFalse(LocalDate.of(2026, 12, 1) in month)
    }

    @Test
    fun `elapsed days counts only started days`() {
        val month = StatsPeriod.of(Granularity.MONTH, sunday)
        assertEquals(8, month.elapsedDays(sunday))
        assertEquals(30, month.elapsedDays(LocalDate.of(2027, 1, 1)))
        assertEquals(0, month.elapsedDays(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `switching granularity keeps today when it is inside the period`() {
        val month = StatsPeriod.of(Granularity.MONTH, sunday)
        assertEquals(sunday, month.withGranularity(Granularity.DAY, sunday).start)
        val lastMonth = month.shift(-1)
        assertEquals(LocalDate.of(2026, 10, 1), lastMonth.withGranularity(Granularity.DAY, sunday).start)
    }

    @Test
    fun `key is stable for storage`() {
        assertEquals("WEEK:2026-11-02", StatsPeriod.of(Granularity.WEEK, sunday).key)
        assertEquals("MONTH:2026-11-01", StatsPeriod.of(Granularity.MONTH, sunday).key)
    }

    @Test
    fun `labels read naturally`() {
        assertEquals("今天", StatsPeriod.of(Granularity.DAY, sunday).label(sunday))
        assertEquals("昨天", StatsPeriod.of(Granularity.DAY, sunday.minusDays(1)).label(sunday))
        assertEquals("11月2日 – 11月8日", StatsPeriod.of(Granularity.WEEK, sunday).label(sunday))
        assertEquals("2025年 12月29日 – 1月4日", StatsPeriod.of(Granularity.WEEK, LocalDate.of(2025, 12, 30)).label(sunday))
        assertEquals("2026年11月", StatsPeriod.of(Granularity.MONTH, sunday).label(sunday))
        assertEquals("2026年", StatsPeriod.of(Granularity.YEAR, sunday).label(sunday))
    }
}
