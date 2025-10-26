package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.api.ApiClient
import app.src.data.models.ProductoConConversiones
import app.src.utils.cache.LruCacheManager
import app.src.utils.NetworkUtils

class ConversionesRepository {

    private val productoService = ApiClient.productoService
    private val TAG = "ConversionesRepository"

    suspend fun obtenerConversiones(
        productoId: Int,
        context: Context
    ): Result<ProductoConConversiones> {
        return try {
            val cacheManager = LruCacheManager.getInstance(context)

            // 1. PRIMERO: Verificar si hay datos en LRU cache válidos (independiente de internet)
            val cachedEntry = cacheManager.getConversion(productoId)

            if (cachedEntry != null) {
                // El LRU cache tiene los datos y son válidos (no expirados)
                Log.d(TAG, "📦 Usando conversiones del LRU cache (válidas) - Evitando llamada a API")
                return Result.Success(cachedEntry.data, isFromCache = true, isCacheExpired = false)
            }

            Log.d(TAG, "📭 No hay conversiones en LRU cache, obteniendo desde API...")

            // 2. Si el cache no existe o está expirado, verificar conexión a internet
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // 3. Hay internet: intentar obtener datos frescos de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo datos frescos de la API...")

                try {
                    val response = productoService.obtenerConversionesProducto(productoId)

                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!

                        // 4. Guardar en LRU cache para uso futuro
                        cacheManager.putConversion(productoId, data)

                        Log.d(TAG, "✅ Conversiones obtenidas de API y guardadas en LRU cache")
                        Result.Success(data, isFromCache = false, isCacheExpired = false)
                    } else {
                        // Si la API falla, no hay cache para usar como respaldo
                        Log.e(TAG, "❌ API respondió con error y no hay cache disponible")
                        Result.Error("Error al obtener conversiones: ${response.code()}")
                    }
                } catch (e: Exception) {
                    // Si hay excepción en la API, no hay cache para usar
                    Log.e(TAG, "❌ Error en API: ${e.message}")
                    Result.Error("Error de conexión: ${e.message}")
                }
            } else {
                // 5. No hay internet y tampoco hay cache
                Log.e(TAG, "📵 Sin internet y sin datos en LRU cache disponibles")
                Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
            }
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}")
        }
    }
}
