package app.src.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import app.src.CalendarItem
import app.src.R

/**
 * Adapter para mostrar el calendario de actividad con intensidad de colores
 */
class CalendarAdapter(
    private val items: List<CalendarItem>
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    // Calcular intensidad máxima para normalizar colores
    private val maxOrderCount = items.maxOfOrNull { it.orderCount } ?: 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], maxOrderCount)
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.card_calendar)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvOrderCount: TextView = itemView.findViewById(R.id.tv_order_count)

        fun bind(item: CalendarItem, maxCount: Int) {
            tvDate.text = item.date
            tvOrderCount.text = "${item.orderCount} órdenes"

            // Calcular intensidad del color (más órdenes = más intenso)
            val intensity = (item.orderCount.toFloat() / maxCount * 255).toInt()
            val color = Color.rgb(255 - intensity, 255, 255 - intensity) // Verde con intensidad

            card.setCardBackgroundColor(color)
        }
    }
}

