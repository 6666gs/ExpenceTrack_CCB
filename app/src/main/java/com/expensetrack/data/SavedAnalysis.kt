package com.expensetrack.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The latest DeepSeek analysis for one stats period, kept so it survives app restarts. */
@Entity(tableName = "saved_analyses")
data class SavedAnalysis(
    /** See StatsPeriod.key, e.g. "MONTH:2026-11-01". */
    @PrimaryKey val periodKey: String,
    /** SpendingAnalysis encoded by AnalysisCodec. */
    val resultJson: String,
    /** Epoch millis when the analysis finished. */
    val createdAt: Long,
    /** RecordsFingerprint of the records that were analysed; a mismatch means the result is outdated. */
    val fingerprint: String,
)

@Dao
interface SavedAnalysisDao {
    @Query("SELECT * FROM saved_analyses WHERE periodKey = :periodKey")
    fun observe(periodKey: String): Flow<SavedAnalysis?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(analysis: SavedAnalysis)
}
