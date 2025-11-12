package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.models.Compra
import app.src.data.models.EstadoCompra
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.round

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

        // Nuevos elementos para los tiempos
        val layoutDeliveryTimes: LinearLayout = itemView.findViewById(R.id.layout_delivery_times)
        val tvTiempoHastaPreparacion: TextView = itemView.findViewById(R.id.tv_tiempo_hasta_preparacion)
        val tvTiempoPreparacion: TextView = itemView.findViewById(R.id.tv_tiempo_preparacion)
        val tvTiempoEsperaEntrega: TextView = itemView.findViewById(R.id.tv_tiempo_espera_entrega)
        val tvTiempoTotal: TextView = itemView.findViewById(R.id.tv_tiempo_total)
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

        // Mostrar tiempos solo si la compra está entregada y los datos están disponibles
        val shouldShowTimes = order.estado == EstadoCompra.ENTREGADO &&
                             order.tiempoTotal != null &&
                             order.tiempoHastaPreparacion != null &&
                             order.tiempoPreparacion != null &&
                             order.tiempoEsperaEntrega != null

        holder.layoutDeliveryTimes.isVisible = shouldShowTimes

        if (shouldShowTimes) {
            // Convertir segundos a minutos y mostrar con 1 decimal
            holder.tvTiempoHastaPreparacion.text = "${formatTime(order.tiempoHastaPreparacion!!)} min"
            holder.tvTiempoPreparacion.text = "${formatTime(order.tiempoPreparacion!!)} min"
            holder.tvTiempoEsperaEntrega.text = "${formatTime(order.tiempoEsperaEntrega!!)} min"
            holder.tvTiempoTotal.text = "${formatTime(order.tiempoTotal!!)} min"
        }

        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }
    }

    override fun getItemCount() = orders.size

    private fun formatTime(timeInSeconds: Double): String {
        val timeInMinutes = timeInSeconds / 60.0
        return String.format(Locale.US, "%.1f", timeInMinutes)
    }

    private fun getEstadoDisplayText(estado: EstadoCompra): String {
        return when (estado) {
            EstadoCompra.CARRITO -> "Cart"
            EstadoCompra.PAGADO -> "Paid"
            EstadoCompra.EN_PREPARACION -> "In Preparation"
            EstadoCompra.LISTO -> "Ready"
            EstadoCompra.ENTREGADO -> "Delivered"
            EstadoCompra.WAITING_CONNECTION -> "Waiting Connection" // ✅ Nuevo estado
        }
    }

    private fun getEstadoColor(context: android.content.Context, estado: EstadoCompra): Int {
        return when (estado) {
            EstadoCompra.PAGADO -> context.getColor(R.color.status_paid)
            EstadoCompra.EN_PREPARACION -> context.getColor(R.color.status_preparing)
            EstadoCompra.LISTO -> context.getColor(R.color.status_ready)
            EstadoCompra.ENTREGADO -> context.getColor(R.color.status_delivered)
            EstadoCompra.WAITING_CONNECTION -> context.getColor(android.R.color.holo_orange_light) // ✅ Color naranja para waiting
            else -> context.getColor(R.color.text_secondary)
        }
    }
}
