package app.src.data.local.dao

import androidx.room.*
import app.src.data.local.entities.OrderOutboxEntity

@Dao
interface OrderOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outbox: OrderOutboxEntity): Long

    @Query("SELECT * FROM order_outbox ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<OrderOutboxEntity>

    @Query("UPDATE order_outbox SET retries = :retries, lastAttempt = :lastAttempt WHERE id = :id")
    suspend fun updateRetries(id: Int, retries: Int, lastAttempt: Long)

    @Query("DELETE FROM order_outbox WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM order_outbox")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM order_outbox")
    suspend fun getPendingCount(): Int
}

