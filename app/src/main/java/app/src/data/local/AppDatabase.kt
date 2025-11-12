package app.src.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.src.data.local.dao.CatalogDao
import app.src.data.local.dao.OrderDao
import app.src.data.local.dao.OrderOutboxDao
import app.src.data.local.entities.CatalogPageEntity
import app.src.data.local.entities.OrderEntity
import app.src.data.local.entities.OrderItemEntity
import app.src.data.local.entities.OrderOutboxEntity

/**
 * Base de datos Room principal
 * Contiene 3 capas de almacenamiento:
 * 1. Orders + OrderItems (historial y estado de órdenes)
 * 2. OrderOutbox (cola de sincronización offline)
 * 3. CatalogPages (cache de productos y categorías con TTL)
 */
@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        OrderOutboxEntity::class,
        CatalogPageEntity::class
    ],
    version = 4, // ✅ INCREMENTADO de 3 a 4 por el campo tempOrderId agregado a OrderOutboxEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun orderOutboxDao(): OrderOutboxDao
    abstract fun catalogDao(): CatalogDao

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
