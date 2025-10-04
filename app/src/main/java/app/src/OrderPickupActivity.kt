package app.src

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.src.data.api.ApiClient
import app.src.data.models.Compra
import app.src.data.models.EstadoCompra
import app.src.utils.QRCodeGenerator
import app.src.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class OrderPickupActivity : AppCompatActivity() {

    private lateinit var tvQrCode: TextView
    private lateinit var ivQrCode: ImageView
    private lateinit var tvCompraId: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvEstado: TextView
    private lateinit var btnCopyCode: Button
    private lateinit var btnBackToHistory: Button
    private lateinit var btnBackToHome: Button

    // Botones de control de estado
    private lateinit var btnEnPreparacion: Button
    private lateinit var btnListo: Button
    private lateinit var btnEscanearQR: Button
    private lateinit var cardEstadoControl: MaterialCardView
    private lateinit var progressBar: ProgressBar

    private val viewModel: OrderPickupViewModel by viewModels()
    private var currentCompra: Compra? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_pickup)

        // Load session token
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        initializeViews()
        setupObservers()
        loadCompraData()
    }

    private fun initializeViews() {
        tvQrCode = findViewById(R.id.tv_qr_code)
        ivQrCode = findViewById(R.id.iv_qr_code)
        tvCompraId = findViewById(R.id.tv_compra_id)
        tvTotal = findViewById(R.id.tv_total)
        tvEstado = findViewById(R.id.tv_estado)
        btnCopyCode = findViewById(R.id.btn_copy_code)
        btnBackToHistory = findViewById(R.id.btn_back_to_history)
        btnBackToHome = findViewById(R.id.btn_back_to_home)

        // Botones de control de estado
        btnEnPreparacion = findViewById(R.id.btn_en_preparacion)
        btnListo = findViewById(R.id.btn_listo)
        btnEscanearQR = findViewById(R.id.btn_escanear_qr)
        cardEstadoControl = findViewById(R.id.card_estado_control)
        progressBar = findViewById(R.id.progress_bar)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Copy QR code to clipboard
        btnCopyCode.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code", tvQrCode.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "QR Code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Back to Order History button
        btnBackToHistory.setOnClickListener {
            val intent = Intent(this, OrderHistoryActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Back to Home button
        btnBackToHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // Botón para pasar a EN_PREPARACION
        btnEnPreparacion.setOnClickListener {
            currentCompra?.let { compra ->
                confirmarCambioEstado(compra.id, EstadoCompra.EN_PREPARACION, "In Preparation")
            }
        }

        // Botón para pasar a LISTO
        btnListo.setOnClickListener {
            currentCompra?.let { compra ->
                confirmarCambioEstado(compra.id, EstadoCompra.LISTO, "Ready")
            }
        }

        // Botón para escanear QR (pasar a ENTREGADO)
        btnEscanearQR.setOnClickListener {
            currentCompra?.let { compra ->
                compra.qr?.let { qr ->
                    confirmarEscaneoQR(qr.codigoQrHash)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is OrderPickupState.Loading -> {
                    showLoading(true)
                }
                is OrderPickupState.Success -> {
                    showLoading(false)
                    val previousEstado = currentCompra?.estado
                    currentCompra = state.compra
                    updateUI(state.compra)

                    // Vibrar cuando el estado cambia (excepto la carga inicial)
                    if (previousEstado != null && previousEstado != state.compra.estado) {
                        vibrateOrderReady()
                    }

                    Toast.makeText(this, "Status updated successfully", Toast.LENGTH_SHORT).show()
                }
                is OrderPickupState.QRScanned -> {
                    showLoading(false)
                    // Vibrar cuando se escanea el QR y se entrega la orden
                    vibrateOrderReady()
                    mostrarResultadoQR(state.response.mensaje, state.response.cliente, state.response.total)
                }
                is OrderPickupState.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is OrderPickupState.Idle -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun loadCompraData() {
        // Get purchase data from Intent
        val qrCode = intent.getStringExtra("qr_code") ?: ""
        val compraId = intent.getIntExtra("compra_id", 0)
        val total = intent.getDoubleExtra("total", 0.0)
        val estadoStr = intent.getStringExtra("estado") ?: "PAGADO"

        // Crear objeto Compra temporal para la UI
        val estado = try {
            EstadoCompra.valueOf(estadoStr)
        } catch (e: Exception) {
            EstadoCompra.PAGADO
        }

        // Crear un objeto Compra simplificado
        val compra = Compra(
            id = compraId,
            fechaHora = "",
            total = total,
            estado = estado,
            detalles = emptyList(),
            qr = if (qrCode.isNotEmpty()) {
                app.src.data.models.QR(
                    codigoQrHash = qrCode,
                    estado = app.src.data.models.EstadoQR.ACTIVO
                )
            } else null,
            // Nuevos campos de fechas - null porque aún no están disponibles
            fechaEnPreparacion = null,
            fechaListo = null,
            fechaEntregado = null,
            // Nuevos campos de tiempos - null porque aún no están disponibles
            tiempoHastaPreparacion = null,
            tiempoPreparacion = null,
            tiempoEsperaEntrega = null,
            tiempoTotal = null
        )

        currentCompra = compra
        viewModel.loadCompra(compra)
        updateUI(compra)
    }

    private fun updateUI(compra: Compra) {
        // Display basic data
        tvQrCode.text = compra.qr?.codigoQrHash ?: "No QR"
        tvCompraId.text = String.format(Locale.US, "Order #%d", compra.id)
        tvTotal.text = String.format(Locale.US, "$%.0f", compra.total)

        // Set status with color
        tvEstado.text = getEstadoDisplayText(compra.estado)
        tvEstado.setTextColor(getEstadoColor(compra.estado))

        // Actualizar visibilidad de botones según el estado actual
        updateButtonsVisibility(compra.estado)

        // Generar y mostrar código QR
        compra.qr?.let { qr ->
            val bitmap = QRCodeGenerator.generateQRCode(qr.codigoQrHash)
            ivQrCode.setImageBitmap(bitmap)
            ivQrCode.visibility = View.VISIBLE
        } ?: run {
            ivQrCode.visibility = View.GONE
        }
    }

    private fun updateButtonsVisibility(estado: EstadoCompra) {
        // Resetear visibilidad
        btnEnPreparacion.visibility = View.GONE
        btnListo.visibility = View.GONE
        btnEscanearQR.visibility = View.GONE

        when (estado) {
            EstadoCompra.PAGADO -> {
                btnEnPreparacion.visibility = View.VISIBLE
                cardEstadoControl.visibility = View.VISIBLE
            }
            EstadoCompra.EN_PREPARACION -> {
                btnListo.visibility = View.VISIBLE
                cardEstadoControl.visibility = View.VISIBLE
            }
            EstadoCompra.LISTO -> {
                btnEscanearQR.visibility = View.VISIBLE
                cardEstadoControl.visibility = View.VISIBLE
            }
            EstadoCompra.ENTREGADO -> {
                cardEstadoControl.visibility = View.GONE
            }
            else -> {
                cardEstadoControl.visibility = View.GONE
            }
        }
    }

    private fun confirmarCambioEstado(compraId: Int, nuevoEstado: EstadoCompra, nombreEstado: String) {
        AlertDialog.Builder(this)
            .setTitle("Change Status")
            .setMessage("Do you want to change the order status to '$nombreEstado'?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.actualizarEstado(compraId, nuevoEstado)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmarEscaneoQR(codigoQrHash: String) {
        AlertDialog.Builder(this)
            .setTitle("Scan QR")
            .setMessage("Do you confirm the delivery of this order to the customer?")
            .setPositiveButton("Yes, Deliver") { _, _ ->
                viewModel.escanearQR(codigoQrHash)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun mostrarResultadoQR(mensaje: String, cliente: String, total: Double) {
        AlertDialog.Builder(this)
            .setTitle("✅ Order Delivered")
            .setMessage(
                "$mensaje\n\n" +
                "Customer: $cliente\n" +
                "Total: $${String.format(Locale.US, "%.0f", total)}"
            )
            .setPositiveButton("OK") { _, _ ->
                // Actualizar UI a estado ENTREGADO
                currentCompra?.let { compra ->
                    val compraActualizada = compra.copy(estado = EstadoCompra.ENTREGADO)
                    currentCompra = compraActualizada
                    updateUI(compraActualizada)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE

        // Deshabilitar botones mientras carga
        btnEnPreparacion.isEnabled = !show
        btnListo.isEnabled = !show
        btnEscanearQR.isEnabled = !show
    }

    /**
     * Vibrates the device when an order is ready for pickup
     * Uses different patterns for Android versions
     */
    private fun vibrateOrderReady() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Check if device has vibrator
            if (!vibrator.hasVibrator()) {
                return
            }

            // Create vibration pattern: wait, vibrate, wait, vibrate
            // Pattern: [delay, vibrate, pause, vibrate, pause, vibrate]
            val pattern = longArrayOf(0, 200, 100, 200, 100, 400)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android O (API 26) and above
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1) // -1 means don't repeat
                vibrator.vibrate(vibrationEffect)
            } else {
                // For older versions
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1) // -1 means don't repeat
            }

            Toast.makeText(this, "✅ Vibration executed!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("OrderPickup", "Vibration error", e)
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
            EstadoCompra.PAGADO -> getColor(R.color.status_paid)
            EstadoCompra.EN_PREPARACION -> getColor(R.color.status_preparing)
            EstadoCompra.LISTO -> getColor(R.color.status_ready)
            EstadoCompra.ENTREGADO -> getColor(R.color.status_delivered)
            else -> getColor(R.color.text_secondary)
        }
    }
}
