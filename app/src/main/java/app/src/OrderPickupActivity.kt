package app.src

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.src.data.api.ApiClient
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

        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Inicializar vistas
        tvQrCode = findViewById(R.id.tv_qr_code)
        tvCompraId = findViewById(R.id.tv_compra_id)
        tvTotal = findViewById(R.id.tv_total)
        tvEstado = findViewById(R.id.tv_estado)
        btnCopyCode = findViewById(R.id.btn_copy_code)
        btnBackToHome = findViewById(R.id.btn_back_to_home)

        // Obtener datos de la compra desde el Intent
        val qrCode = intent.getStringExtra("qr_code") ?: ""
        val compraId = intent.getIntExtra("compra_id", 0)
        val total = intent.getDoubleExtra("total", 0.0)

        // Mostrar datos
        tvQrCode.text = qrCode
        tvCompraId.text = String.format(Locale.US, "Order #%d", compraId)
        tvTotal.text = String.format(Locale.US, "$%.2f", total)
        tvEstado.text = "PAID"

        // Copiar código QR al portapapeles
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
}
