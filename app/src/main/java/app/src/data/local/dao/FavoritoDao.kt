package app.src.data.local.dao

import androidx.room.*
import app.src.data.local.entities.FavoritoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD de favoritos
 * Usa Flow para observar cambios en tiempo real
 */
@Dao
interface FavoritoDao {

    /**
     * Obtiene todos los favoritos ordenados por fecha (más recientes primero)
     * Retorna Flow para actualizaciones automáticas en la UI
     */
    @Query("SELECT * FROM favoritos ORDER BY fechaAgregado DESC")
    fun getAllFavoritos(): Flow<List<FavoritoEntity>>

    /**
     * Obtiene todos los favoritos de una sola vez (sin Flow)
     */
    @Query("SELECT * FROM favoritos ORDER BY fechaAgregado DESC")
    suspend fun getAllFavoritesOnce(): List<FavoritoEntity>

    /**
     * Verifica si un producto está en favoritos
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE productoId = :productoId)")
    suspend fun isFavorito(productoId: Int): Boolean

    /**
     * Obtiene un favorito específico
     */
    @Query("SELECT * FROM favoritos WHERE productoId = :productoId")
    suspend fun getFavoritoById(productoId: Int): FavoritoEntity?

    /**
     * Agrega un producto a favoritos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorito(favorito: FavoritoEntity)

    /**
     * Elimina un producto de favoritos
     */
    @Query("DELETE FROM favoritos WHERE productoId = :productoId")
    suspend fun deleteFavorito(productoId: Int)

    /**
     * Elimina todos los favoritos
     */
    @Query("DELETE FROM favoritos")
    suspend fun deleteAllFavoritos()

    /**
     * Cuenta el total de favoritos
     */
    @Query("SELECT COUNT(*) FROM favoritos")
    suspend fun countFavoritos(): Int

    /**
     * Obtiene favoritos por tipo de producto
     */
    @Query("SELECT * FROM favoritos WHERE idTipo = :tipoId ORDER BY fechaAgregado DESC")
    fun getFavoritosByTipo(tipoId: Int): Flow<List<FavoritoEntity>>
}
