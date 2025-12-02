package app.src.data.local.dao

import androidx.room.*
import app.src.data.local.entities.CalificacionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para calificaciones
 * Operaciones sobre base de datos relacional local
 */
@Dao
interface CalificacionDao {

    @Query("SELECT * FROM calificaciones WHERE orderId = :orderId")
    suspend fun getCalificacionByOrderId(orderId: Int): CalificacionEntity?

    @Query("SELECT * FROM calificaciones ORDER BY fechaCalificacion DESC")
    fun getAllCalificaciones(): Flow<List<CalificacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalificacion(calificacion: CalificacionEntity)

    @Update
    suspend fun updateCalificacion(calificacion: CalificacionEntity)

    @Delete
    suspend fun deleteCalificacion(calificacion: CalificacionEntity)

    @Query("SELECT COUNT(*) FROM calificaciones")
    suspend fun countCalificaciones(): Int

    @Query("SELECT AVG(calificacion) FROM calificaciones")
    suspend fun getPromedioCalificaciones(): Double?
}

