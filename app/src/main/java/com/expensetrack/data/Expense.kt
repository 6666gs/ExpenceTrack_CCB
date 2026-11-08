package com.expensetrack.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val SOURCE_MANUAL = "manual"
const val MAX_NOTE_LENGTH = 100

@Entity(tableName = "expenses", indices = [Index("timestamp")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Amount in fen (1/100 yuan) to avoid floating point errors. */
    val amountCents: Long,
    /** Epoch millis. */
    val timestamp: Long,
    val merchant: String? = null,
    val note: String? = null,
    /** Package name of the notifying app, or [SOURCE_MANUAL]. */
    val source: String,
    /** Original notification text, kept for dedup and troubleshooting. */
    val rawText: String? = null,
    /** Money in (income, refund, reversal). Records are spending unless marked otherwise. */
    @ColumnInfo(defaultValue = "0")
    val isIncome: Boolean = false,
) {
    val isManual: Boolean get() = source == SOURCE_MANUAL

    /** What the user sees first: their own note wins over the captured merchant name. */
    val title: String
        get() = note ?: merchant ?: when {
            isManual -> if (isIncome) "手动收入" else "手动记录"
            isIncome -> "建设银行入账"
            else -> "建设银行消费"
        }
}
