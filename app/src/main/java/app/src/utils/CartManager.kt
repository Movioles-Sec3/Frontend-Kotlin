package app.src.utils

import app.src.data.models.Producto

object CartManager {

    data class CartItem(
        val producto: Producto,
        var cantidad: Int = 1
    ) {
        val subtotal: Double
            get() = producto.precio * cantidad
    }

    private val items = mutableMapOf<Int, CartItem>()

    fun addProduct(producto: Producto, cantidad: Int = 1) {
        val existing = items[producto.id]
        if (existing != null) {
            existing.cantidad += cantidad
        } else {
            items[producto.id] = CartItem(producto, cantidad)
        }
    }

    fun removeProduct(productoId: Int) {
        items.remove(productoId)
    }

    fun updateQuantity(productoId: Int, cantidad: Int) {
        val item = items[productoId]
        if (item != null) {
            if (cantidad <= 0) {
                items.remove(productoId)
            } else {
                item.cantidad = cantidad
            }
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
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }
}

