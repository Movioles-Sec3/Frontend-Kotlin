package app.src

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.src.data.api.ApiClient
import app.src.data.models.EstadoCompra
import app.src.utils.SessionManager
import java.util.Locale

class OrderPickupActivity : AppCompatActivity() {

    private lateinit var tvQrCode: TextView
    private lateinit var tvCompraId: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvEstado: TextView
    private lateinit var btnCopyCode: Button
    private lateinit var btnBackToHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_pickup)

        // Load session token
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Initialize views
        tvQrCode = findViewById(R.id.tv_qr_code)
        tvCompraId = findViewById(R.id.tv_compra_id)
        tvTotal = findViewById(R.id.tv_total)
        tvEstado = findViewById(R.id.tv_estado)
        btnCopyCode = findViewById(R.id.btn_copy_code)
        btnBackToHome = findViewById(R.id.btn_back_to_home)

        // Get purchase data from Intent
        val qrCode = intent.getStringExtra("qr_code") ?: ""
        val compraId = intent.getIntExtra("compra_id", 0)
        val total = intent.getDoubleExtra("total", 0.0)
        val estadoStr = intent.getStringExtra("estado") ?: "PAGADO"

        // Display data
        tvQrCode.text = qrCode
        tvCompraId.text = String.format(Locale.US, "Order #%d", compraId)
        tvTotal.text = String.format(Locale.US, "$%.2f", total)

        // Set status with color
        val estado = try {
            EstadoCompra.valueOf(estadoStr)
        } catch (e: Exception) {
            EstadoCompra.PAGADO
        }

        tvEstado.text = getEstadoDisplayText(estado)
        tvEstado.setTextColor(getEstadoColor(estado))

        // Copy QR code to clipboard
        btnCopyCode.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code", qrCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "QR Code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnBackToHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun getEstadoDisplayText(estado: EstadoCompra): String {
        return when (estado) {
            EstadoCompra.CARRITO -> "Cart"
            EstadoCompra.PAGADO -> "Paid"
            EstadoCompra.EN_PREPARACION -> "In Preparation"
            EstadoCompra.LISTO -> "Ready"
            EstadoCompra.ENTREGADO -> "Delivered"
        }
    }

    private fun getEstadoColor(estado: EstadoCompra): Int {
        return when (estado) {
            EstadoCompra.PAGADO -> getColor(android.R.color.holo_blue_dark)
            EstadoCompra.EN_PREPARACION -> getColor(android.R.color.holo_orange_dark)
            EstadoCompra.LISTO -> getColor(android.R.color.holo_green_dark)
            EstadoCompra.ENTREGADO -> getColor(android.R.color.holo_green_light)
            else -> getColor(android.R.color.darker_gray)
        }
    }
}
