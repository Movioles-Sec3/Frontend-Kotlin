package app.src.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para calificaciones de órdenes
 * Almacena comentarios en base de datos relacional local
 */
@Entity(tableName = "calificaciones")
data class CalificacionEntity(
    @PrimaryKey
    val orderId: Int,
    val calificacion: Int, // 1-10
    val comentario: String,
    val fechaCalificacion: Long = System.currentTimeMillis()
)

