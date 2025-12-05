package app.src.data.repositories

import android.content.Context
import app.src.data.local.AppDatabase
import app.src.data.models.UserInsights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository para Business Questions y User Insights
 *
 * Proporciona análisis de patrones de compra del usuario
 * basándose en datos locales de órdenes históricas
 */
class InsightsRepository(context: Context) {

    private val insightsDao = AppDatabase.getDatabase(context).insightsDao()
    private val orderDao = AppDatabase.getDatabase(context).orderDao()

    /**
     * Obtener insights completos del usuario
     *
     * BQ Principal: ¿Cuál es el producto más frecuente y el patrón semanal de compra?
     */
    suspend fun getUserInsights(userId: Int): Result<UserInsights> = withContext(Dispatchers.IO) {
        try {
            // Obtener estadísticas generales
            val totalOrders = insightsDao.getTotalOrderCount(userId)

            // Si no hay órdenes, retornar insights vacíos
            if (totalOrders == 0) {
                return@withContext Result.Success(
                    UserInsights(
                        totalOrders = 0,
                        totalSpent = 0.0,
                        averageOrderValue = 0.0,
                        uniqueProducts = 0,
                        topProducts = emptyList(),
                        weeklyPattern = emptyList(),
                        categorySpending = emptyList(),
                        largestOrderValue = 0.0
                    )
                )
            }

            val totalSpent = insightsDao.getTotalSpent(userId)
            val avgOrderValue = insightsDao.getAverageOrderValue(userId)
            val uniqueProducts = insightsDao.getUniqueProductsCount(userId)
            val largestOrder = insightsDao.getLargestOrder(userId)

            // BQ1: Productos más frecuentes
            val topProducts = insightsDao.getMostFrequentProducts(userId, limit = 5)

            // BQ2: Patrón de compra semanal
            val weeklyPattern = insightsDao.getWeeklySpendingPattern(userId)

            // BQ3: Gasto por categoría
            val categorySpending = insightsDao.getSpendingByCategory(userId)

            val insights = UserInsights(
                totalOrders = totalOrders,
                totalSpent = totalSpent,
                averageOrderValue = avgOrderValue,
                uniqueProducts = uniqueProducts,
                topProducts = topProducts,
                weeklyPattern = weeklyPattern,
                categorySpending = categorySpending,
                largestOrderValue = largestOrder?.total ?: 0.0
            )

            Result.Success(insights)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al cargar estadísticas")
        }
    }

    /**
     * Obtener el producto favorito del usuario (más comprado)
     */
    suspend fun getFavoriteProduct(userId: Int) = withContext(Dispatchers.IO) {
        try {
            val topProducts = insightsDao.getMostFrequentProducts(userId, limit = 1)
            Result.Success(topProducts.firstOrNull())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener producto favorito")
        }
    }

    /**
     * Obtener día de la semana más activo
     */
    suspend fun getMostActiveDayOfWeek(userId: Int) = withContext(Dispatchers.IO) {
        try {
            val weeklyPattern = insightsDao.getWeeklySpendingPattern(userId)
            Result.Success(weeklyPattern.firstOrNull())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener día más activo")
        }
    }
}
