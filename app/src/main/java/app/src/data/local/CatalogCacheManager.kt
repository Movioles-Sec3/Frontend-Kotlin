package app.src.data.local

import android.content.Context
import app.src.data.local.dao.CatalogDao
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de caché de catálogo con TTL (Time To Live)
 * Implementa estrategia de "read-before-network" con refresh en background
 */
class CatalogCacheManager(context: Context) {

    private val catalogDao: CatalogDao = AppDatabase.getDatabase(context).catalogDao()
    private val gson = Gson()

    companion object {
        // TTL en milisegundos
        const val TTL_HOME_FEED = 30 * 60 * 1000L // 30 minutos
        const val TTL_HOME_LAYOUT = 30 * 60 * 1000L
        const val TTL_RECOMMENDED = 30 * 60 * 1000L
        const val TTL_CATEGORY_LIST = 30 * 60 * 1000L
        const val TTL_CATEGORY_PRODUCTS = 10 * 60 * 1000L // 10 minutos

        // Prefijos de keys
        const val KEY_HOME_FEED = "home:feed:v1"
        const val KEY_HOME_LAYOUT = "home:layout:v1"
        const val KEY_HOME_RECOMMENDED = "home:recommended:v1"
        const val KEY_CATEGORIES_LIST = "categories:list:v1"

        fun keyCategoryProducts(categoryId: Int, page: Int) =
            "category:$categoryId:products:page:$page:v1"

        fun keyCategoryRecommended(categoryId: Int) =
            "category:$categoryId:recommended:v1"
    }

    // === GUARDAR EN CACHÉ ===
    suspend fun <T> saveToCache(key: String, data: T, ttlMs: Long) = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)
        val expiresAt = System.currentTimeMillis() + ttlMs
        val entity = app.src.data.local.entities.CatalogPageEntity(
            key = key,
            json = json,
            expiresAt = expiresAt
        )
        catalogDao.insert(entity)
    }

    // === OBTENER DE CACHÉ ===
    suspend fun <T> getFromCache(key: String, clazz: Class<T>): T? = withContext(Dispatchers.IO) {
        val entity = catalogDao.getValidPage(key, System.currentTimeMillis())
        entity?.let {
            try {
                gson.fromJson(it.json, clazz)
            } catch (e: Exception) {
                null
            }
        }
    }

    // === VERIFICAR SI ESTÁ EXPIRADO ===
    suspend fun isExpired(key: String): Boolean = withContext(Dispatchers.IO) {
        val entity = catalogDao.getPage(key)
        entity?.expiresAt?.let { it < System.currentTimeMillis() } ?: true
    }

    // === LIMPIAR CACHÉ ===
    suspend fun clearExpired() = withContext(Dispatchers.IO) {
        catalogDao.deleteExpired(System.currentTimeMillis())
    }

    suspend fun clearByCategory(categoryId: Int) = withContext(Dispatchers.IO) {
        catalogDao.deleteByPattern("category:$categoryId:%")
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        catalogDao.clearAll()
    }

    // === ESTADÍSTICAS ===
    suspend fun getValidCacheCount(): Int = withContext(Dispatchers.IO) {
        catalogDao.getValidCacheCount(System.currentTimeMillis())
    }
}

