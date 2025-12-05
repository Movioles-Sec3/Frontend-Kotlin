package app.src.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.src.R
import app.src.ReceiptItem
import java.text.NumberFormat
import java.util.*

/**
 * Adapter para mostrar el recibo de gastos totales
 */
class ReceiptAdapter(
    private val items: List<ReceiptItem>
) : RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderNumber: TextView = itemView.findViewById(R.id.tv_receipt_order_number)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_receipt_date)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_receipt_amount)

        fun bind(item: ReceiptItem) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            tvOrderNumber.text = "Orden #${item.orderNumber}"
            tvDate.text = item.date
            tvAmount.text = currencyFormat.format(item.amount)
        }
    }
}

