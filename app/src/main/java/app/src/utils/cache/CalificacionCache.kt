package app.src.utils.cache

import android.util.Log
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

/**
 * Cache en memoria para calificaciones usando Guava Cache
 * Almacena calificaciones (rating 1-10) con política LRU
 * Funciona offline sin necesidad de red
 */
class CalificacionCache private constructor() {

    companion object {
        private const val TAG = "CalificacionCache"
        private const val MAX_ENTRIES = 200
        private const val TTL_MINUTES = 30L

        @Volatile
        private var INSTANCE: CalificacionCache? = null

        fun getInstance(): CalificacionCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CalificacionCache().also { INSTANCE = it }
            }
        }
    }

    // Cache para calificaciones: OrderId -> Rating (1-10)
    private val calificacionCache: Cache<Int, Int> = CacheBuilder.newBuilder()
        .maximumSize(MAX_ENTRIES.toLong())
        .expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES)
        .recordStats() // Para métricas
        .build()

    /**
     * Guardar calificación en cache
     */
    fun saveCalificacion(orderId: Int, rating: Int) {
        require(rating in 1..10) { "Rating debe estar entre 1 y 10" }
        calificacionCache.put(orderId, rating)
        Log.d(TAG, "💾 Calificación guardada en cache: Order #$orderId -> $rating/10")
    }

    /**
     * Obtener calificación del cache
     */
    fun getCalificacion(orderId: Int): Int? {
        val rating = calificacionCache.getIfPresent(orderId)
        if (rating != null) {
            Log.d(TAG, "✅ Cache HIT: Order #$orderId -> $rating/10")
        } else {
            Log.d(TAG, "❌ Cache MISS: Order #$orderId")
        }
        return rating
    }

    /**
     * Verificar si existe calificación en cache
     */
    fun hasCalificacion(orderId: Int): Boolean {
        return calificacionCache.getIfPresent(orderId) != null
    }

    /**
     * Eliminar calificación del cache
     */
    fun removeCalificacion(orderId: Int) {
        calificacionCache.invalidate(orderId)
        Log.d(TAG, "🗑️ Calificación eliminada del cache: Order #$orderId")
    }

    /**
     * Limpiar todo el cache
     */
    fun clearAll() {
        calificacionCache.invalidateAll()
        Log.d(TAG, "🧹 Cache de calificaciones limpiado completamente")
    }

    /**
     * Obtener estadísticas del cache
     */
    fun getStats(): CacheStats {
        val stats = calificacionCache.stats()
        return CacheStats(
            hits = stats.hitCount(),
            misses = stats.missCount(),
            evictionCount = stats.evictionCount(),
            currentSize = calificacionCache.size().toInt(),
            maxSize = MAX_ENTRIES
        )
    }

    /**
     * Obtener todas las calificaciones cacheadas
     */
    fun getAllCached(): Map<Int, Int> {
        return calificacionCache.asMap().toMap()
    }

    fun logStats() {
        val stats = getStats()
        Log.d(TAG, """
            📊 ========== CALIFICACION CACHE STATS ==========
            💾 Entries: ${stats.currentSize}
            ✅ Hits: ${stats.hits}
            ❌ Misses: ${stats.misses}
            📈 Hit Rate: ${"%.2f".format(stats.hitRate)}%
            ================================================
        """.trimIndent())
    }
}
