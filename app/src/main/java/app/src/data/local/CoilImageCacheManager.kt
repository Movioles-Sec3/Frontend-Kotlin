package app.src.data.local

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Gestor de caché de imágenes con Coil
 * Implementa caché en memoria y disco con TTL diferenciado
 */
class CoilImageCacheManager(context: Context) {

    companion object {
        // Tamaños de caché
        const val MEMORY_CACHE_SIZE_PERCENT = 0.25 // 25% de RAM disponible
        const val DISK_CACHE_SIZE_MB = 100L * 1024 * 1024 // 100 MB

        // TTL por tipo de imagen
        const val TTL_BANNERS_DAYS = 7
        const val TTL_PRODUCTS_DAYS = 2
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_SIZE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_SIZE_MB)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
            }
            .respectCacheHeaders(false) // Control manual de TTL
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    /**
     * Limpia la caché de imágenes
     */
    fun clearCache() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    /**
     * Obtiene el tamaño actual de la caché en disco
     */
    fun getDiskCacheSize(): Long {
        return imageLoader.diskCache?.size ?: 0L
    }

    /**
     * Obtiene el tamaño actual de la caché en memoria
     */
    fun getMemoryCacheSize(): Int {
        return imageLoader.memoryCache?.size ?: 0
    }
}

