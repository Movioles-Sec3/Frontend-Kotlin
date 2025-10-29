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
        val cacheManager = LruCacheManager.getInstance(context)
        val cacheKey = cacheManager.generateProductosKey(idTipo, disponible)

        // 1. Verificar conectividad de red
        val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

        if (!hasInternet) {
            // NO HAY INTERNET: Usar cache directamente
            Log.d(TAG, "📵 Sin internet, usando productos del caché...")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        }

        // 2. HAY INTERNET: Intentar obtener de la API
        Log.d(TAG, "🌐 Internet disponible, obteniendo productos frescos de la API...")

        try {
            val response = api.listarProductos(idTipo, disponible)
            if (response.isSuccessful && response.body() != null) {
                val productos = response.body()!!

                // Guardar en LRU cache
                cacheManager.putProductos(cacheKey, productos)

                Log.d(TAG, "✅ ${productos.size} productos obtenidos de API y guardados en LRU cache")
                return Result.Success(productos, isFromCache = false, isCacheExpired = false)
            } else {
                // API respondió pero con error
                Log.w(TAG, "⚠️ API respondió con error (${response.code()}), usando caché como respaldo")
                return usarCacheComoRespaldo(cacheManager, cacheKey)
            }
        } catch (e: java.net.SocketTimeoutException) {
            // Timeout - backend probablemente apagado
            Log.w(TAG, "⏱️ Timeout al conectar con el backend, usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        } catch (e: java.net.ConnectException) {
            // Conexión rechazada - backend definitivamente apagado
            Log.w(TAG, "🔌 Backend no disponible (conexión rechazada), usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        } catch (e: java.io.IOException) {
            // Error de I/O (red inestable, etc.)
            Log.w(TAG, "📡 Error de red: ${e.message}, usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        } catch (e: Exception) {
            // Cualquier otro error
            Log.e(TAG, "❌ Error inesperado: ${e.message}, usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
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
        val cacheManager = LruCacheManager.getInstance(context)

        // 1. Verificar conectividad de red
        val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

        if (!hasInternet) {
            // NO HAY INTERNET: Usar cache directamente
            Log.d(TAG, "📵 Sin internet, usando producto $productoId del caché...")
            return usarProductoCacheComoRespaldo(cacheManager, productoId)
        }

        // 2. HAY INTERNET: Intentar obtener de la API
        Log.d(TAG, "🌐 Internet disponible, obteniendo producto $productoId de la API...")

        try {
            val response = api.obtenerProducto(productoId)
            if (response.isSuccessful && response.body() != null) {
                val producto = response.body()!!

                // Guardar en LRU cache
                cacheManager.putProducto(productoId, producto)

                Log.d(TAG, "✅ Producto $productoId obtenido de API y guardado en LRU cache")
                return Result.Success(producto, isFromCache = false, isCacheExpired = false)
            } else {
                Log.w(TAG, "⚠️ API respondió con error (${response.code()}), usando caché como respaldo")
                return usarProductoCacheComoRespaldo(cacheManager, productoId)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "⏱️ Timeout al conectar con el backend, usando caché como respaldo")
            return usarProductoCacheComoRespaldo(cacheManager, productoId)
        } catch (e: java.net.ConnectException) {
            Log.w(TAG, "🔌 Backend no disponible (conexión rechazada), usando caché como respaldo")
            return usarProductoCacheComoRespaldo(cacheManager, productoId)
        } catch (e: java.io.IOException) {
            Log.w(TAG, "📡 Error de red: ${e.message}, usando caché como respaldo")
            return usarProductoCacheComoRespaldo(cacheManager, productoId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado: ${e.message}, usando caché como respaldo")
            return usarProductoCacheComoRespaldo(cacheManager, productoId)
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
        val cacheManager = LruCacheManager.getInstance(context)

        // 1. Verificar conectividad de red
        val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

        if (!hasInternet) {
            // NO HAY INTERNET: Usar cache directamente
            Log.d(TAG, "📵 Sin internet, usando tipos del caché...")
            return usarTiposCacheComoRespaldo(cacheManager)
        }

        // 2. HAY INTERNET: Intentar obtener de la API
        Log.d(TAG, "🌐 Internet disponible, obteniendo tipos de la API...")

        try {
            val response = api.listarTipos()
            if (response.isSuccessful && response.body() != null) {
                val tipos = response.body()!!

                // Guardar en LRU cache
                cacheManager.putTipos(tipos)

                Log.d(TAG, "✅ ${tipos.size} tipos obtenidos de API y guardados en LRU cache")
                return Result.Success(tipos, isFromCache = false, isCacheExpired = false)
            } else {
                Log.w(TAG, "⚠️ API respondió con error (${response.code()}), usando caché como respaldo")
                return usarTiposCacheComoRespaldo(cacheManager)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "⏱️ Timeout al conectar con el backend, usando caché como respaldo")
            return usarTiposCacheComoRespaldo(cacheManager)
        } catch (e: java.net.ConnectException) {
            Log.w(TAG, "🔌 Backend no disponible (conexión rechazada), usando caché como respaldo")
            return usarTiposCacheComoRespaldo(cacheManager)
        } catch (e: java.io.IOException) {
            Log.w(TAG, "📡 Error de red: ${e.message}, usando caché como respaldo")
            return usarTiposCacheComoRespaldo(cacheManager)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado: ${e.message}, usando caché como respaldo")
            return usarTiposCacheComoRespaldo(cacheManager)
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
        val cacheManager = LruCacheManager.getInstance(context)
        val cacheKey = "productos_recomendados"

        // 1. Verificar conectividad de red
        val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

        if (!hasInternet) {
            // NO HAY INTERNET: Usar cache directamente
            Log.d(TAG, "📵 Sin internet, usando productos recomendados del caché...")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        }

        // 2. HAY INTERNET: Intentar obtener de la API
        Log.d(TAG, "🌐 Internet disponible, obteniendo productos recomendados de la API...")

        try {
            val response = api.obtenerProductosRecomendados()
            if (response.isSuccessful && response.body() != null) {
                val productos = response.body()!!

                // Guardar en LRU cache
                cacheManager.putProductos(cacheKey, productos)

                Log.d(TAG, "✅ ${productos.size} productos recomendados obtenidos de API y guardados en LRU cache")
                return Result.Success(productos, isFromCache = false, isCacheExpired = false)
            } else {
                // API respondió pero con error
                Log.w(TAG, "⚠️ API respondió con error (${response.code()}), usando caché como respaldo")
                return usarCacheComoRespaldo(cacheManager, cacheKey)
            }
        } catch (e: java.net.SocketTimeoutException) {
            // Timeout - backend probablemente apagado
            Log.w(TAG, "⏱️ Timeout al conectar con el backend, usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        } catch (e: java.net.ConnectException) {
            // Conexión rechazada - backend definitivamente apagado
            Log.w(TAG, "🔌 Backend no disponible (conexión rechazada), usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        } catch (e: java.io.IOException) {
            // Error de I/O (red inestable, etc.)
            Log.w(TAG, "📡 Error de red: ${e.message}, usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
        } catch (e: Exception) {
            // Cualquier otro error
            Log.e(TAG, "❌ Error inesperado: ${e.message}, usando caché como respaldo")
            return usarCacheComoRespaldo(cacheManager, cacheKey)
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
