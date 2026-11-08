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

    @Query("UPDATE expenses SET amountCents = :amountCents, note = :note WHERE id = :id")
    suspend fun updateAmountAndNote(id: Long, amountCents: Long, note: String?)

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    suspend fun listBetween(start: Long, end: Long): List<Expense>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE timestamp >= :start AND timestamp < :end")
    fun observeTotal(start: Long, end: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE timestamp >= :start AND timestamp < :end")
    suspend fun total(start: Long, end: Long): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE timestamp >= :start AND timestamp < :end")
    suspend fun count(start: Long, end: Long): Int

    @Query("SELECT * FROM expenses WHERE amountCents = :amountCents AND timestamp BETWEEN :from AND :to")
    suspend fun findByAmountBetween(amountCents: Long, from: Long, to: Long): List<Expense>
}
