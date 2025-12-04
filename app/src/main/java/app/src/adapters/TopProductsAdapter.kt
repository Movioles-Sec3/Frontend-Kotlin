package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.models.ProductFrequency
import java.text.NumberFormat
import java.util.*

/**
 * Adapter para mostrar los productos más comprados
 * Parte de la Business Question de frecuencia de productos
 */
class TopProductsAdapter(
    private val onProductClick: (ProductFrequency) -> Unit = {}
) : ListAdapter<ProductFrequency, TopProductsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_product, parent, false)
        return ViewHolder(view, onProductClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(
        itemView: View,
        private val onProductClick: (ProductFrequency) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvRanking: TextView = itemView.findViewById(R.id.tv_ranking)
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvOrderCount: TextView = itemView.findViewById(R.id.tv_order_count)
        private val tvTotalSpent: TextView = itemView.findViewById(R.id.tv_total_spent)

        fun bind(product: ProductFrequency, rank: Int) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            tvRanking.text = "#$rank"
            tvProductName.text = product.productName
            tvOrderCount.text = "${product.orderCount} órdenes"
            tvTotalSpent.text = currencyFormat.format(product.totalSpent)

            itemView.setOnClickListener {
                onProductClick(product)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductFrequency>() {
        override fun areItemsTheSame(oldItem: ProductFrequency, newItem: ProductFrequency): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: ProductFrequency, newItem: ProductFrequency): Boolean {
            return oldItem == newItem
        }
    }
}
