package app.src.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para cola de sincronización offline
 * Almacena órdenes pendientes de enviar al servidor
 */
@Entity(tableName = "order_outbox")
data class OrderOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val payloadJson: String,
    val createdAt: Long,
    val retries: Int = 0,
    val lastAttempt: Long? = null
)

