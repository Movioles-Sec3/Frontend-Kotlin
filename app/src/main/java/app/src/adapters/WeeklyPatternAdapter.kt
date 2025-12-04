package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.data.models.WeeklySpending
import java.text.NumberFormat
import java.util.*

/**
 * Adapter para mostrar el patrón de compra semanal
 * Parte de la Business Question de patrones temporales
 */
class WeeklyPatternAdapter : ListAdapter<WeeklySpending, WeeklyPatternAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_pattern, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayName: TextView = itemView.findViewById(R.id.tv_day_name)
        private val tvOrderCount: TextView = itemView.findViewById(R.id.tv_order_count)
        private val tvTotalSpent: TextView = itemView.findViewById(R.id.tv_total_spent)
        private val tvAvgValue: TextView = itemView.findViewById(R.id.tv_avg_value)

        fun bind(weeklySpending: WeeklySpending) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            tvDayName.text = weeklySpending.getDayName()
            tvOrderCount.text = "${weeklySpending.orderCount} órdenes"
            tvTotalSpent.text = currencyFormat.format(weeklySpending.totalSpent)
            tvAvgValue.text = "Promedio: ${currencyFormat.format(weeklySpending.avgOrderValue)}"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<WeeklySpending>() {
        override fun areItemsTheSame(oldItem: WeeklySpending, newItem: WeeklySpending): Boolean {
            return oldItem.dayOfWeek == newItem.dayOfWeek
        }

        override fun areContentsTheSame(oldItem: WeeklySpending, newItem: WeeklySpending): Boolean {
            return oldItem == newItem
        }
    }
}

