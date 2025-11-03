package app.src.utils

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import app.src.utils.cache.LruCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gestor centralizado de memoria
 * Responde a eventos de presión de memoria del sistema (onTrimMemory)
 */
object MemoryManager {

    private const val TAG = "MemoryManager"

    /**
     * Maneja eventos de presión de memoria
     * Llamar desde ComponentCallbacks2.onTrimMemory()
     */
    fun handleMemoryPressure(context: Context, level: Int) {
        when (level) {
            // Nivel crítico: la app está en background y el sistema está matando procesos
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "🔴 MEMORIA CRÍTICA - Liberando todo lo posible")
                clearAllCaches(context)
            }

            // Nivel moderado: la app está en background y la memoria es baja
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                Log.w(TAG, "🟡 MEMORIA MODERADA - Liberando cachés no esenciales")
                clearNonEssentialCaches(context)
            }

            // Nivel UI no visible: la app pasó a background
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "⚪ UI OCULTA - Limpieza ligera")
                clearExpiredData(context)
            }

            // Nivel running low: la app está en foreground pero la memoria es baja
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.w(TAG, "🟠 MEMORIA BAJA (foreground) - Limpiando datos expirados")
                clearExpiredData(context)
            }

            // Nivel running critical: la app está en foreground y la memoria es crítica
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.e(TAG, "🔴 MEMORIA CRÍTICA (foreground) - Liberación agresiva")
                clearNonEssentialCaches(context)
            }
        }
    }

    /**
     * Limpia TODOS los cachés (escenario crítico)
     */
    private fun clearAllCaches(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Limpiar LRU Cache en memoria
                LruCacheManager.getInstance(context).clearAll()

                // 2. Limpiar caché de imágenes
                ImagePreloader.clearCache()

                // 3. Limpiar Room y catálogo (usando reflexión para evitar dependencias circulares)
                try {
                    val catalogManager = Class.forName("app.src.data.local.CatalogCacheManager")
                        .getDeclaredMethod("getInstance", Context::class.java)
                        .invoke(null, context)

                    catalogManager?.javaClass?.getDeclaredMethod("clearAll")?.invoke(catalogManager)
                } catch (e: Exception) {
                    Log.d(TAG, "CatalogCacheManager no disponible: ${e.message}")
                }

                Log.d(TAG, "✅ Todos los cachés liberados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error liberando cachés: ${e.message}")
            }
        }
    }

    /**
     * Limpia cachés no esenciales (imágenes, datos expirados)
     */
    private fun clearNonEssentialCaches(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Limpiar solo parte del LRU Cache
                LruCacheManager.getInstance(context).trimMemory(50) // Liberar 50%

                // 2. Limpiar caché de imágenes
                ImagePreloader.clearCache()

                // 3. Limpiar solo páginas expiradas
                try {
                    val catalogManager = Class.forName("app.src.data.local.CatalogCacheManager")
                        .getDeclaredMethod("getInstance", Context::class.java)
                        .invoke(null, context)

                    catalogManager?.javaClass?.getDeclaredMethod("cleanExpiredPages")?.invoke(catalogManager)
                } catch (e: Exception) {
                    Log.d(TAG, "CatalogCacheManager no disponible: ${e.message}")
                }

                Log.d(TAG, "✅ Cachés no esenciales liberados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error liberando cachés no esenciales: ${e.message}")
            }
        }
    }

    /**
     * Limpia solo datos expirados (limpieza suave)
     */
    private fun clearExpiredData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Limpiar páginas expiradas del catálogo
                try {
                    val catalogManager = Class.forName("app.src.data.local.CatalogCacheManager")
                        .getDeclaredMethod("getInstance", Context::class.java)
                        .invoke(null, context)

                    catalogManager?.javaClass?.getDeclaredMethod("cleanExpiredPages")?.invoke(catalogManager)
                } catch (e: Exception) {
                    Log.d(TAG, "CatalogCacheManager no disponible: ${e.message}")
                }

                // Limpiar órdenes antiguas (más de 30 días)
                try {
                    val appDatabase = Class.forName("app.src.data.local.AppDatabase")
                        .getDeclaredMethod("getInstance", Context::class.java)
                        .invoke(null, context)

                    val orderDao = appDatabase?.javaClass?.getDeclaredMethod("orderDao")?.invoke(appDatabase)

                    val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                    orderDao?.javaClass?.getDeclaredMethod("deleteOldOrders", Long::class.java)
                        ?.invoke(orderDao, thirtyDaysAgo)
                } catch (e: Exception) {
                    Log.d(TAG, "AppDatabase no disponible: ${e.message}")
                }

                Log.d(TAG, "✅ Datos expirados eliminados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error eliminando datos expirados: ${e.message}")
            }
        }
    }

    /**
     * Obtiene información de memoria disponible
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val runtime = Runtime.getRuntime()

        return MemoryInfo(
            totalRam = memoryInfo.totalMem / (1024 * 1024), // MB
            availableRam = memoryInfo.availMem / (1024 * 1024), // MB
            isLowMemory = memoryInfo.lowMemory,
            heapMax = runtime.maxMemory() / (1024 * 1024), // MB
            heapTotal = runtime.totalMemory() / (1024 * 1024), // MB
            heapFree = runtime.freeMemory() / (1024 * 1024) // MB
        )
    }

    /**
     * Verifica si hay suficiente memoria disponible
     */
    fun hasEnoughMemory(context: Context): Boolean {
        val info = getMemoryInfo(context)
        return !info.isLowMemory && info.availableRam > 100 // Más de 100MB disponibles
    }
}

data class MemoryInfo(
    val totalRam: Long,
    val availableRam: Long,
    val isLowMemory: Boolean,
    val heapMax: Long,
    val heapTotal: Long,
    val heapFree: Long
) {
    val usedHeap: Long get() = heapTotal - heapFree
    val heapUsagePercent: Int get() = ((usedHeap.toFloat() / heapMax) * 100).toInt()
}
