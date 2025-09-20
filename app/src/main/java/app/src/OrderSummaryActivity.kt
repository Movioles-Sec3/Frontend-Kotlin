package app.src

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

class OrderSummaryActivity : AppCompatActivity() {

    // ✅ No parcelize: just implement Serializable
    data class CartItem(
        val id: String,
        val title: String,
        val qty: Int,
        val price: Double
    ) : Serializable

    private val currency: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_order_summary)

            // READ ITEMS (Serializable fallback)
            @Suppress("UNCHECKED_CAST")
            val items: List<CartItem> =
                (intent.getSerializableExtra("cart_items") as? ArrayList<CartItem>)
                    ?: mockItems()

            val container = findViewById<LinearLayout>(R.id.ll_items_container) ?: error("Missing ll_items_container in activity_order_summary.xml")
            val inflater = LayoutInflater.from(this)

            var subtotal = 0.0
            for (item in items) {
                val row = inflater.inflate(R.layout.item_order_summary, container, false)
                row.findViewById<TextView>(R.id.tv_item_title)?.text = item.title
                    ?: error("Missing tv_item_title in item_order_summary.xml")
                row.findViewById<TextView>(R.id.tv_item_qty_price)?.text =
                    "${item.qty} × ${currency.format(item.price)}"
                        ?: error("Missing tv_item_qty_price in item_order_summary.xml")
                val lineTotal = item.qty * item.price
                row.findViewById<TextView>(R.id.tv_item_total)?.text = currency.format(lineTotal)
                    ?: error("Missing tv_item_total in item_order_summary.xml")
                subtotal += lineTotal
                container.addView(row)
            }

            val tax = subtotal * 0.08
            val total = subtotal + tax
            findViewById<TextView>(R.id.tv_subtotal_value)?.text = currency.format(subtotal)
                ?: error("Missing tv_subtotal_value in activity_order_summary.xml")
            findViewById<TextView>(R.id.tv_tax_value)?.text = currency.format(tax)
                ?: error("Missing tv_tax_value in activity_order_summary.xml")
            findViewById<TextView>(R.id.tv_total_value)?.text = currency.format(total)
                ?: error("Missing tv_total_value in activity_order_summary.xml")

            findViewById<Button>(R.id.btn_checkout)?.setOnClickListener {
                Toast.makeText(this, "Proceeding to checkout…", Toast.LENGTH_SHORT).show()
            } ?: error("Missing btn_checkout in activity_order_summary.xml")

            findViewById<Button>(R.id.btn_back_to_home)?.setOnClickListener {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            } ?: error("Missing btn_back_to_home in activity_order_summary.xml")

        } catch (t: Throwable) {
            Toast.makeText(this, "OrderSummary error: ${t.message}", Toast.LENGTH_LONG).show()
            t.printStackTrace()
            finish() // avoid leaving a broken screen
        }
    }

    private fun mockItems(): ArrayList<CartItem> = arrayListOf(
        CartItem("pils", "Pilsner 16oz", 2, 3.50),
        CartItem("fries", "Fries", 1, 3.00),
        CartItem("tapas", "Tapas Mix", 1, 6.50)
    )
}
