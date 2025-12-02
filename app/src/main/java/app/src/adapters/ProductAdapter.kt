package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.models.Producto
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.*

class ProductAdapter(
    private var products: List<Producto>,
    private val onAddToCart: (Producto) -> Unit,
    private val onShowConversions: (Producto) -> Unit,
    private val onToggleFavorite: ((Producto) -> Unit)? = null,
    private val favoriteProductIds: Set<Int> = emptySet()
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private var currentFavorites = favoriteProductIds.toMutableSet()

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        val productName: TextView = itemView.findViewById(R.id.tv_product_name)
        val productDescription: TextView = itemView.findViewById(R.id.tv_product_description)
        val productPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        val productAvailability: TextView = itemView.findViewById(R.id.tv_product_availability)
        val btnAddToCart: Button = itemView.findViewById(R.id.btn_add_to_cart)
        val btnConversions: MaterialButton = itemView.findViewById(R.id.btn_conversions)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btn_favorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.productName.text = product.nombre
        holder.productDescription.text = product.descripcion ?: "No description"
        holder.productPrice.text = "$${String.format("%.2f", product.precio)}"

        // Cargar imagen con Glide
        Glide.with(holder.itemView.context)
            .load(product.imagenUrl)
            .placeholder(R.drawable.ic_store_24)
            .error(R.drawable.ic_store_24)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.productImage)

        if (product.disponible) {
            holder.productAvailability.text = "Available"
            holder.productAvailability.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
            holder.btnAddToCart.isEnabled = true
        } else {
            holder.productAvailability.text = "Not Available"
            holder.productAvailability.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
            )
            holder.btnAddToCart.isEnabled = false
        }

        // Configurar botón de favorito
        val isFavorite = currentFavorites.contains(product.id)
        updateFavoriteButton(holder.btnFavorite, isFavorite)

        holder.btnFavorite.setOnClickListener {
            onToggleFavorite?.invoke(product)
            // Cambiar estado visual inmediatamente
            val newState = !currentFavorites.contains(product.id)
            if (newState) {
                currentFavorites.add(product.id)
            } else {
                currentFavorites.remove(product.id)
            }
            updateFavoriteButton(holder.btnFavorite, newState)
        }

        holder.btnAddToCart.setOnClickListener {
            onAddToCart(product)
        }

        holder.btnConversions.setOnClickListener {
            onShowConversions(product)
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Producto>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun updateFavorites(favoriteIds: Set<Int>) {
        currentFavorites = favoriteIds.toMutableSet()
        notifyDataSetChanged()
    }

    private fun updateFavoriteButton(button: ImageButton, isFavorite: Boolean) {
        if (isFavorite) {
            button.setImageResource(android.R.drawable.btn_star_big_on)
            button.contentDescription = "Remove from favorites"
        } else {
            button.setImageResource(android.R.drawable.btn_star_big_off)
            button.contentDescription = "Add to favorites"
        }
    }
}
