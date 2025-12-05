package app.src.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.src.data.models.ProductFrequency
import app.src.data.models.WeeklySpending
import app.src.data.models.CategorySpending

/**
 * DAO para Business Questions y Analytics
 *
 * BQ Principal: ¿Cuál es el producto más frecuente en las órdenes del usuario
 * y cuál es su patrón de compra semanal?
 */
@Dao
interface InsightsDao {

    /**
     * BQ1: Obtener los productos más comprados por frecuencia
     * Retorna: producto, cantidad total de veces comprado, y gasto total
     */
    @Query("""
        SELECT 
            oi.productId,
            oi.name as productName,
            COUNT(DISTINCT oi.orderId) as orderCount,
            SUM(oi.quantity) as totalQuantity,
            SUM(oi.quantity * oi.price) as totalSpent,
            oi.imagenUrl as imagenUrl
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.userId = :userId 
        AND o.status != 'CARRITO'
        GROUP BY oi.productId, oi.name, oi.imagenUrl
        ORDER BY orderCount DESC, totalSpent DESC
        LIMIT :limit
    """)
    suspend fun getMostFrequentProducts(userId: Int, limit: Int = 10): List<ProductFrequency>

    /**
     * BQ2: Patrón de compra semanal (día de la semana más activo)
     * Agrupa por día de la semana desde epoch
     */
    @Query("""
        SELECT 
            CAST(strftime('%w', datetime(createdAt/1000, 'unixepoch')) AS INTEGER) as dayOfWeek,
            COUNT(*) as orderCount,
            SUM(total) as totalSpent,
            AVG(total) as avgOrderValue
        FROM orders
        WHERE userId = :userId 
        AND status != 'CARRITO'
        GROUP BY dayOfWeek
        ORDER BY orderCount DESC
    """)
    suspend fun getWeeklySpendingPattern(userId: Int): List<WeeklySpending>

    /**
     * BQ3: Gasto por categoría (basado en tipo de producto)
     * Requiere tener datos del tipo en order_items
     */
    @Query("""
        SELECT 
            'Categoría ' || (oi.productId % 5 + 1) as categoryName,
            COUNT(DISTINCT oi.orderId) as orderCount,
            SUM(oi.quantity * oi.price) as totalSpent
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.userId = :userId 
        AND o.status != 'CARRITO'
        GROUP BY (oi.productId % 5)
        ORDER BY totalSpent DESC
    """)
    suspend fun getSpendingByCategory(userId: Int): List<CategorySpending>

    /**
     * Estadísticas generales del usuario
     */
    @Query("""
        SELECT COUNT(*) FROM orders 
        WHERE userId = :userId AND status != 'CARRITO'
    """)
    suspend fun getTotalOrderCount(userId: Int): Int

    @Query("""
        SELECT COALESCE(SUM(total), 0.0) FROM orders 
        WHERE userId = :userId AND status != 'CARRITO'
    """)
    suspend fun getTotalSpent(userId: Int): Double

    @Query("""
        SELECT COALESCE(AVG(total), 0.0) FROM orders 
        WHERE userId = :userId AND status != 'CARRITO'
    """)
    suspend fun getAverageOrderValue(userId: Int): Double

    @Query("""
        SELECT * FROM orders 
        WHERE userId = :userId AND status != 'CARRITO'
        ORDER BY total DESC
        LIMIT 1
    """)
    suspend fun getLargestOrder(userId: Int): app.src.data.local.entities.OrderEntity?

    /**
     * Obtener productos únicos comprados
     */
    @Query("""
        SELECT COUNT(DISTINCT productId) FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.userId = :userId AND o.status != 'CARRITO'
    """)
    suspend fun getUniqueProductsCount(userId: Int): Int
}
