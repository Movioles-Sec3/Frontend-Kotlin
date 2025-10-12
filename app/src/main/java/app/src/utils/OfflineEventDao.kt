package app.src.utils

import androidx.room.*

@Dao
interface OfflineEventDao {
    @Insert
    suspend fun insert(event: OfflineEvent): Long

    @Query("SELECT * FROM offline_events WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingEvents(): List<OfflineEvent>

    @Query("UPDATE offline_events SET isSynced = 1 WHERE id = :eventId")
    suspend fun markAsSynced(eventId: Long)

    @Query("UPDATE offline_events SET retryCount = :retryCount, lastError = :error WHERE id = :eventId")
    suspend fun updateRetryInfo(eventId: Long, retryCount: Int, error: String)

    @Query("DELETE FROM offline_events WHERE isSynced = 1 AND timestamp < :olderThan")
    suspend fun deleteSyncedOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM offline_events WHERE isSynced = 0")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM offline_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 10): List<OfflineEvent>
}

