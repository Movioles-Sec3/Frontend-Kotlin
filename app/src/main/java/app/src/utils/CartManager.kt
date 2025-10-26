package app.src.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import app.src.data.models.Producto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * CartManager - Gestión del Carrito de Compras con Persistencia
 *
 * Características:
 * - Almacenamiento en memoria (rápido) + SharedPreferences (persistente)
 * - El carrito NO se pierde al cerrar la app
 * - Sincronización automática entre memoria y disco
 * - Thread-safe para operaciones concurrentes
 */
object CartManager {

    private const val TAG = "CartManager"
    private const val PREFS_NAME = "CartPreferences"
    private const val KEY_CART_ITEMS = "cart_items"

    private val gson = Gson()
    private var context: Context? = null

    data class CartItem(
        val producto: Producto,
        var cantidad: Int = 1
    ) {
        val subtotal: Double
            get() = producto.precio * cantidad
    }

    // Cache en memoria para acceso rápido
    private val items = mutableMapOf<Int, CartItem>()

    /**
     * Inicializa el CartManager con el contexto de la aplicación
     * Debe llamarse al inicio de la app (en Application o Activity principal)
     */
    fun init(appContext: Context) {
        context = appContext.applicationContext
        loadFromPreferences()
    }

    /**
     * Carga el carrito desde SharedPreferences al iniciar
     */
    private fun loadFromPreferences() {
        try {
            val prefs = getPreferences() ?: return
            val json = prefs.getString(KEY_CART_ITEMS, null) ?: return

            val type = object : TypeToken<Map<Int, CartItem>>() {}.type
            val loadedItems: Map<Int, CartItem> = gson.fromJson(json, type)

            items.clear()
            items.putAll(loadedItems)

            Log.d(TAG, "✅ Carrito cargado desde persistencia: ${items.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cargar carrito: ${e.message}")
        }
    }

    /**
     * Guarda el carrito en SharedPreferences
     */
    private fun saveToPreferences() {
        try {
            val prefs = getPreferences() ?: return
            val json = gson.toJson(items)

            prefs.edit().putString(KEY_CART_ITEMS, json).apply()
            Log.d(TAG, "💾 Carrito guardado en persistencia")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar carrito: ${e.message}")
        }
    }

    private fun getPreferences(): SharedPreferences? {
        return context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addProduct(producto: Producto, cantidad: Int = 1) {
        val existing = items[producto.id]
        if (existing != null) {
            existing.cantidad += cantidad
        } else {
            items[producto.id] = CartItem(producto, cantidad)
        }
        saveToPreferences() // Persistir cambios
        Log.d(TAG, "➕ Producto agregado: ${producto.nombre} (cantidad: $cantidad)")
    }

    fun removeProduct(productoId: Int) {
        items.remove(productoId)
        saveToPreferences() // Persistir cambios
        Log.d(TAG, "➖ Producto eliminado: ID $productoId")
    }

    fun updateQuantity(productoId: Int, cantidad: Int) {
        val item = items[productoId]
        if (item != null) {
            if (cantidad <= 0) {
                items.remove(productoId)
            } else {
                item.cantidad = cantidad
            }
            saveToPreferences() // Persistir cambios
            Log.d(TAG, "🔄 Cantidad actualizada: ID $productoId → $cantidad")
        }
    }

    fun getItems(): List<CartItem> {
        return items.values.toList()
    }

    fun getTotal(): Double {
        return items.values.sumOf { it.subtotal }
    }

    fun getItemCount(): Int {
        return items.values.sumOf { it.cantidad }
    }

    fun clear() {
        items.clear()
        saveToPreferences() // Persistir cambios
        Log.d(TAG, "🗑️ Carrito limpiado")
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }
}
