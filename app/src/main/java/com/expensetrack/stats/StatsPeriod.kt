package com.expensetrack.stats

import com.expensetrack.util.DayRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class Granularity(val label: String) { DAY("日"), WEEK("周"), MONTH("月"), YEAR("年") }

/**
 * A calendar day, week (Monday first), month or year. Always built through [of], so two periods
 * covering the same dates are equal no matter which date inside them was used to create them.
 */
class StatsPeriod private constructor(val granularity: Granularity, val start: LocalDate) {

    /** Stable id for storage, e.g. "MONTH:2026-11-01". */
    val key: String get() = "${granularity.name}:$start"

    val endExclusive: LocalDate
        get() = when (granularity) {
            Granularity.DAY -> start.plusDays(1)
            Granularity.WEEK -> start.plusWeeks(1)
            Granularity.MONTH -> start.plusMonths(1)
            Granularity.YEAR -> start.plusYears(1)
        }

    fun range(zone: ZoneId = ZoneId.systemDefault()) = DayRange(
        start.atStartOfDay(zone).toInstant().toEpochMilli(),
        endExclusive.atStartOfDay(zone).toInstant().toEpochMilli(),
    )

    fun shift(steps: Long): StatsPeriod = of(
        granularity,
        when (granularity) {
            Granularity.DAY -> start.plusDays(steps)
            Granularity.WEEK -> start.plusWeeks(steps)
            Granularity.MONTH -> start.plusMonths(steps)
            Granularity.YEAR -> start.plusYears(steps)
        },
    )

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)

    /** Number of days in this period up to and including [today]; used for the daily average. */
    fun elapsedDays(today: LocalDate): Long = when {
        today.isBefore(start) -> 0
        !today.isBefore(endExclusive) -> ChronoUnit.DAYS.between(start, endExclusive)
        else -> ChronoUnit.DAYS.between(start, today) + 1
    }

    /** Switching granularity keeps today in view when it is inside this period. */
    fun withGranularity(target: Granularity, today: LocalDate): StatsPeriod =
        of(target, if (today in this) today else start)

    fun label(today: LocalDate): String {
        val sameYear = start.year == today.year
        return when (granularity) {
            Granularity.DAY -> when (start) {
                today -> "今天"
                today.minusDays(1) -> "昨天"
                else -> start.format(if (sameYear) MONTH_DAY_WEEKDAY else YEAR_MONTH_DAY)
            }
            Granularity.WEEK -> {
                val end = endExclusive.minusDays(1)
                val prefix = if (sameYear) "" else "${start.year}年 "
                "$prefix${start.format(MONTH_DAY)} – ${end.format(MONTH_DAY)}"
            }
            Granularity.MONTH -> start.format(YEAR_MONTH)
            Granularity.YEAR -> "${start.year}年"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is StatsPeriod && other.granularity == granularity && other.start == start

    override fun hashCode(): Int = 31 * granularity.hashCode() + start.hashCode()

    override fun toString(): String = "StatsPeriod($granularity, $start)"

    companion object {
        private val MONTH_DAY = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
        private val MONTH_DAY_WEEKDAY = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
        private val YEAR_MONTH_DAY = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
        private val YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

        fun of(granularity: Granularity, date: LocalDate): StatsPeriod = StatsPeriod(
            granularity,
            when (granularity) {
                Granularity.DAY -> date
                Granularity.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                Granularity.MONTH -> date.withDayOfMonth(1)
                Granularity.YEAR -> date.withDayOfYear(1)
            },
        )
    }
}
