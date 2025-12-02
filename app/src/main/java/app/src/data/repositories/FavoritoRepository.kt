package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.local.AppDatabase
import app.src.data.local.dao.FavoritoDao
import app.src.data.local.entities.FavoritoEntity
import app.src.data.models.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestión de favoritos
 * Trabaja completamente offline usando Room Database
 * ✅ OPTIMIZADO CON MULTITHREADING usando Dispatchers
 */
class FavoritoRepository(context: Context) {

    private val favoritoDao: FavoritoDao = AppDatabase.getDatabase(context).favoritoDao()
    private val TAG = "FavoritoRepository"

    /**
     * Obtiene todos los favoritos como Flow (actualización automática)
     * Convierte FavoritoEntity a Producto para compatibilidad con la UI
     * ✅ Dispatcher.IO para operaciones de base de datos
     */
    fun getAllFavoritos(): Flow<List<Producto>> {
        return favoritoDao.getAllFavoritos().map { entities ->
            // ✅ Dispatcher.Default para transformación de datos (operación CPU-intensive)
            withContext(Dispatchers.Default) {
                entities.map { it.toProducto() }
            }
        }
    }

    /**
     * Verifica si un producto está en favoritos
     * ✅ Dispatcher.IO para lectura de base de datos
     */
    suspend fun isFavorito(productoId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                favoritoDao.isFavorito(productoId)
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando favorito: ${e.message}")
                false
            }
        }
    }

    /**
     * Agrega un producto a favoritos
     * ✅ Dispatcher.IO para escritura en base de datos
     */
    suspend fun addFavorito(producto: Producto): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val favorito = FavoritoEntity(
                    productoId = producto.id,
                    nombre = producto.nombre,
                    descripcion = producto.descripcion,
                    imagenUrl = producto.imagenUrl,
                    precio = producto.precio,
                    disponible = producto.disponible,
                    idTipo = producto.idTipo,
                    nombreTipo = producto.tipoProducto.nombre,
                    fechaAgregado = System.currentTimeMillis()
                )
                favoritoDao.insertFavorito(favorito)
                Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Producto ${producto.nombre} agregado a favoritos")
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error agregando favorito: ${e.message}")
                Result.Error("Error al agregar a favoritos: ${e.message}")
            }
        }
    }

    /**
     * Elimina un producto de favoritos
     * ✅ Dispatcher.IO para operación de eliminación en base de datos
     */
    suspend fun removeFavorito(productoId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                favoritoDao.deleteFavorito(productoId)
                Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Producto $productoId eliminado de favoritos")
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error eliminando favorito: ${e.message}")
                Result.Error("Error al eliminar de favoritos: ${e.message}")
            }
        }
    }

    /**
     * Toggle favorito (agregar si no existe, eliminar si existe)
     * ✅ Dispatcher.IO para operaciones de base de datos
     */
    suspend fun toggleFavorito(producto: Producto): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val isFav = favoritoDao.isFavorito(producto.id)
                if (isFav) {
                    favoritoDao.deleteFavorito(producto.id)
                    Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Toggle: eliminado ${producto.nombre}")
                    Result.Success(false) // Ahora NO es favorito
                } else {
                    val favorito = FavoritoEntity(
                        productoId = producto.id,
                        nombre = producto.nombre,
                        descripcion = producto.descripcion,
                        imagenUrl = producto.imagenUrl,
                        precio = producto.precio,
                        disponible = producto.disponible,
                        idTipo = producto.idTipo,
                        nombreTipo = producto.tipoProducto.nombre,
                        fechaAgregado = System.currentTimeMillis()
                    )
                    favoritoDao.insertFavorito(favorito)
                    Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Toggle: agregado ${producto.nombre}")
                    Result.Success(true) // Ahora SÍ es favorito
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en toggle favorito: ${e.message}")
                Result.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Elimina todos los favoritos
     * ✅ Dispatcher.IO para operación masiva de eliminación
     */
    suspend fun clearAllFavoritos(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val count = favoritoDao.countFavoritos()
                favoritoDao.deleteAllFavoritos()
                Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Todos los favoritos eliminados ($count items)")
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error limpiando favoritos: ${e.message}")
                Result.Error("Error al limpiar favoritos: ${e.message}")
            }
        }
    }

    /**
     * Cuenta el total de favoritos
     * ✅ Dispatcher.IO para consulta de base de datos
     */
    suspend fun countFavoritos(): Int {
        return withContext(Dispatchers.IO) {
            try {
                favoritoDao.countFavoritos()
            } catch (e: Exception) {
                Log.e(TAG, "Error contando favoritos: ${e.message}")
                0
            }
        }
    }

    /**
     * Extensión para convertir FavoritoEntity a Producto
     * Se ejecuta en el contexto apropiado (Default) para procesamiento de datos
     */
    private fun FavoritoEntity.toProducto(): Producto {
        return Producto(
            id = productoId,
            nombre = nombre,
            descripcion = descripcion,
            imagenUrl = imagenUrl,
            precio = precio,
            disponible = disponible,
            idTipo = idTipo,
            tipoProducto = app.src.data.models.TipoProducto(
                id = idTipo,
                nombre = nombreTipo
            )
        )
    }
}
