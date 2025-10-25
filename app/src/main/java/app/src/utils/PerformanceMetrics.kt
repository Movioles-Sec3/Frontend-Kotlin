package app.src.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sistema de medición de rendimiento para comparar carga paralela vs. secuencial
 * Calcula percentiles p50 (mediana) y p95 para analizar el rendimiento
 */
object PerformanceMetrics {
    private const val TAG = "PerformanceMetrics"
    private const val PREFS_NAME = "performance_metrics"
    private const val MAX_SAMPLES = 100 // Mantener últimas 100 mediciones

    // Mutex para garantizar thread-safety
    private val mutex = Mutex()

    // Tipos de carga
    enum class LoadType {
        PARALLEL,      // Catálogo e imágenes en paralelo
        SEQUENTIAL     // Catálogo primero, luego imágenes
    }

    // Datos de una medición
    data class LoadMeasurement(
        val timestamp: Long,
        val loadType: LoadType,
        val catalogLoadTime: Long,      // Tiempo de carga del catálogo (ms)
        val imagesLoadTime: Long,       // Tiempo de carga de imágenes (ms)
        val totalTime: Long,             // Tiempo total (ms)
        val menuReadyTime: Long,         // Tiempo hasta menú usable (ms)
        val productCount: Int,           // Número de productos cargados
        val networkType: String,         // Wi-Fi, 4G, 5G, etc.
        val deviceTier: String           // low, mid, high
    )

    // Estadísticas calculadas
    data class PerformanceStats(
        val loadType: LoadType,
        val sampleCount: Int,
        val p50: Double,      // Percentil 50 (mediana)
        val p95: Double,      // Percentil 95
        val min: Long,        // Tiempo mínimo
        val max: Long,        // Tiempo máximo
        val avg: Double,      // Promedio
        val improvement: Double? = null  // Mejora respecto al otro método (%)
    )

