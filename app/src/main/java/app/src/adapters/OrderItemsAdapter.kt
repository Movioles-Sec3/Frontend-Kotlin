package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.local.entities.OrderItemEntity
import java.text.NumberFormat
import java.util.*

/**
 * Adapter para mostrar los items de una orden
 */
class OrderItemsAdapter(
    private val items: List<OrderItemEntity>
) : RecyclerView.Adapter<OrderItemsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tv_detail_product_name)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tv_detail_quantity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_detail_price)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tv_detail_subtotal)

        fun bind(item: OrderItemEntity) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            tvProductName.text = item.name
            tvQuantity.text = "x${item.quantity}"
            tvPrice.text = currencyFormat.format(item.price)
            tvSubtotal.text = currencyFormat.format(item.quantity * item.price)
        }
    }
}

