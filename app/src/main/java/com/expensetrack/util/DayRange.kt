package com.expensetrack.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Half-open epoch-millis interval [start, end). */
data class DayRange(val start: Long, val end: Long) {
    companion object {
        fun of(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) = DayRange(
            date.atStartOfDay(zone).toInstant().toEpochMilli(),
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        fun ofMonth(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()) = DayRange(
            month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }
}
