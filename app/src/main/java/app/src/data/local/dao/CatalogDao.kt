package app.src.data.local.dao

import androidx.room.*
import app.src.data.local.entities.CatalogPageEntity

@Dao
interface CatalogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: CatalogPageEntity)

    @Query("SELECT * FROM catalog_pages WHERE key = :key AND expiresAt > :currentTime")
    suspend fun getValidPage(key: String, currentTime: Long): CatalogPageEntity?

    @Query("SELECT * FROM catalog_pages WHERE key = :key")
    suspend fun getPage(key: String): CatalogPageEntity?

    @Query("DELETE FROM catalog_pages WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long)

    @Query("DELETE FROM catalog_pages WHERE key LIKE :pattern")
    suspend fun deleteByPattern(pattern: String)

    @Query("DELETE FROM catalog_pages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM catalog_pages WHERE expiresAt > :currentTime")
    suspend fun getValidCacheCount(currentTime: Long): Int
}

