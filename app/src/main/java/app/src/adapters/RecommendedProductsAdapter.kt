package app.src.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.models.Producto
import app.src.utils.AnalyticsLogger
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import java.util.Locale

class RecommendedProductsAdapter(
    private val productos: List<Producto>,
    private val onProductClick: (Producto) -> Unit,
    private val onAddToCartClick: (Producto) -> Unit,
    private val onShowConversions: (Producto) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<RecommendedProductsAdapter.RecommendedProductViewHolder>() {

    class RecommendedProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        val tvProductCategory: TextView = itemView.findViewById(R.id.tv_product_category)
        val btnAddToCart: Button = itemView.findViewById(R.id.btn_add_to_cart)
        val btnConversions: MaterialButton = itemView.findViewById(R.id.btn_conversions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommended_product, parent, false)
        return RecommendedProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendedProductViewHolder, position: Int) {
        val producto = productos[position]

        holder.tvProductName.text = producto.nombre
        holder.tvProductPrice.text = String.format(Locale.US, "$%.0f", producto.precio)
        holder.tvProductCategory.text = producto.tipoProducto.nombre

        // Cargar imagen con Glide
        Glide.with(holder.itemView.context)
            .load(producto.imagenUrl)
            .placeholder(R.drawable.ic_store_24) // Imagen mientras carga
            .error(R.drawable.ic_store_24) // Imagen si falla la carga
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cachear la imagen
            .centerCrop() // Ajustar la imagen al ImageView
            .into(holder.ivProductImage)

        // Click en el producto completo
        holder.itemView.setOnClickListener {
            onProductClick(producto)
        }

        // Click en el botón "Agregar al carrito"
        holder.btnAddToCart.setOnClickListener {
            // Registrar evento de analytics ANTES de añadir al carrito
            AnalyticsLogger.logProductAddedFromRecommended(
                context = context,
                productId = producto.id,
                productName = producto.nombre,
                productPrice = producto.precio,
                productCategory = producto.tipoProducto.nombre,
                quantity = 1 // Por defecto 1, se actualizará en el diálogo
            )

            onAddToCartClick(producto)
        }

        // Click en el botón "Conversiones"
        holder.btnConversions.setOnClickListener {
            onShowConversions(producto)
        }

        // Mostrar si el producto está disponible o no
        holder.btnAddToCart.isEnabled = producto.disponible
        holder.btnAddToCart.text = if (producto.disponible) "Add" else "N/A"
    }

    override fun getItemCount() = productos.size
}
