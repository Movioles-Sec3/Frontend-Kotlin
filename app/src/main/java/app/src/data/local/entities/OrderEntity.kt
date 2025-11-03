package app.src.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para órdenes confirmadas
 * Almacena resumen de órdenes para acceso offline
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: Int,
    val status: String, // PENDING, READY, DELIVERED
    val total: Double,
    val createdAt: Long,
    val readyAt: Long? = null,
    val deliveredAt: Long? = null,
    val userId: Int,
    val qrCode: String? = null, // ✅ NUEVO: Almacenar QR para acceso offline
    // ✅ NUEVO: Tiempos de entrega en segundos
    val tiempoHastaPreparacion: Double? = null,
    val tiempoPreparacion: Double? = null,
    val tiempoEsperaEntrega: Double? = null,
    val tiempoTotal: Double? = null
)
