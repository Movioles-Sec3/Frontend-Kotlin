package app.src.utils

import android.content.Context
import android.util.Log
import app.src.data.models.Producto
import app.src.data.models.TipoProducto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ProductoCacheManager {

    private const val PREF_NAME = "ProductoCache"
    private const val TAG = "ProductoCache"

    // Tiempo de expiración del caché: 1 hora (los productos cambian más seguido que conversiones)
    private const val CACHE_EXPIRATION_TIME = 60 * 60 * 1000L // 1 hora

    private val gson = Gson()

    /**
     * Guarda una lista de productos en caché
     */
    fun saveProductos(context: Context, key: String, productos: List<Producto>) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            val cacheEntry = CacheEntry(
                data = gson.toJson(productos),
                timestamp = System.currentTimeMillis()
            )

            val json = gson.toJson(cacheEntry)
            prefs.edit().putString(key, json).apply()

            Log.d(TAG, "✅ ${productos.size} productos guardados en caché con key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar productos en caché: ${e.message}")
        }
    }

    /**
     * Obtiene una lista de productos del caché
     * @return Pair<List<Producto>, Boolean> donde Boolean indica si el caché es válido
     */
    fun getProductos(context: Context, key: String): Pair<List<Producto>, Boolean>? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(key, null) ?: return null

            val cacheEntryType = object : TypeToken<CacheEntry>() {}.type
            val cacheEntry: CacheEntry = gson.fromJson(json, cacheEntryType)

            val productosType = object : TypeToken<List<Producto>>() {}.type
            val productos: List<Producto> = gson.fromJson(cacheEntry.data, productosType)

            val isValid = isCacheValid(cacheEntry.timestamp)

            Log.d(TAG, "📦 ${productos.size} productos obtenidos del caché con key: $key (válido: $isValid)")

            return Pair(productos, isValid)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener productos del caché: ${e.message}")
            return null
        }
    }

    /**
     * Guarda tipos de productos en caché
     */
    fun saveTipos(context: Context, tipos: List<TipoProducto>) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            val cacheEntry = CacheEntry(
                data = gson.toJson(tipos),
                timestamp = System.currentTimeMillis()
            )

            val json = gson.toJson(cacheEntry)
            prefs.edit().putString("tipos", json).apply()

            Log.d(TAG, "✅ ${tipos.size} tipos guardados en caché")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar tipos en caché: ${e.message}")
        }
    }

    /**
     * Obtiene tipos de productos del caché
     */
    fun getTipos(context: Context): Pair<List<TipoProducto>, Boolean>? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString("tipos", null) ?: return null

            val cacheEntryType = object : TypeToken<CacheEntry>() {}.type
            val cacheEntry: CacheEntry = gson.fromJson(json, cacheEntryType)

            val tiposType = object : TypeToken<List<TipoProducto>>() {}.type
            val tipos: List<TipoProducto> = gson.fromJson(cacheEntry.data, tiposType)

            val isValid = isCacheValid(cacheEntry.timestamp)

            Log.d(TAG, "📦 ${tipos.size} tipos obtenidos del caché (válido: $isValid)")

            return Pair(tipos, isValid)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener tipos del caché: ${e.message}")
            return null
        }
    }

    /**
     * Guarda un producto individual en caché
     */
    fun saveProducto(context: Context, productoId: Int, producto: Producto) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = "producto_$productoId"

            val cacheEntry = CacheEntry(
                data = gson.toJson(producto),
                timestamp = System.currentTimeMillis()
            )

            val json = gson.toJson(cacheEntry)
            prefs.edit().putString(key, json).apply()

            Log.d(TAG, "✅ Producto $productoId guardado en caché")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar producto en caché: ${e.message}")
        }
    }

    /**
     * Obtiene un producto individual del caché
     */
    fun getProducto(context: Context, productoId: Int): Pair<Producto, Boolean>? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = "producto_$productoId"
            val json = prefs.getString(key, null) ?: return null

            val cacheEntryType = object : TypeToken<CacheEntry>() {}.type
            val cacheEntry: CacheEntry = gson.fromJson(json, cacheEntryType)

            val producto: Producto = gson.fromJson(cacheEntry.data, Producto::class.java)

            val isValid = isCacheValid(cacheEntry.timestamp)

            Log.d(TAG, "📦 Producto $productoId obtenido del caché (válido: $isValid)")

            return Pair(producto, isValid)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener producto del caché: ${e.message}")
            return null
        }
    }

    /**
     * Verifica si el caché es válido (no ha expirado)
     */
    private fun isCacheValid(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - timestamp
        return cacheAge < CACHE_EXPIRATION_TIME
    }

    /**
     * Limpia todo el caché de productos
     */
    fun clearCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "🗑️ Caché de productos limpiado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al limpiar caché: ${e.message}")
        }
    }

    /**
     * Genera una key única para cachear listas de productos con filtros
     */
    fun generateKey(idTipo: Int?, disponible: Boolean?): String {
        return "productos_tipo_${idTipo ?: "all"}_disponible_${disponible ?: "all"}"
    }

    /**
     * Clase interna para almacenar datos con timestamp
     */
    private data class CacheEntry(
        val data: String,
        val timestamp: Long
    )
}

