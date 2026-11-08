package com.expensetrack.stats

import com.expensetrack.data.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One bar in a chart; tapping it drills down into [target]. */
data class Bucket(val label: String, val totalCents: Long, val target: StatsPeriod)

data class PeriodStats(
    val totalCents: Long,
    val count: Int,
    val dailyAverageCents: Long,
    val maxSingleCents: Long,
    val dailyTotals: Map<LocalDate, Long>,
    /** Seven weekday bars for a week, twelve month bars for a year, empty otherwise. */
    val buckets: List<Bucket>,
) {
    companion object {
        val EMPTY = PeriodStats(0, 0, 0, 0, emptyMap(), emptyList())
    }
}

val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

object StatsCalculator {

    fun compute(
        period: StatsPeriod,
        expenses: List<Expense>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): PeriodStats {
        val inPeriod = expenses.filter { it.localDate(zone) in period }
        val dailyTotals = inPeriod.groupBy { it.localDate(zone) }.mapValues { (_, list) -> list.sumOf { it.amountCents } }
        val total = inPeriod.sumOf { it.amountCents }
        val elapsed = period.elapsedDays(today)
        return PeriodStats(
            totalCents = total,
            count = inPeriod.size,
            dailyAverageCents = if (elapsed > 0) total / elapsed else 0,
            maxSingleCents = inPeriod.maxOfOrNull { it.amountCents } ?: 0,
            dailyTotals = dailyTotals,
            buckets = buckets(period, dailyTotals),
        )
    }

    private fun buckets(period: StatsPeriod, dailyTotals: Map<LocalDate, Long>): List<Bucket> =
        when (period.granularity) {
            Granularity.WEEK -> WEEKDAY_LABELS.mapIndexed { i, label ->
                val day = period.start.plusDays(i.toLong())
                Bucket(label, dailyTotals[day] ?: 0, StatsPeriod.of(Granularity.DAY, day))
            }
            Granularity.YEAR -> (0 until 12).map { i ->
                val month = period.start.plusMonths(i.toLong())
                val total = dailyTotals.entries
                    .filter { it.key.year == month.year && it.key.month == month.month }
                    .sumOf { it.value }
                Bucket("${i + 1}", total, StatsPeriod.of(Granularity.MONTH, month))
            }
            Granularity.DAY, Granularity.MONTH -> emptyList()
        }

    private fun Expense.localDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
}
