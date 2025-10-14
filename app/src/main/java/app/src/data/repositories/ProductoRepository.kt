package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.api.ApiClient
import app.src.data.models.*
import app.src.utils.NetworkUtils
import app.src.utils.ProductoCacheManager
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
            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // 2. HAY INTERNET: Siempre obtener datos frescos de la API (NO usar caché)
                Log.d(TAG, "🌐 Internet disponible, obteniendo productos frescos de la API...")

                try {
                    val response = api.listarProductos(idTipo, disponible)
                    if (response.isSuccessful && response.body() != null) {
                        val productos = response.body()!!

                        // Generar key y guardar en caché para uso futuro (cuando no haya internet)
                        val cacheKey = ProductoCacheManager.generateKey(idTipo, disponible)
                        ProductoCacheManager.saveProductos(context, cacheKey, productos)

                        Log.d(TAG, "✅ ${productos.size} productos obtenidos de API y guardados en caché")
                        Result.Success(productos, isFromCache = false, isCacheExpired = false)
                    } else {
                        // API falló, intentar usar caché como respaldo
                        Log.w(TAG, "⚠️ API respondió con error, intentando usar caché como respaldo...")
                        val cacheKey = ProductoCacheManager.generateKey(idTipo, disponible)
                        usarCacheComoRespaldo(context, cacheKey)
                    }
                } catch (e: Exception) {
                    // Error en API, intentar usar caché
                    Log.e(TAG, "❌ Error en API: ${e.message}, intentando usar caché...")
                    val cacheKey = ProductoCacheManager.generateKey(idTipo, disponible)
                    usarCacheComoRespaldo(context, cacheKey)
                }
            } else {
                // 3. NO HAY INTERNET: Usar caché como respaldo
                Log.d(TAG, "📵 Sin internet, buscando productos en caché...")
                val cacheKey = ProductoCacheManager.generateKey(idTipo, disponible)
                usarCacheComoRespaldo(context, cacheKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarCacheComoRespaldo(context: Context, cacheKey: String): Result<List<Producto>> {
        val cachedData = ProductoCacheManager.getProductos(context, cacheKey)

        return if (cachedData != null) {
            val (productos, isValid) = cachedData

            if (isValid) {
                Log.d(TAG, "📦 Usando ${productos.size} productos del caché (válido)")
            } else {
                Log.d(TAG, "📦⚠️ Usando ${productos.size} productos del caché (expirado)")
            }

            Result.Success(productos, isFromCache = true, isCacheExpired = !isValid)
        } else {
            Log.e(TAG, "❌ No hay productos en caché disponibles")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    suspend fun obtenerProducto(context: Context, productoId: Int): Result<Producto> {
        return try {
            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Siempre obtener de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo producto $productoId de la API...")

                val response = api.obtenerProducto(productoId)
                if (response.isSuccessful && response.body() != null) {
                    val producto = response.body()!!

                    // Guardar en caché para uso futuro
                    ProductoCacheManager.saveProducto(context, productoId, producto)

                    Log.d(TAG, "✅ Producto $productoId obtenido de API y guardado en caché")
                    Result.Success(producto, isFromCache = false, isCacheExpired = false)
                } else {
                    usarProductoCacheComoRespaldo(context, productoId)
                }
            } else {
                // NO HAY INTERNET: Usar caché
                Log.d(TAG, "📵 Sin internet, buscando producto $productoId en caché...")
                usarProductoCacheComoRespaldo(context, productoId)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarProductoCacheComoRespaldo(context: Context, productoId: Int): Result<Producto> {
        val cachedData = ProductoCacheManager.getProducto(context, productoId)

        return if (cachedData != null) {
            val (producto, isValid) = cachedData
            Log.d(TAG, if (isValid) "📦 Usando producto del caché (válido)" else "📦⚠️ Usando producto del caché (expirado)")
            Result.Success(producto, isFromCache = true, isCacheExpired = !isValid)
        } else {
            Log.e(TAG, "❌ No hay producto en caché disponible")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    suspend fun listarTipos(context: Context): Result<List<TipoProducto>> {
        return try {
            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Siempre obtener de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo tipos de la API...")

                val response = api.listarTipos()
                if (response.isSuccessful && response.body() != null) {
                    val tipos = response.body()!!

                    // Guardar en caché para uso futuro
                    ProductoCacheManager.saveTipos(context, tipos)

                    Log.d(TAG, "✅ ${tipos.size} tipos obtenidos de API y guardados en caché")
                    Result.Success(tipos, isFromCache = false, isCacheExpired = false)
                } else {
                    usarTiposCacheComoRespaldo(context)
                }
            } else {
                // NO HAY INTERNET: Usar caché
                Log.d(TAG, "📵 Sin internet, buscando tipos en caché...")
                usarTiposCacheComoRespaldo(context)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun usarTiposCacheComoRespaldo(context: Context): Result<List<TipoProducto>> {
        val cachedData = ProductoCacheManager.getTipos(context)

        return if (cachedData != null) {
            val (tipos, isValid) = cachedData
            Log.d(TAG, if (isValid) "📦 Usando ${tipos.size} tipos del caché (válido)" else "📦⚠️ Usando ${tipos.size} tipos del caché (expirado)")
            Result.Success(tipos, isFromCache = true, isCacheExpired = !isValid)
        } else {
            Log.e(TAG, "❌ No hay tipos en caché disponibles")
            Result.Error("No hay conexión a internet y no hay datos en caché disponibles")
        }
    }

    suspend fun obtenerProductosRecomendados(context: Context): Result<List<Producto>> {
        return try {
            // 1. Verificar internet PRIMERO
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                // HAY INTERNET: Siempre obtener de la API
                Log.d(TAG, "🌐 Internet disponible, obteniendo productos recomendados de la API...")

                val response = api.obtenerProductosRecomendados()
                if (response.isSuccessful && response.body() != null) {
                    val productos = response.body()!!

                    // Guardar en caché para uso futuro
                    val cacheKey = "productos_recomendados"
                    ProductoCacheManager.saveProductos(context, cacheKey, productos)

                    Log.d(TAG, "✅ ${productos.size} productos recomendados obtenidos de API")
                    Result.Success(productos, isFromCache = false, isCacheExpired = false)
                } else {
                    val cacheKey = "productos_recomendados"
                    usarCacheComoRespaldo(context, cacheKey)
                }
            } else {
                // NO HAY INTERNET: Usar caché
                Log.d(TAG, "📵 Sin internet, buscando productos recomendados en caché...")
                val cacheKey = "productos_recomendados"
                usarCacheComoRespaldo(context, cacheKey)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
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
