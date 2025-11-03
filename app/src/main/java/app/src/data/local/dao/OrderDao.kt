package app.src.data.local.dao

import androidx.room.*
import app.src.data.local.entities.OrderEntity
import app.src.data.local.entities.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    // === INSERCIÓN ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }

    // === CONSULTAS ===
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getOrdersByUser(userId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: Int): List<OrderItemEntity>

    @Query("SELECT * FROM orders WHERE status = :status AND userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastOrderByStatus(userId: Int, status: String): OrderEntity?

    // === ACTUALIZACIÓN ===
    @Query("UPDATE orders SET status = :status, readyAt = :readyAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, status: String, readyAt: Long? = null)

    @Query("UPDATE orders SET status = 'DELIVERED', deliveredAt = :deliveredAt WHERE id = :orderId")
    suspend fun markAsDelivered(orderId: Int, deliveredAt: Long)

    // === ELIMINACIÓN ===
    @Query("DELETE FROM orders WHERE createdAt < :timestamp")
    suspend fun deleteOldOrders(timestamp: Long)

    @Query("DELETE FROM orders")
    suspend fun clearAll()

    // === ESTADÍSTICAS ===
    @Query("SELECT COUNT(*) FROM orders WHERE userId = :userId")
    suspend fun getOrderCount(userId: Int): Int

    @Query("SELECT * FROM orders WHERE status = 'READY' AND userId = :userId LIMIT 1")
    suspend fun getActiveReadyOrder(userId: Int): OrderEntity?

    // === OBTENER ÚLTIMO ID PARA ÓRDENES OFFLINE ===
    @Query("SELECT MAX(id) FROM orders WHERE id > 0")
    suspend fun getMaxOrderId(): Int?
}
