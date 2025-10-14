package app.src.utils

import android.content.Context
import android.util.Log
import app.src.data.models.ProductoConConversiones
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ConversionCacheManager {

    private const val PREF_NAME = "ConversionCache"
    private const val TAG = "ConversionCache"

    // Tiempo de expiración del caché: 24 horas en milisegundos
    private const val CACHE_EXPIRATION_TIME = 24 * 60 * 60 * 1000L

    private val gson = Gson()

    /**
     * Guarda una conversión en caché
     */
    fun saveConversion(context: Context, productoId: Int, data: ProductoConConversiones) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = "conversion_$productoId"

            val cacheEntry = CacheEntry(
                data = data,
                timestamp = System.currentTimeMillis()
            )

            val json = gson.toJson(cacheEntry)
            prefs.edit().putString(key, json).apply()

            Log.d(TAG, "✅ Conversión guardada en caché para producto $productoId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar conversión en caché: ${e.message}")
        }
    }

    /**
     * Obtiene una conversión del caché
     * @return Pair<ProductoConConversiones, Boolean> donde Boolean indica si el caché es válido (no expirado)
     */
    fun getConversion(context: Context, productoId: Int): Pair<ProductoConConversiones, Boolean>? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = "conversion_$productoId"

            val json = prefs.getString(key, null) ?: return null

            val type = object : TypeToken<CacheEntry>() {}.type
            val cacheEntry: CacheEntry = gson.fromJson(json, type)

            val isValid = isCacheValid(cacheEntry.timestamp)

            Log.d(TAG, "📦 Conversión obtenida del caché para producto $productoId (válida: $isValid)")

            return Pair(cacheEntry.data, isValid)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener conversión del caché: ${e.message}")
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
     * Limpia todo el caché de conversiones
     */
    fun clearCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "🗑️ Caché de conversiones limpiado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al limpiar caché: ${e.message}")
        }
    }

    /**
     * Limpia caché expirado
     */
    fun clearExpiredCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            var removedCount = 0

            val allEntries = prefs.all
            allEntries.forEach { (key, value) ->
                if (key.startsWith("conversion_") && value is String) {
                    try {
                        val type = object : TypeToken<CacheEntry>() {}.type
                        val cacheEntry: CacheEntry = gson.fromJson(value, type)

                        if (!isCacheValid(cacheEntry.timestamp)) {
                            editor.remove(key)
                            removedCount++
                        }
                    } catch (e: Exception) {
                        // Si hay error al parsear, eliminar la entrada corrupta
                        editor.remove(key)
                        removedCount++
                    }
                }
            }

            editor.apply()
            Log.d(TAG, "🗑️ Se eliminaron $removedCount entradas expiradas del caché")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al limpiar caché expirado: ${e.message}")
        }
    }

    /**
     * Obtiene el tamaño del caché (cantidad de conversiones guardadas)
     */
    fun getCacheSize(context: Context): Int {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.all.count { it.key.startsWith("conversion_") }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener tamaño del caché: ${e.message}")
            return 0
        }
    }

    /**
     * Clase interna para almacenar datos con timestamp
     */
    private data class CacheEntry(
        val data: ProductoConConversiones,
        val timestamp: Long
    )
}

