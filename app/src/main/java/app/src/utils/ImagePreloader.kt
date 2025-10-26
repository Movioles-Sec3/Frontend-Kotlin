package app.src.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sistema de precarga de imágenes con caché en memoria
 * Soporta carga paralela y secuencial para comparar rendimiento
 *
 * ESTRATEGIAS DE GESTIÓN DE MEMORIA IMPLEMENTADAS:
 * 1. LRU Cache con límite adaptativo (12.5% de heap)
 * 2. Optimización de bitmaps con RGB_565 (reduce 50% de memoria)
 * 3. Reciclaje de bitmaps evictados
 * 4. Reducción de caché bajo presión (trimCache)
 */
object ImagePreloader {
    private const val TAG = "ImagePreloader"

    // Caché LRU para imágenes (usa 1/8 de la memoria disponible)
    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }

            /**
             * ═══════════════════════════════════════════════════════════════
             * ESTRATEGIA: RECICLAJE AUTOMÁTICO DE BITMAPS EVICTADOS
             * ═══════════════════════════════════════════════════════════════
             *
             * Cuando LruCache elimina un bitmap (por espacio), lo reciclamos
             *
             * ¿Por qué es importante?
             * - Bitmaps ocupan memoria nativa (fuera de heap Java)
             * - bitmap.recycle() libera la memoria nativa inmediatamente
             * - Sin recycle(): GC lo liberaría eventualmente (más lento)
             *
             * Cuándo se llama:
             * - evicted=true: LRU eliminó por falta de espacio
             * - evicted=false: Eliminación manual (clear, remove)
             */
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                if (evicted && !oldValue.isRecycled) {
                    oldValue.recycle()
                    Log.d(TAG, "♻️ Bitmap reciclado: $key")
                }
            }
        }
    }

    /**
     * Precarga imágenes en paralelo
     * Todas las imágenes se cargan simultáneamente
     */
    suspend fun preloadImagesParallel(imageUrls: List<String>): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            Log.d(TAG, "🚀 Iniciando precarga PARALELA de ${imageUrls.size} imágenes")

            // Lanzar todas las cargas en paralelo
            val jobs = imageUrls.map { url ->
                async {
                    loadAndCacheImage(url)
                }
            }

            // Esperar a que todas terminen
            jobs.awaitAll()

            val duration = System.currentTimeMillis() - startTime
            val successCount = jobs.count { it.getCompleted() != null }

            Log.d(TAG, "✅ Precarga PARALELA completada: $successCount/${imageUrls.size} imágenes en ${duration}ms")
            duration

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en precarga paralela: ${e.message}")
            System.currentTimeMillis() - startTime
        }
    }

    /**
     * Precarga imágenes en secuencia
     * Las imágenes se cargan una después de otra
     */
    suspend fun preloadImagesSequential(imageUrls: List<String>): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            Log.d(TAG, "📦 Iniciando precarga SECUENCIAL de ${imageUrls.size} imágenes")

            var successCount = 0
            for (url in imageUrls) {
                val bitmap = loadAndCacheImage(url)
                if (bitmap != null) successCount++
            }

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Precarga SECUENCIAL completada: $successCount/${imageUrls.size} imágenes en ${duration}ms")
            duration

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en precarga secuencial: ${e.message}")
            System.currentTimeMillis() - startTime
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * ESTRATEGIA: REDUCCIÓN DE CACHÉ BAJO PRESIÓN DE MEMORIA
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * Reduce el caché a un porcentaje del tamaño máximo
     *
     * @param percentage Porcentaje del caché a mantener (0-100)
     *
     * Ejemplo:
     * - Cache máximo: 8MB
     * - trimCache(50) → Reduce a 4MB
     * - Elimina las imágenes menos usadas (LRU)
     *
     * Cuándo se usa:
     * - TRIM_MEMORY_RUNNING_MODERATE: trimCache(50)
     * - Libera memoria preventivamente antes de llegar a crítico
     */
    fun trimCache(percentage: Int) {
        val targetSize = (memoryCache.maxSize() * percentage) / 100
        memoryCache.trimToSize(targetSize)
        Log.i(TAG, "🔄 Caché reducido a $percentage% (~${targetSize}KB)")
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * ESTRATEGIA: OPTIMIZACIÓN DE BITMAPS CON BitmapFactory.Options
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * Carga una imagen y la guarda en caché con optimizaciones de memoria
     *
     * Optimizaciones aplicadas:
     * 1. RGB_565 vs ARGB_8888
     *    - ARGB_8888: 4 bytes/pixel (canal alpha)
     *    - RGB_565: 2 bytes/pixel (sin alpha)
     *    - Ahorro: 50% de memoria
     *    - Trade-off: Menos colores (65K vs 16M)
     *    - Justificación: Fotos de productos no necesitan transparencia
     *
     * 2. inDither = true
     *    - Mejora calidad visual en RGB_565
     *    - Simula más colores mediante patrones
     *
     * 3. inScaled = true
     *    - Permite que Android ajuste densidad
     *    - Mejor compatibilidad entre dispositivos
     */
    private suspend fun loadAndCacheImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Verificar si ya está en caché
            memoryCache.get(imageUrl)?.let {
                Log.d(TAG, "💾 Imagen ya en caché: $imageUrl")
                return@withContext it
            }

            // Descargar imagen
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doInput = true
            connection.connect()

            val inputStream = connection.inputStream

            // OPTIMIZACIÓN: Configurar BitmapFactory.Options para reducir memoria
            val options = BitmapFactory.Options().apply {
                // RGB_565: 2 bytes/pixel (vs ARGB_8888: 4 bytes/pixel)
                inPreferredConfig = Bitmap.Config.RGB_565
                // Permitir scaling automático
                inScaled = true
                // Dithering para mejor calidad visual en RGB_565
                inDither = true
            }

            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            connection.disconnect()

            if (bitmap != null) {
                // Guardar en caché
                memoryCache.put(imageUrl, bitmap)
                val sizeKB = bitmap.byteCount / 1024
                Log.d(TAG, "✅ Imagen cargada: $imageUrl (${sizeKB}KB, ${bitmap.config})")
            } else {
                Log.w(TAG, "⚠️ No se pudo decodificar imagen: $imageUrl")
            }

            bitmap

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cargar imagen $imageUrl: ${e.message}")
            null
        }
    }

    /**
     * Obtiene una imagen del caché
     */
    fun getFromCache(imageUrl: String): Bitmap? {
        return memoryCache.get(imageUrl)
    }

    /**
     * Verifica si una imagen está en caché
     */
    fun isInCache(imageUrl: String): Boolean {
        return memoryCache.get(imageUrl) != null
    }

    /**
     * Limpia el caché de imágenes
     */
    fun clearCache() {
        memoryCache.evictAll()
        Log.d(TAG, "🗑️ Caché de imágenes limpiado")
    }

    /**
     * Obtiene el tamaño actual del caché
     */
    fun getCacheSize(): Int {
        return memoryCache.size()
    }

    /**
     * Obtiene información del caché
     */
    fun getCacheInfo(): String {
        return """
            Caché de imágenes:
            - Tamaño: ${memoryCache.size()} KB
            - Máximo: ${memoryCache.maxSize()} KB
            - Hits: ${memoryCache.hitCount()}
            - Misses: ${memoryCache.missCount()}
            - Hit rate: ${String.format("%.1f", memoryCache.hitCount() * 100.0 / (memoryCache.hitCount() + memoryCache.missCount()))}%
        """.trimIndent()
    }
}
