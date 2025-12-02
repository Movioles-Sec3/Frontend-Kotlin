package app.src.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.src.data.local.dao.CalificacionDao
import app.src.data.local.dao.CatalogDao
import app.src.data.local.dao.FavoritoDao
import app.src.data.local.dao.OrderDao
import app.src.data.local.dao.OrderOutboxDao
import app.src.data.local.entities.CalificacionEntity
import app.src.data.local.entities.CatalogPageEntity
import app.src.data.local.entities.FavoritoEntity
import app.src.data.local.entities.OrderEntity
import app.src.data.local.entities.OrderItemEntity
import app.src.data.local.entities.OrderOutboxEntity

/**
 * Base de datos Room principal
 * Contiene 5 capas de almacenamiento:
 * 1. Orders + OrderItems (historial y estado de órdenes)
 * 2. OrderOutbox (cola de sincronización offline)
 * 3. CatalogPages (cache de productos y categorías con TTL)
 * 4. Favoritos (productos favoritos del usuario - funciona 100% offline)
 * 5. Calificaciones (calificaciones y comentarios de órdenes - funciona 100% offline)
 */
@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        OrderOutboxEntity::class,
        CatalogPageEntity::class,
        FavoritoEntity::class,
        CalificacionEntity::class
    ],
    version = 6, // ✅ INCREMENTADO de 5 a 6 por agregar CalificacionEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun orderOutboxDao(): OrderOutboxDao
    abstract fun catalogDao(): CatalogDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun calificacionDao(): CalificacionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tapandtoast_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
