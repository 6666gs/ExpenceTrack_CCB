package com.expensetrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("UPDATE expenses SET amountCents = :amountCents, note = :note, isIncome = :isIncome WHERE id = :id")
    suspend fun update(id: Long, amountCents: Long, note: String?, isIncome: Boolean)

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    suspend fun listBetween(start: Long, end: Long): List<Expense>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Expense>>

    /** Sum of spending, or of income when [income] is true. */
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE timestamp >= :start AND timestamp < :end AND isIncome = :income")
    fun observeTotal(start: Long, end: Long, income: Boolean = false): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE timestamp >= :start AND timestamp < :end AND isIncome = :income")
    suspend fun total(start: Long, end: Long, income: Boolean = false): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE timestamp >= :start AND timestamp < :end AND isIncome = 0")
    suspend fun spendingCount(start: Long, end: Long): Int

    @Query("SELECT * FROM expenses WHERE amountCents = :amountCents AND timestamp BETWEEN :from AND :to")
    suspend fun findByAmountBetween(amountCents: Long, from: Long, to: Long): List<Expense>
}
