package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.api.ApiClient
import app.src.data.models.*
import app.src.utils.NetworkUtils
import app.src.utils.cache.LruCacheManager
import retrofit2.Response

class ProductoRepository {

    private val api = ApiClient.productoService
    private val TAG = "ProductoRepository"

    suspend fun listarProductos(
        context: Context,
        idTipo: Int? = null,
        disponible: Boolean? = true
    ): Result<List<Producto>> {
        return try {
            val cacheManager = LruCacheManager.getInstance(context)
            val cacheKey = cacheManager.generateProductosKey(idTipo, disponible)

            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // 2. HAY INTERNET: Siempre obtener datos frescos de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo productos frescos de la API...")

                try {
                    val response = api.listarProductos(idTipo, disponible)
                    if (response.isSuccessful && response.body() != null) {
                        val productos = response.body()!!

                        // Guardar en LRU cache
                        cacheManager.putProductos(cacheKey, productos)

                        Log.d(TAG, "✅ ${productos.size} productos obtenidos de API y guardados en LRU cache")
                        Result.Success(productos, isFromCache = false, isCacheExpired = false)
                    } else {
                        // API falló, intentar usar cache como respaldo
                        Log.w(TAG, "⚠️ API respondió con error, intentando usar LRU cache como respaldo...")
                        usarCacheComoRespaldo(cacheManager, cacheKey)
                    }
                } catch (e: Exception) {
                    // Error en API, intentar usar cache
                    Log.e(TAG, "❌ Error en API: ${e.message}, intentando usar LRU cache...")
                    usarCacheComoRespaldo(cacheManager, cacheKey)
                }
            } else {
                // 3. NO HAY INTERNET: Usar cache como respaldo
                Log.d(TAG, "📵 Sin internet, buscando productos en LRU cache...")
                usarCacheComoRespaldo(cacheManager, cacheKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarCacheComoRespaldo(cacheManager: LruCacheManager, cacheKey: String): Result<List<Producto>> {
        val cachedEntry = cacheManager.getProductos(cacheKey)

        return if (cachedEntry != null) {
            Log.d(TAG, "📦 Usando ${cachedEntry.data.size} productos del LRU cache")
            Result.Success(cachedEntry.data, isFromCache = true, isCacheExpired = false)
        } else {
            Log.e(TAG, "❌ No hay productos en LRU cache disponibles")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    suspend fun obtenerProducto(context: Context, productoId: Int): Result<Producto> {
        return try {
            val cacheManager = LruCacheManager.getInstance(context)

            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Siempre obtener de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo producto $productoId de la API...")

                val response = api.obtenerProducto(productoId)
                if (response.isSuccessful && response.body() != null) {
                    val producto = response.body()!!
                    // Sprint 4.2
                    // Guardar en LRU cache
                    cacheManager.putProducto(productoId, producto)

                    Log.d(TAG, "✅ Producto $productoId obtenido de API y guardado en LRU cache")
                    Result.Success(producto, isFromCache = false, isCacheExpired = false)
                } else {
                    usarProductoCacheComoRespaldo(cacheManager, productoId)
                }
            } else {
                // NO HAY INTERNET: Usar cache
                Log.d(TAG, "📵 Sin internet, buscando producto $productoId en LRU cache...")
                usarProductoCacheComoRespaldo(cacheManager, productoId)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarProductoCacheComoRespaldo(cacheManager: LruCacheManager, productoId: Int): Result<Producto> {
        val cachedEntry = cacheManager.getProducto(productoId)

        return if (cachedEntry != null) {
            Log.d(TAG, "📦 Usando producto del LRU cache")
            Result.Success(cachedEntry.data, isFromCache = true, isCacheExpired = false)
        } else {
            Log.e(TAG, "❌ No hay producto en LRU cache disponible")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    suspend fun listarTipos(context: Context): Result<List<TipoProducto>> {
        return try {
            val cacheManager = LruCacheManager.getInstance(context)

            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Siempre obtener de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo tipos de la API...")

                val response = api.listarTipos()
                if (response.isSuccessful && response.body() != null) {
                    val tipos = response.body()!!

                    // Guardar en LRU cache
                    cacheManager.putTipos(tipos)

                    Log.d(TAG, "✅ ${tipos.size} tipos obtenidos de API y guardados en LRU cache")
                    Result.Success(tipos, isFromCache = false, isCacheExpired = false)
                } else {
                    usarTiposCacheComoRespaldo(cacheManager)
                }
            } else {
                // NO HAY INTERNET: Usar cache
                Log.d(TAG, "📵 Sin internet, buscando tipos en LRU cache...")
                usarTiposCacheComoRespaldo(cacheManager)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarTiposCacheComoRespaldo(cacheManager: LruCacheManager): Result<List<TipoProducto>> {
        val cachedEntry = cacheManager.getTipos()

        return if (cachedEntry != null) {
            Log.d(TAG, "📦 Usando ${cachedEntry.data.size} tipos del LRU cache")
            Result.Success(cachedEntry.data, isFromCache = true, isCacheExpired = false)
        } else {
            Log.e(TAG, "❌ No hay tipos en LRU cache disponibles")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    suspend fun obtenerProductosRecomendados(context: Context): Result<List<Producto>> {
        return try {
            val cacheManager = LruCacheManager.getInstance(context)
            val cacheKey = "productos_recomendados"

            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Siempre obtener de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo productos recomendados de la API...")

                try {
                    val response = api.obtenerProductosRecomendados()
                    if (response.isSuccessful && response.body() != null) {
                        val productos = response.body()!!

                        // Guardar en LRU cache
                        cacheManager.putProductos(cacheKey, productos)

                        Log.d(TAG, "✅ ${productos.size} productos recomendados obtenidos de API y guardados en LRU cache")
                        Result.Success(productos, isFromCache = false, isCacheExpired = false)
                    } else {
                        Log.w(TAG, "⚠️ API respondió con código ${response.code()}, usando cache como respaldo")
                        usarCacheComoRespaldo(cacheManager, cacheKey)
                    }
                } catch (apiError: Exception) {
                    Log.e(TAG, "❌ Error llamando a la API: ${apiError.javaClass.simpleName}: ${apiError.message}")
                    apiError.printStackTrace()
                    usarCacheComoRespaldo(cacheManager, cacheKey)
                }
            } else {
                // NO HAY INTERNET: Usar cache
                Log.d(TAG, "📵 Sin internet, buscando productos recomendados en LRU cache...")
                usarCacheComoRespaldo(cacheManager, cacheKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error general: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Result.Error("Error al cargar productos: ${e.message}")
        }
    }

    private fun parseError(response: Response<*>): String {
        return when (response.code()) {
            400 -> "Datos inválidos"
            404 -> "Producto no encontrado"
            500 -> "Error del servidor"
            else -> "Error: ${response.code()}"
        }
    }
}
