package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.api.ApiClient
import app.src.data.models.ProductoConConversiones
import app.src.utils.ConversionCacheManager
import app.src.utils.NetworkUtils

class ConversionesRepository {

    private val productoService = ApiClient.productoService
    private val TAG = "ConversionesRepository"

    suspend fun obtenerConversiones(
        productoId: Int,
        context: Context
    ): Result<ProductoConConversiones> {
        return try {
            // 1. PRIMERO: Verificar si hay datos en caché válidos (independiente de internet)
            val cachedData = ConversionCacheManager.getConversion(context, productoId)

            if (cachedData != null) {
                val (data, isValid) = cachedData

                if (isValid) {
                    // El caché es válido (menos de 24 horas) - USAR CACHÉ
                    Log.d(TAG, "📦 Usando caché válido (no expirado) - Evitando llamada a API")
                    return Result.Success(data, isFromCache = true, isCacheExpired = false)
                } else {
                    Log.d(TAG, "⚠️ Caché expirado (más de 24 horas), intentando actualizar desde API...")
                }
            } else {
                Log.d(TAG, "📭 No hay caché disponible, obteniendo desde API...")
            }

            // 2. Si el caché no existe o está expirado, verificar conexión a internet
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // 3. Hay internet: intentar obtener datos frescos de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo datos frescos de la API...")

                try {
                    val response = productoService.obtenerConversionesProducto(productoId)

                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!

                        // 4. Guardar en caché para uso futuro
                        ConversionCacheManager.saveConversion(context, productoId, data)

                        Log.d(TAG, "✅ Datos obtenidos de API y guardados en caché")
                        Result.Success(data, isFromCache = false, isCacheExpired = false)
                    } else {
                        // Si la API falla, intentar usar caché aunque esté expirado
                        Log.w(TAG, "⚠️ API respondió con error, usando caché expirado como respaldo...")
                        usarCacheExpiradoComoUltimoRecurso(cachedData)
                    }
                } catch (e: Exception) {
                    // Si hay excepción en la API, intentar usar caché aunque esté expirado
                    Log.e(TAG, "❌ Error en API: ${e.message}, usando caché expirado como respaldo...")
                    usarCacheExpiradoComoUltimoRecurso(cachedData)
                }
            } else {
                // 5. No hay internet: usar caché aunque esté expirado
                Log.d(TAG, "📵 Sin internet, usando caché como respaldo...")
                usarCacheExpiradoComoUltimoRecurso(cachedData)
            }
        } catch (e: Exception) {
            Result.Error("Error inesperado: ${e.message}")
        }
    }

    /**
     * Usa el caché expirado como último recurso cuando no hay otra opción
     */
    private fun usarCacheExpiradoComoUltimoRecurso(
        cachedData: Pair<ProductoConConversiones, Boolean>?
    ): Result<ProductoConConversiones> {
        return if (cachedData != null) {
            val (data, _) = cachedData
            Log.d(TAG, "⚠️ Usando caché expirado como último recurso")
            Result.Success(data, isFromCache = true, isCacheExpired = true)
        } else {
            Log.e(TAG, "❌ No hay datos en caché disponibles")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    /**
     * Intenta usar el caché como respaldo cuando no hay internet o la API falla
     */
    private fun usarCacheComoFallback(context: Context, productoId: Int): Result<ProductoConConversiones> {
        val cachedData = ConversionCacheManager.getConversion(context, productoId)

        return if (cachedData != null) {
            val (data, isValid) = cachedData

            if (isValid) {
                Log.d(TAG, "✅ Usando caché válido (no expirado)")
            } else {
                Log.d(TAG, "⚠️ Usando caché expirado (sin internet disponible)")
            }

            Result.Success(data, isFromCache = true, isCacheExpired = !isValid)
        } else {
            Log.e(TAG, "❌ No hay datos en caché disponibles")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }
}