    /**
     * Registra una medición de rendimiento
     */
    suspend fun recordMeasurement(
        context: Context,
        loadType: LoadType,
        catalogLoadTime: Long,
        imagesLoadTime: Long,
        totalTime: Long,
        menuReadyTime: Long,
        productCount: Int,
        networkType: String,
        deviceTier: String
    ) {
        mutex.withLock {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val key = "${loadType.name}_measurements"

                // Obtener mediciones existentes
                val existing = getMeasurementsInternal(prefs, key)

                // Crear nueva medición
                val measurement = LoadMeasurement(
                    timestamp = System.currentTimeMillis(),
                    loadType = loadType,
                    catalogLoadTime = catalogLoadTime,
                    imagesLoadTime = imagesLoadTime,
                    totalTime = totalTime,
                    menuReadyTime = menuReadyTime,
                    productCount = productCount,
                    networkType = networkType,
                    deviceTier = deviceTier
                )

                // Agregar y mantener solo las últimas MAX_SAMPLES
                val updated = (existing + measurement).takeLast(MAX_SAMPLES)

                // Guardar
                saveMeasurementsInternal(prefs, key, updated)

                Log.d(TAG, """
                    📊 Nueva medición registrada:
                    - Tipo: $loadType
                    - Catálogo: ${catalogLoadTime}ms
                    - Imágenes: ${imagesLoadTime}ms
                    - Total: ${totalTime}ms
                    - Menú listo: ${menuReadyTime}ms
                    - Productos: $productCount
                    - Red: $networkType
                    - Dispositivo: $deviceTier
                """.trimIndent())

                // Enviar a Analytics/Firebase
                AnalyticsLogger.logPerformanceMetric(
                    context = context,
                    metricName = "menu_load_performance",
                    params = mapOf(
                        "load_type" to loadType.name,
                        "catalog_time_ms" to catalogLoadTime,
                        "images_time_ms" to imagesLoadTime,
                        "total_time_ms" to totalTime,
                        "menu_ready_time_ms" to menuReadyTime,
                        "product_count" to productCount,
                        "network_type" to networkType,
                        "device_tier" to deviceTier
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar medición: ${e.message}")
            }
        }
    }

    /**
     * Obtiene estadísticas para un tipo de carga
     */
    suspend fun getStats(context: Context, loadType: LoadType): PerformanceStats? {
        return mutex.withLock {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val key = "${loadType.name}_measurements"
                val measurements = getMeasurementsInternal(prefs, key)

                if (measurements.isEmpty()) return@withLock null

                // Ordenar por tiempo de menú listo
                val times = measurements.map { it.menuReadyTime }.sorted()

                PerformanceStats(
                    loadType = loadType,
                    sampleCount = measurements.size,
                    p50 = calculatePercentile(times, 50),
                    p95 = calculatePercentile(times, 95),
                    min = times.minOrNull() ?: 0,
                    max = times.maxOrNull() ?: 0,
                    avg = times.average()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener estadísticas: ${e.message}")
                null
            }
        }
    }

    /**
     * Compara el rendimiento de ambos métodos
     */
    suspend fun comparePerformance(context: Context): Pair<PerformanceStats?, PerformanceStats?> {
        val parallelStats = getStats(context, LoadType.PARALLEL)
        val sequentialStats = getStats(context, LoadType.SEQUENTIAL)

        // Calcular mejora si tenemos ambas estadísticas
        if (parallelStats != null && sequentialStats != null) {
            val improvement = ((sequentialStats.p50 - parallelStats.p50) / sequentialStats.p50) * 100

            return Pair(
                parallelStats.copy(improvement = improvement),
                sequentialStats.copy(improvement = -improvement)
            )
        }

        return Pair(parallelStats, sequentialStats)
    }

    /**
     * Obtiene todas las mediciones para un tipo de carga
     */
    suspend fun getAllMeasurements(context: Context, loadType: LoadType): List<LoadMeasurement> {
        return mutex.withLock {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val key = "${loadType.name}_measurements"
                getMeasurementsInternal(prefs, key)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener mediciones: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Genera un reporte de rendimiento legible
     */
    suspend fun generateReport(context: Context): String {
        val (parallel, sequential) = comparePerformance(context)

        val builder = StringBuilder()
        builder.appendLine("════════════════════════════════════════")
        builder.appendLine()

        if (parallel != null) {
            builder.appendLine("🚀 CARGA PARALELA")
            builder.appendLine("Muestras: ${parallel.sampleCount}")
            builder.appendLine("P50: ${parallel.p50.toLong()}ms | P95: ${parallel.p95.toLong()}ms")
            builder.appendLine("Min: ${parallel.min}ms | Max: ${parallel.max}ms")
            builder.appendLine()
        }

        if (sequential != null) {
            builder.appendLine("📦 CARGA SECUENCIAL")
            builder.appendLine("Muestras: ${sequential.sampleCount}")
            builder.appendLine("P50: ${sequential.p50.toLong()}ms | P95: ${sequential.p95.toLong()}ms")
            builder.appendLine("Min: ${sequential.min}ms | Max: ${sequential.max}ms")
            builder.appendLine()
        }

        if (parallel != null && sequential != null) {
            val timeSavedP50 = sequential.p50 - parallel.p50
            val timeSavedP95 = sequential.p95 - parallel.p95
            val percentageImprovement = (timeSavedP50 / sequential.p50) * 100

            builder.appendLine("════════════════════════════════════════")
            builder.appendLine()

            // Determinar cuál método es mejor
            if (timeSavedP50 > 0) {
                // Paralela es mejor
                builder.appendLine("✅ RESULTADO: La carga PARALELA es mejor")
                builder.appendLine()
                builder.appendLine("La carga paralela es ${String.format("%.1f", percentageImprovement)}% más rápida que la secuencial.")
                builder.appendLine()
                builder.appendLine("📊 Diferencias:")
                builder.appendLine("• P50: ${timeSavedP50.toLong()}ms más rápido")
                builder.appendLine("• P95: ${timeSavedP95.toLong()}ms más rápido")
                builder.appendLine()
                builder.appendLine("Esto significa que el catálogo e imágenes cargando al mismo tiempo aprovechan mejor tu conexión, haciendo que el menú esté listo más rápido para el usuario.")
            } else {
                // Secuencial es mejor
                val absTimeSavedP50 = kotlin.math.abs(timeSavedP50)
                val absPercentage = kotlin.math.abs(percentageImprovement)
                builder.appendLine("✅ RESULTADO: La carga SECUENCIAL es mejor")
                builder.appendLine()
                builder.appendLine("La carga secuencial es ${String.format("%.1f", absPercentage)}% más rápida que la paralela.")
                builder.appendLine()
                builder.appendLine("📊 Diferencias:")
                builder.appendLine("• P50: ${absTimeSavedP50.toLong()}ms más rápido")
                builder.appendLine("• P95: ${kotlin.math.abs(timeSavedP95).toLong()}ms más rápido")
                builder.appendLine()
                builder.appendLine("Esto puede ocurrir cuando el dispositivo tiene recursos limitados o conexión inestable. Cargar el catálogo primero y luego las imágenes evita saturar la conexión.")
            }
        } else {
            builder.appendLine("═══════════════════════════════════════════")
            builder.appendLine()
            builder.appendLine("⚠️ Se necesitan más muestras")
            builder.appendLine()
            builder.appendLine("Abre y cierra la app varias veces (10-20)")
            builder.appendLine("para recolectar datos de ambos métodos.")
            builder.appendLine()
            if (parallel == null) {
                builder.appendLine("Falta: Muestras de carga PARALELA")
            }
            if (sequential == null) {
                builder.appendLine("Falta: Muestras de carga SECUENCIAL")
            }
        }

        return builder.toString()
    }

    /**
     * Limpia todas las mediciones
     */
    suspend fun clearAllMeasurements(context: Context) {
        mutex.withLock {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                Log.d(TAG, "✅ Todas las mediciones fueron limpiadas")
            } catch (e: Exception) {
                Log.e(TAG, "Error al limpiar mediciones: ${e.message}")
            }
        }
    }

    // Funciones auxiliares privadas

    private fun calculatePercentile(sortedValues: List<Long>, percentile: Int): Double {
        if (sortedValues.isEmpty()) return 0.0

        val index = (percentile / 100.0) * (sortedValues.size - 1)
        val lower = sortedValues[index.toInt()]
        val upper = if (index.toInt() + 1 < sortedValues.size) {
            sortedValues[index.toInt() + 1]
        } else {
            lower
        }

        val fraction = index - index.toInt()
        return lower + (upper - lower) * fraction
    }

    private fun getMeasurementsInternal(
        prefs: android.content.SharedPreferences,
        key: String
    ): List<LoadMeasurement> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            deserializeMeasurements(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error al deserializar mediciones: ${e.message}")
            emptyList()
        }
    }

    private fun saveMeasurementsInternal(
        prefs: android.content.SharedPreferences,
        key: String,
        measurements: List<LoadMeasurement>
    ) {
        val json = serializeMeasurements(measurements)
        prefs.edit().putString(key, json).apply()
    }

    private fun serializeMeasurements(measurements: List<LoadMeasurement>): String {
        // Serialización simple con formato CSV
        return measurements.joinToString("|") { m ->
            "${m.timestamp},${m.loadType.name},${m.catalogLoadTime},${m.imagesLoadTime}," +
            "${m.totalTime},${m.menuReadyTime},${m.productCount},${m.networkType},${m.deviceTier}"
        }
    }

    private fun deserializeMeasurements(json: String): List<LoadMeasurement> {
        if (json.isBlank()) return emptyList()

        return json.split("|").mapNotNull { entry ->
            try {
                val parts = entry.split(",")
                LoadMeasurement(
                    timestamp = parts[0].toLong(),
                    loadType = LoadType.valueOf(parts[1]),
                    catalogLoadTime = parts[2].toLong(),
                    imagesLoadTime = parts[3].toLong(),
                    totalTime = parts[4].toLong(),
                    menuReadyTime = parts[5].toLong(),
                    productCount = parts[6].toInt(),
                    networkType = parts[7],
                    deviceTier = parts[8]
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear medición: ${e.message}")
                null
            }
        }
    }
}
