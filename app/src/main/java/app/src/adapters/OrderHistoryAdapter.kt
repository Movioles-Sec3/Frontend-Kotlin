package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private val onOrderClick: (Compra) -> Unit,
    private val onCalificarClick: (Compra) -> Unit // Nuevo callback para calificación
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

        // Botón de calificación
        val btnCalificar: Button = itemView.findViewById(R.id.btn_calificar)
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

        // Click en la tarjeta para ver detalles
        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }

        // Click en botón de calificación
        holder.btnCalificar.setOnClickListener {
            onCalificarClick(order)
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
            EstadoCompra.WAITING_CONNECTION -> "Pending Sync"
        }
    }

    private fun getEstadoColor(context: android.content.Context, estado: EstadoCompra): Int {
        val colorResId = when (estado) {
            EstadoCompra.PAGADO -> com.google.android.material.R.attr.colorPrimary
            EstadoCompra.EN_PREPARACION -> com.google.android.material.R.attr.colorTertiary
            EstadoCompra.LISTO -> com.google.android.material.R.attr.colorSecondary
            EstadoCompra.ENTREGADO -> com.google.android.material.R.attr.colorPrimary
            EstadoCompra.WAITING_CONNECTION -> com.google.android.material.R.attr.colorError
            else -> com.google.android.material.R.attr.colorOnSurfaceVariant
        }
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(colorResId, typedValue, true)
        return typedValue.data
    }
}
