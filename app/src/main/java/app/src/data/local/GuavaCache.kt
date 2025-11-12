package app.src.data.local

import android.util.Log
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheStats
import com.google.common.cache.Cache
import java.util.concurrent.TimeUnit

/**
 * ✅ REQUERIMIENTO 1: Sistema de caché avanzado usando Guava Cache
 *
 * Guava Cache es una librería profesional de caché en memoria compatible con Android API 24+:
 * - Expiración automática basada en tiempo (TTL)
 * - Tamaño máximo configurable con política LRU automática
 * - Thread-safe sin necesidad de sincronización manual
 * - Estadísticas de rendimiento (hit rate, miss rate)
 * - Compatible con Android minSdk 24
 *
 * Casos de uso:
 * - Cachear productos recomendados con TTL de 5 minutos
 * - Cachear categorías con TTL de 10 minutos
 * - Cachear respuestas de API completas
 * - Cachear cálculos costosos
 */
object GuavaCache {

    private const val TAG = "GuavaCache"

    /**
     * Caché para productos recomendados
     * TTL: 5 minutos | Máximo: 100 entradas
     */
    private val recommendedProductsCache: Cache<String, Any> = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100)
        .recordStats()
        .build()

    /**
     * Caché para categorías de productos
     * TTL: 10 minutos | Máximo: 50 entradas
     */
    private val categoriesCache: Cache<String, Any> = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(50)
        .recordStats()
        .build()

    /**
     * Caché general para datos diversos
     * TTL: 3 minutos | Máximo: 200 entradas
     */
    private val generalCache: Cache<String, Any> = CacheBuilder.newBuilder()
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .maximumSize(200)
        .recordStats()
        .build()

    /**
     * Caché para cálculos pesados (sin expiración por tiempo, solo por tamaño)
     * Máximo: 500 entradas con política LRU automática
     */
    private val computationCache: Cache<String, Any> = CacheBuilder.newBuilder()
        .maximumSize(500)
        .recordStats()
        .build()

    // ==================== PRODUCTOS RECOMENDADOS ====================

    fun <T> putRecommended(key: String, value: T) {
        recommendedProductsCache.put(key, value as Any)
        Log.d(TAG, "💾 [Recommended] Guardado: $key")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getRecommended(key: String): T? {
        val value = recommendedProductsCache.getIfPresent(key)
        if (value != null) {
            Log.d(TAG, "✅ [Recommended] Hit: $key")
        } else {
            Log.d(TAG, "❌ [Recommended] Miss: $key")
        }
        return value as? T
    }

    // ==================== CATEGORÍAS ====================

    fun <T> putCategory(key: String, value: T) {
        categoriesCache.put(key, value as Any)
        Log.d(TAG, "💾 [Category] Guardado: $key")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCategory(key: String): T? {
        val value = categoriesCache.getIfPresent(key)
        if (value != null) {
            Log.d(TAG, "✅ [Category] Hit: $key")
        } else {
            Log.d(TAG, "❌ [Category] Miss: $key")
        }
        return value as? T
    }

    // ==================== CACHE GENERAL ====================

    fun <T> put(key: String, value: T) {
        generalCache.put(key, value as Any)
        Log.d(TAG, "💾 [General] Guardado: $key")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val value = generalCache.getIfPresent(key)
        if (value != null) {
            Log.d(TAG, "✅ [General] Hit: $key")
        } else {
            Log.d(TAG, "❌ [General] Miss: $key")
        }
        return value as? T
    }

    // ==================== CACHE DE CÁLCULOS ====================

    fun <T> putComputation(key: String, value: T) {
        computationCache.put(key, value as Any)
        Log.d(TAG, "💾 [Computation] Guardado: $key")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getComputation(key: String): T? {
        val value = computationCache.getIfPresent(key)
        if (value != null) {
            Log.d(TAG, "✅ [Computation] Hit: $key")
        } else {
            Log.d(TAG, "❌ [Computation] Miss: $key")
        }
        return value as? T
    }

    // ==================== ESTADÍSTICAS Y LIMPIEZA ====================

    fun getRecommendedStats(): String {
        val stats = recommendedProductsCache.stats()
        return formatStats(stats)
    }

    fun getCategoryStats(): String {
        val stats = categoriesCache.stats()
        return formatStats(stats)
    }

    fun getGeneralStats(): String {
        val stats = generalCache.stats()
        return formatStats(stats)
    }

    fun getComputationStats(): String {
        val stats = computationCache.stats()
        return formatStats(stats)
    }

    private fun formatStats(stats: CacheStats): String {
        val hitRate = stats.hitRate() * 100
        return "Hits: ${stats.hitCount()}, Misses: ${stats.missCount()}, Hit Rate: ${"%.2f".format(hitRate)}%"
    }

    fun logAllStats() {
        Log.d(TAG, "📊 ========== GUAVA CACHE STATS ==========")
        Log.d(TAG, "📦 Recommended: ${getRecommendedStats()}")
        Log.d(TAG, "📁 Categories: ${getCategoryStats()}")
        Log.d(TAG, "🗂️ General: ${getGeneralStats()}")
        Log.d(TAG, "🧮 Computation: ${getComputationStats()}")
        Log.d(TAG, "==========================================")
    }

    fun clearRecommended() {
        recommendedProductsCache.invalidateAll()
        Log.d(TAG, "🗑️ Caché de productos recomendados limpiado")
    }

    fun clearCategories() {
        categoriesCache.invalidateAll()
        Log.d(TAG, "🗑️ Caché de categorías limpiado")
    }

    fun clearGeneral() {
        generalCache.invalidateAll()
        Log.d(TAG, "🗑️ Caché general limpiado")
    }

    fun clearComputation() {
        computationCache.invalidateAll()
        Log.d(TAG, "🗑️ Caché de cálculos limpiado")
    }

    fun clearAll() {
        clearRecommended()
        clearCategories()
        clearGeneral()
        clearComputation()
        Log.d(TAG, "🗑️ Todos los cachés Guava limpiados")
    }
}

