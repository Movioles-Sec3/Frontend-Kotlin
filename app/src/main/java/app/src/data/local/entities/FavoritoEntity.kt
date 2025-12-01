package app.src.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar productos favoritos offline
 *
 * Esta tabla guarda los productos marcados como favoritos por el usuario,
 * permitiendo acceso rápido sin necesidad de internet.
 */
@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey
    val productoId: Int,
    val nombre: String,
    val descripcion: String?,
    val imagenUrl: String?,
    val precio: Double,
    val disponible: Boolean,
    val idTipo: Int,
    val nombreTipo: String,
    val fechaAgregado: Long = System.currentTimeMillis() // Timestamp de cuándo se agregó a favoritos
)

