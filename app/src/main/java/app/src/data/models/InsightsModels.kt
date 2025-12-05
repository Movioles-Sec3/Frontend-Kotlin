package app.src.data.models

import androidx.room.ColumnInfo

/**
 * Modelos para Business Questions y Analytics del usuario
 */

/**
 * BQ1: Frecuencia de productos comprados
 */
data class ProductFrequency(
    @ColumnInfo(name = "productId") val productId: Int,
    @ColumnInfo(name = "productName") val productName: String,
    @ColumnInfo(name = "orderCount") val orderCount: Int,
    @ColumnInfo(name = "totalQuantity") val totalQuantity: Int,
    @ColumnInfo(name = "totalSpent") val totalSpent: Double,
    @ColumnInfo(name = "imagenUrl") val imagenUrl: String? = null
)

/**
 * BQ2: Patrón de compra semanal
 */
data class WeeklySpending(
    @ColumnInfo(name = "dayOfWeek") val dayOfWeek: Int, // 0=Domingo, 1=Lunes, ..., 6=Sábado
    @ColumnInfo(name = "orderCount") val orderCount: Int,
    @ColumnInfo(name = "totalSpent") val totalSpent: Double,
    @ColumnInfo(name = "avgOrderValue") val avgOrderValue: Double
) {
    fun getDayName(): String {
        return when (dayOfWeek) {
            0 -> "Domingo"
            1 -> "Lunes"
            2 -> "Martes"
            3 -> "Miércoles"
            4 -> "Jueves"
            5 -> "Viernes"
            6 -> "Sábado"
            else -> "Desconocido"
        }
    }
}

/**
 * BQ3: Gasto por categoría
 */
data class CategorySpending(
    @ColumnInfo(name = "categoryName") val categoryName: String,
    @ColumnInfo(name = "orderCount") val orderCount: Int,
    @ColumnInfo(name = "totalSpent") val totalSpent: Double
)

/**
 * Resumen completo de insights del usuario
 */
data class UserInsights(
    val totalOrders: Int,
    val totalSpent: Double,
    val averageOrderValue: Double,
    val uniqueProducts: Int,
    val topProducts: List<ProductFrequency>,
    val weeklyPattern: List<WeeklySpending>,
    val categorySpending: List<CategorySpending>,
    val largestOrderValue: Double
)
