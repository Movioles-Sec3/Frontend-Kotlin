package app.src

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Configurar navegación a cada vista
        findViewById<Button>(R.id.btn_categories).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_products).setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_order_summary).setOnClickListener {
            startActivity(Intent(this, OrderSummaryActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_order_pickup).setOnClickListener {
            startActivity(Intent(this, OrderPickupActivity::class.java))
        }
    }
}
