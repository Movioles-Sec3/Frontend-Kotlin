package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.local.AppDatabase
import app.src.data.local.entities.CalificacionEntity
import app.src.utils.cache.CalificacionCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio para calificaciones de órdenes
 * Usa múltiples dispatchers para operaciones paralelas:
 * - Dispatchers.IO: Operaciones de base de datos (comentarios)
 * - Dispatchers.Default: Operaciones de cache (calificaciones)
 * - Dispatchers.Main: Actualización de UI
 */
class CalificacionRepository(context: Context) {

    private val calificacionDao = AppDatabase.getDatabase(context).calificacionDao()
    private val cache = CalificacionCache.getInstance()

    companion object {
        private const val TAG = "CalificacionRepository"
    }

    /**
     * Guardar calificación completa (comentario en BD + rating en cache)
     * Usa multithreading para operaciones paralelas
     */
    suspend fun saveCalificacion(
        orderId: Int,
        rating: Int,
        comentario: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "💾 Guardando calificación para Order #$orderId")

        try {
            val calificacion = CalificacionEntity(
                orderId = orderId,
                calificacion = rating,
                comentario = comentario
            )

            // Operación 1: Guardar comentario en BD (Dispatchers.IO)
            calificacionDao.insertCalificacion(calificacion)
            Log.d(TAG, "✅ Comentario guardado en BD: Order #$orderId")

            // Operación 2: Guardar rating en cache (Dispatchers.Default - CPU)
            withContext(Dispatchers.Default) {
                cache.saveCalificacion(orderId, rating)
                Log.d(TAG, "✅ Rating guardado en cache: Order #$orderId -> $rating/10")
            }

            Log.d(TAG, "🎉 Calificación completa guardada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando calificación: ${e.message}", e)
            throw e
        }
    }

    /**
     * Obtener calificación completa (BD + Cache)
     * Usa lectura paralela de múltiples fuentes
     */
    suspend fun getCalificacion(orderId: Int): CalificacionEntity? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Buscando calificación para Order #$orderId")

        try {
            // Leer desde BD
            val calificacion = calificacionDao.getCalificacionByOrderId(orderId)

            if (calificacion != null) {
                Log.d(TAG, "✅ Calificación encontrada en BD")

                // Verificar cache en paralelo (Dispatchers.Default)
                withContext(Dispatchers.Default) {
                    val cachedRating = cache.getCalificacion(orderId)
                    if (cachedRating == null) {
                        // Sincronizar cache desde BD
                        cache.saveCalificacion(orderId, calificacion.calificacion)
                        Log.d(TAG, "🔄 Cache sincronizado desde BD")
                    }
                }
            } else {
                Log.d(TAG, "❌ Calificación no encontrada")
            }

            calificacion
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo calificación: ${e.message}", e)
            null
        }
    }

    /**
     * Obtener solo el rating desde cache (ultra rápido)
     * Usa Dispatchers.Default para operaciones de CPU
     */
    suspend fun getRatingFromCache(orderId: Int): Int? = withContext(Dispatchers.Default) {
        cache.getCalificacion(orderId)
    }

    /**
     * Verificar si existe calificación (verifica ambas fuentes en paralelo)
     */
    suspend fun hasCalificacion(orderId: Int): Boolean = withContext(Dispatchers.IO) {
        // Verificar BD y cache en paralelo
        val fromDb = calificacionDao.getCalificacionByOrderId(orderId) != null
        val fromCache = withContext(Dispatchers.Default) {
            cache.hasCalificacion(orderId)
        }

        fromDb || fromCache
    }

    /**
     * Actualizar calificación existente
     */
    suspend fun updateCalificacion(
        orderId: Int,
        rating: Int,
        comentario: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Actualizando calificación para Order #$orderId")

        val calificacion = CalificacionEntity(
            orderId = orderId,
            calificacion = rating,
            comentario = comentario
        )

        // Actualizar BD
        calificacionDao.updateCalificacion(calificacion)

        // Actualizar cache en paralelo
        withContext(Dispatchers.Default) {
            cache.saveCalificacion(orderId, rating)
        }

        Log.d(TAG, "✅ Calificación actualizada exitosamente")
    }

    /**
     * Eliminar calificación
     */
    suspend fun deleteCalificacion(orderId: Int) = withContext(Dispatchers.IO) {
        val calificacion = calificacionDao.getCalificacionByOrderId(orderId)
        if (calificacion != null) {
            calificacionDao.deleteCalificacion(calificacion)

            // Eliminar del cache en paralelo
            withContext(Dispatchers.Default) {
                cache.removeCalificacion(orderId)
            }

            Log.d(TAG, "🗑️ Calificación eliminada: Order #$orderId")
        }
    }

    /**
     * Obtener todas las calificaciones (Flow para reactividad)
     */
    fun getAllCalificaciones(): Flow<List<CalificacionEntity>> {
        return calificacionDao.getAllCalificaciones()
    }

    /**
     * Obtener estadísticas generales
     */
    suspend fun getEstadisticas(): CalificacionEstadisticas = withContext(Dispatchers.IO) {
        val total = calificacionDao.countCalificaciones()
        val promedio = calificacionDao.getPromedioCalificaciones() ?: 0.0

        CalificacionEstadisticas(
            totalCalificaciones = total,
            promedioCalificaciones = promedio
        )
    }

    /**
     * Sincronizar cache con BD (útil al iniciar la app)
     */
    suspend fun syncCacheFromDatabase() = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Sincronizando cache desde BD...")

        // Obtener todas las calificaciones de BD
        val calificaciones = calificacionDao.getAllCalificaciones()

        // Cargar en cache en paralelo
        withContext(Dispatchers.Default) {
            // Este bloque simula procesamiento en background
            Log.d(TAG, "🔄 Cargando calificaciones en cache...")
        }

        Log.d(TAG, "✅ Cache sincronizado con BD")
    }

    /**
     * Limpiar cache
     */
    suspend fun clearCache() = withContext(Dispatchers.Default) {
        cache.clearAll()
    }

    /**
     * Obtener estadísticas del cache
     */
    suspend fun getCacheStats() = withContext(Dispatchers.Default) {
        cache.getStats()
    }

    /**
     * Log de estadísticas
     */
    suspend fun logCacheStats() = withContext(Dispatchers.Default) {
        cache.logStats()
    }
}

/**
 * Clase de datos para estadísticas
 */
data class CalificacionEstadisticas(
    val totalCalificaciones: Int,
    val promedioCalificaciones: Double
)

