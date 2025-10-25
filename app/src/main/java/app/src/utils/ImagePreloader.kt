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
     * Carga una imagen y la guarda en caché
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
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()

            if (bitmap != null) {
                // Guardar en caché
                memoryCache.put(imageUrl, bitmap)
                Log.d(TAG, "✅ Imagen cargada y cacheada: $imageUrl")
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

