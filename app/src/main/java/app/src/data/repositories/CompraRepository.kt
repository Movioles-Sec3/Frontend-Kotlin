package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.api.ApiClient
import app.src.data.models.Compra
import app.src.data.models.CompraRequest
import app.src.data.models.ActualizarEstadoRequest
import app.src.data.models.EscanearQRRequest
import app.src.data.models.EscanearQRResponse
import app.src.utils.NetworkUtils
import app.src.utils.cache.LruCacheManager

class CompraRepository {

    private val api = ApiClient.compraService
    private val TAG = "CompraRepository"

    /**
     * Obtiene el historial de compras del usuario
     * Con caché LRU para mostrar offline
     */
    suspend fun obtenerHistorial(context: Context): Result<List<Compra>> {
        return try {
            val cacheManager = LruCacheManager.getInstance(context)

            // 1. Verificar internet
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Obtener de API y actualizar cache
                Log.d(TAG, "🌐 Internet disponible, obteniendo historial de la API...")

                try {
                    val response = api.historialCompras("Bearer ${ApiClient.getToken()}")
                    if (response.isSuccessful && response.body() != null) {
                        val compras = response.body()!!

                        // Guardar en LRU cache (incluye códigos QR)
                        cacheManager.putCompras(compras)

                        Log.d(TAG, "✅ ${compras.size} compras obtenidas de API y guardadas en LRU cache")
                        Result.Success(compras, isFromCache = false, isCacheExpired = false)
                    } else {
                        // API falló, usar cache
                        Log.w(TAG, "⚠️ API respondió con error, usando LRU cache...")
                        usarCacheComoRespaldo(cacheManager)
                    }
                } catch (e: Exception) {
                    // Error en API, usar cache
                    Log.e(TAG, "❌ Error en API: ${e.message}, usando LRU cache...")
                    usarCacheComoRespaldo(cacheManager)
                }
            } else {
                // NO HAY INTERNET: Usar cache
                Log.d(TAG, "📵 Sin internet, buscando historial en LRU cache...")
                usarCacheComoRespaldo(cacheManager)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarCacheComoRespaldo(cacheManager: LruCacheManager): Result<List<Compra>> {
        val cachedEntry = cacheManager.getCompras()

        return if (cachedEntry != null) {
            Log.d(TAG, "📦 Usando ${cachedEntry.data.size} compras del LRU cache (con códigos QR)")
            Result.Success(cachedEntry.data, isFromCache = true, isCacheExpired = false)
        } else {
            Log.e(TAG, "❌ No hay historial en LRU cache disponible")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    /**
     * Crea una nueva compra
     */
    suspend fun crearCompra(context: Context, compraRequest: CompraRequest): Result<Compra> {
        return try {
            // Las compras REQUIEREN internet
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.Error("No hay conexión a internet para procesar la compra")
            }

            val response = api.crearCompra("Bearer ${ApiClient.getToken()}", compraRequest)
            if (response.isSuccessful && response.body() != null) {
                val compra = response.body()!!

                // Invalidar cache de historial (hay nueva compra)
                val cacheManager = LruCacheManager.getInstance(context)
                cacheManager.getCompras() // Esto lo removerá si existe

                Log.d(TAG, "✅ Compra creada exitosamente: ID ${compra.id}")
                Result.Success(compra, isFromCache = false, isCacheExpired = false)
            } else {
                Result.Error("Error al crear compra: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al crear compra: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Actualiza el estado de una compra (requiere internet)
     */
    suspend fun actualizarEstado(
        context: Context,
        compraId: Int,
        nuevoEstado: String
    ): Result<Compra> {
        return try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.Error("No hay conexión a internet")
            }

            val response = api.actualizarEstadoCompra(
                compraId,
                ActualizarEstadoRequest(nuevoEstado)
            )

            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!, isFromCache = false, isCacheExpired = false)
            } else {
                Result.Error("Error al actualizar estado: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Escanea un código QR para validar y entregar un pedido
     * Solo funciona con internet (staff validation)
     */
    suspend fun escanearQR(codigoQR: String): Result<EscanearQRResponse> {
        return try {
            val response = api.escanearQR(EscanearQRRequest(codigoQR))

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ QR escaneado exitosamente")
                Result.Success(response.body()!!, isFromCache = false, isCacheExpired = false)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Código QR no válido o no encontrado"
                    400 -> "El código QR ya fue canjeado o no está listo"
                    else -> "Error al escanear QR: ${response.code()}"
                }
                Log.e(TAG, "❌ Error al escanear QR: ${response.code()}")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al escanear QR: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
