package app.src.utils.cache

/**
 * CacheStats - Estadísticas de rendimiento del cache LRU
 *
 * Proporciona métricas para monitorear el comportamiento del cache
 * y evaluar su efectividad.
 *
 * MÉTRICAS INCLUIDAS:
 * -------------------
 * - hits: Número de veces que se encontró el dato en cache (éxito)
 * - misses: Número de veces que NO se encontró (falló)
 * - hitRate: Porcentaje de éxito (hits / total requests)
 * - evictionCount: Cuántas entradas fueron eliminadas por LRU policy
 * - currentSize: Tamaño actual del cache en KB
 * - maxSize: Tamaño máximo configurado en KB
 * - utilizationPercentage: Qué porcentaje del cache está en uso
 *
 * UTILIDAD:
 * ---------
 * Estas métricas permiten:
 * 1. Evaluar si el tamaño del cache es apropiado
 * 2. Detectar si hay muchos evictions (cache muy pequeño)
 * 3. Calcular el hit rate (buen cache > 70%)
 * 4. Optimizar TTL según comportamiento real
 */
data class CacheStats(
    val hits: Long,
    val misses: Long,
    val evictionCount: Long,
    val currentSize: Int,
    val maxSize: Int
) {
    /**
     * Calcula el hit rate (tasa de aciertos) como porcentaje
     * Hit rate alto (>70%) indica un cache efectivo
     */
    val hitRate: Float
        get() {
            val total = hits + misses
            return if (total > 0) (hits.toFloat() / total.toFloat()) * 100f else 0f
        }

    /**
     * Calcula el porcentaje de utilización del cache
     */
    val utilizationPercentage: Float
        get() = if (maxSize > 0) (currentSize.toFloat() / maxSize.toFloat()) * 100f else 0f

    /**
     * Genera un reporte legible de las estadísticas
     */
    fun generateReport(): String {
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("📊 LRU CACHE STATISTICS")
            appendLine("═══════════════════════════════════════")
            appendLine("Hits: $hits")
            appendLine("Misses: $misses")
            appendLine("Hit Rate: %.2f%%".format(hitRate))
            appendLine("Evictions: $evictionCount")
            appendLine("Current Size: ${currentSize / 1024} MB / ${maxSize / 1024} MB")
            appendLine("Utilization: %.2f%%".format(utilizationPercentage))
            appendLine("═══════════════════════════════════════")
        }
    }
}

