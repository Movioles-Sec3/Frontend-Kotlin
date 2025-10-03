package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.models.Compra
import app.src.data.models.EstadoCompra
import java.text.SimpleDateFormat
import java.util.Locale

class OrderHistoryAdapter(
    private val orders: List<Compra>,
    private val onOrderClick: (Compra) -> Unit
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tv_order_date)
        val tvOrderTotal: TextView = itemView.findViewById(R.id.tv_order_total)
        val tvOrderStatus: TextView = itemView.findViewById(R.id.tv_order_status)
        val tvOrderItems: TextView = itemView.findViewById(R.id.tv_order_items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_history, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvOrderId.text = String.format(Locale.US, "Order #%d", order.id)
        holder.tvOrderTotal.text = String.format(Locale.US, "$%.2f", order.total)

        // Format date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
            val date = inputFormat.parse(order.fechaHora)
            holder.tvOrderDate.text = if (date != null) outputFormat.format(date) else order.fechaHora
        } catch (e: Exception) {
            holder.tvOrderDate.text = order.fechaHora
        }

        // Status
        holder.tvOrderStatus.text = getEstadoDisplayText(order.estado)
        holder.tvOrderStatus.setTextColor(getEstadoColor(holder.itemView.context, order.estado))

        // Items summary
        val itemCount = order.detalles.sumOf { it.cantidad }
        holder.tvOrderItems.text = "$itemCount item${if (itemCount != 1) "s" else ""}"

        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }
    }

    override fun getItemCount() = orders.size

    private fun getEstadoDisplayText(estado: EstadoCompra): String {
        return when (estado) {
            EstadoCompra.CARRITO -> "Cart"
            EstadoCompra.PAGADO -> "Paid"
            EstadoCompra.EN_PREPARACION -> "In Preparation"
            EstadoCompra.LISTO -> "Ready"
            EstadoCompra.ENTREGADO -> "Delivered"
        }
    }

    private fun getEstadoColor(context: android.content.Context, estado: EstadoCompra): Int {
        return when (estado) {
            EstadoCompra.PAGADO -> context.getColor(android.R.color.holo_blue_dark)
            EstadoCompra.EN_PREPARACION -> context.getColor(android.R.color.holo_orange_dark)
            EstadoCompra.LISTO -> context.getColor(android.R.color.holo_green_dark)
            EstadoCompra.ENTREGADO -> context.getColor(android.R.color.holo_green_light)
            else -> context.getColor(android.R.color.darker_gray)
        }
    }
}

