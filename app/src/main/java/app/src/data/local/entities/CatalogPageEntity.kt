package app.src.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para cache de catálogo (home, categorías, productos)
 * Implementa TTL (Time To Live) para expiración automática
 */
@Entity(tableName = "catalog_pages")
data class CatalogPageEntity(
    @PrimaryKey val key: String, // ej: "home:feed:v1", "category:1:products:page:1:v1"
    val json: String, // Datos serializados en JSON
    val expiresAt: Long // Timestamp de expiración
)

