package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.utils.CartManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class CartAdapter(
    private var items: List<CartManager.CartItem>,
    private val onRemoveItem: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_cart_item_image)
        val tvName: TextView = itemView.findViewById(R.id.tv_cart_item_name)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_cart_item_price)
        val tvQuantity: TextView = itemView.findViewById(R.id.tv_cart_item_quantity)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_cart_item_total)
        val btnRemove: Button = itemView.findViewById(R.id.btn_remove_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_summary, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        val producto = item.producto

        holder.tvName.text = producto.nombre
        holder.tvPrice.text = "$${String.format("%.2f", producto.precio)}"
        holder.tvQuantity.text = "x${item.cantidad}"
        holder.tvTotal.text = "$${String.format("%.2f", item.subtotal)}"

        // Cargar imagen con Glide
        Glide.with(holder.itemView.context)
            .load(producto.imagenUrl)
            .placeholder(R.drawable.ic_store_24)
            .error(R.drawable.ic_store_24)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.ivImage)

        holder.btnRemove.setOnClickListener {
            onRemoveItem(producto.id)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<CartManager.CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
