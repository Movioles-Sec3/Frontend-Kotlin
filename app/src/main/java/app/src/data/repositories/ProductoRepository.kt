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

    // NUEVO: Búsqueda de productos por texto.
    // Comportamiento:
    // - Si query está vacío, delega a listarProductos para mantener consistencia.
    // - Si hay internet: obtiene listado completo de la API y filtra por nombre/descripcion (case-insensitive), guarda el listado completo en cache.
    // - Si no hay internet o la API falla: intenta filtrar sobre las entradas disponibles en LRU cache (keys comunes) y devuelve coincidencias.
    suspend fun buscarProductos(context: Context, query: String): Result<List<Producto>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            // Delegar a listarProductos para comportamiento por defecto
            return listarProductos(context, null, true)
        }

        return try {
            val cacheManager = LruCacheManager.getInstance(context)
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode
            val lowerQuery = trimmed.lowercase()

            if (hasInternet) {
                try {
                    // Obtener lista completa desde API (sin filtros)
                    val response = api.listarProductos(null, true)
                    if (response.isSuccessful && response.body() != null) {
                        val productos: List<Producto> = response.body()!!

                        // Guardar listado completo en cache para búsquedas offline futuras
                        val cacheKey = cacheManager.generateProductosKey(null, true)
                        cacheManager.putProductos(cacheKey, productos)

                        // Filtrar por nombre o descripción (tipo explícito en lambda)
                        val filtered = productos.filter { p: Producto ->
                            p.nombre.lowercase().contains(lowerQuery) ||
                                    (p.descripcion?.lowercase()?.contains(lowerQuery) ?: false)
                        }

                        Log.d(TAG, "🔎 Búsqueda online: ${filtered.size} coincidencias para '$trimmed'")
                        return Result.Success(filtered, isFromCache = false, isCacheExpired = false)
                    } else {
                        Log.w(TAG, "⚠️ API search fallback: respuesta no exitosa, intentando usar cache")
                        // fallthrough a uso de cache
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error buscando en API: ${e.message}, usando cache como respaldo")
                    // fallthrough a uso de cache
                }
            }

            // Sin internet o la API falló -> buscar en cache
            val candidates = mutableListOf<Producto>()

            // Intentar keys comunes: all disponible + recomendados
            val keysToTry: List<String> = listOf(
                cacheManager.generateProductosKey(null, true),
                "productos_recomendados",
                cacheManager.generateProductosKey(null, null)
            )

            keysToTry.forEach { key: String ->
                try {
                    val entry = cacheManager.getProductos(key)
                    entry?.data?.let { list: List<Producto> ->
                        candidates.addAll(list)
                    }
                } catch (_: Exception) {
                    // ignorar
                }
            }

            // Eliminar duplicados por id (especificar tipo de retorno)
            val unique: List<Producto> = candidates.distinctBy { it.id }

            val filtered = unique.filter { p: Producto ->
                p.nombre.lowercase().contains(lowerQuery) ||
                        (p.descripcion?.lowercase()?.contains(lowerQuery) ?: false)
            }

            return if (filtered.isNotEmpty()) {
                Log.d(TAG, "🔎 Búsqueda offline: ${filtered.size} coincidencias para '$trimmed'")
                Result.Success(filtered, isFromCache = true, isCacheExpired = false)
            } else {
                Log.e(TAG, "🔎 Búsqueda offline: 0 coincidencias para '$trimmed'")
                Result.Error("No se encontraron productos para '$trimmed'")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en buscarProductos: ${e.message}")
            Result.Error(e.message ?: "Error en la búsqueda")
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
                    // Sprint 4.2.3
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
