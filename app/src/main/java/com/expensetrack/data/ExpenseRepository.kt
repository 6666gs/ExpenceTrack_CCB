package com.expensetrack.data

import com.expensetrack.parser.ParsedTransaction
import com.expensetrack.util.DayRange
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

data class DaySummary(val totalCents: Long, val count: Int, val incomeCents: Long = 0)

class ExpenseRepository(
    private val dao: ExpenseDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeRecent(limit: Int = 300): Flow<List<Expense>> = dao.observeRecent(limit)

    fun observeDayTotal(date: LocalDate): Flow<Long> =
        DayRange.of(date).let { dao.observeTotal(it.start, it.end) }

    fun observeMonthTotal(month: YearMonth): Flow<Long> =
        DayRange.ofMonth(month).let { dao.observeTotal(it.start, it.end) }

    fun observeMonthIncome(month: YearMonth): Flow<Long> =
        DayRange.ofMonth(month).let { dao.observeTotal(it.start, it.end, income = true) }

    fun observeBetween(range: DayRange): Flow<List<Expense>> = dao.observeBetween(range.start, range.end)

    suspend fun listBetween(range: DayRange): List<Expense> = dao.listBetween(range.start, range.end)

    /** Fixes a mis-captured amount, the note, or income vs spending in one write. */
    suspend fun updateExpense(id: Long, amountCents: Long, note: String?, isIncome: Boolean) {
        require(amountCents > 0) { "amount must be positive" }
        dao.update(id, amountCents, normalizeNote(note), isIncome)
    }

    suspend fun daySummary(date: LocalDate): DaySummary {
        val range = DayRange.of(date)
        return DaySummary(
            totalCents = dao.total(range.start, range.end),
            count = dao.spendingCount(range.start, range.end),
            incomeCents = dao.total(range.start, range.end, income = true),
        )
    }

    /** Returns true if a new record was inserted, false if it was a duplicate. */
    suspend fun recordCaptured(parsed: ParsedTransaction, source: String, rawText: String, postedAt: Long): Boolean {
        val candidate = Expense(
            amountCents = parsed.amountCents,
            timestamp = postedAt,
            merchant = parsed.merchant,
            source = source,
            rawText = rawText,
            isIncome = parsed.isIncome,
        )
        val nearby = dao.findByAmountBetween(
            parsed.amountCents,
            postedAt - DuplicateDetector.WINDOW_MS,
            postedAt + DuplicateDetector.WINDOW_MS,
        )
        if (DuplicateDetector.isDuplicate(candidate, nearby)) return false
        dao.insert(candidate)
        return true
    }

    suspend fun addManual(amountCents: Long, note: String?, isIncome: Boolean = false) {
        require(amountCents > 0) { "amount must be positive" }
        dao.insert(
            Expense(
                amountCents = amountCents,
                timestamp = now(),
                note = normalizeNote(note),
                source = SOURCE_MANUAL,
                isIncome = isIncome,
            )
        )
    }

    suspend fun delete(expense: Expense) = dao.delete(expense)

    companion object {
        fun normalizeNote(note: String?): String? = note?.trim()?.take(MAX_NOTE_LENGTH)?.ifBlank { null }
    }
}
