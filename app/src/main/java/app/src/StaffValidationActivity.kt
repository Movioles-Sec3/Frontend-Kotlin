package app.src

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.src.data.api.ApiClient
import app.src.data.models.EscanearQRRequest
import app.src.data.repositories.CompraRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch
import java.util.Locale

class StaffValidationActivity : BaseActivity() {

    private val compraRepo = CompraRepository()
    private lateinit var etQrCode: EditText
    private lateinit var btnScanQr: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView
    private lateinit var tvResultDetails: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_validation)

        // Initialize views
        etQrCode = findViewById(R.id.et_qr_code)
        btnScanQr = findViewById(R.id.btn_scan_qr)
        progressBar = findViewById(R.id.progress_bar)
        tvResult = findViewById(R.id.tv_result)
        tvResultDetails = findViewById(R.id.tv_result_details)

        btnScanQr.setOnClickListener {
            val qrCode = etQrCode.text.toString().trim()

            if (qrCode.isEmpty()) {
                Toast.makeText(this, "Please enter a QR code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scanQRCode(qrCode)
        }
    }

    private fun scanQRCode(qrCode: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            btnScanQr.isEnabled = false
            tvResult.visibility = View.GONE
            tvResultDetails.visibility = View.GONE

            when (val result = compraRepo.escanearQR(qrCode)) {
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    btnScanQr.isEnabled = true

                    val response = result.data
                    tvResult.visibility = View.VISIBLE
                    tvResultDetails.visibility = View.VISIBLE

                    tvResult.text = "✓ ${response.mensaje}"
                    tvResult.setTextColor(getColor(android.R.color.holo_green_dark))

                    tvResultDetails.text = """
                        Order ID: ${response.compraId}
                        Customer: ${response.cliente}
                        Total: ${String.format(Locale.US, "$%.2f", response.total)}
                    """.trimIndent()

                    // Clear input
                    etQrCode.text.clear()

                    Toast.makeText(
                        this@StaffValidationActivity,
                        "Order delivered successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    btnScanQr.isEnabled = true

                    tvResult.visibility = View.VISIBLE
                    tvResultDetails.visibility = View.GONE

                    val errorMessage = when {
                        result.message.contains("no válido", ignoreCase = true) ||
                        result.message.contains("not found", ignoreCase = true) ->
                            "✗ Invalid QR Code"
                        result.message.contains("canjeado", ignoreCase = true) ||
                        result.message.contains("already", ignoreCase = true) ->
                            "✗ QR Code already used"
                        result.message.contains("no está lista", ignoreCase = true) ||
                        result.message.contains("not ready", ignoreCase = true) ->
                            "✗ Order is not ready yet"
                        else -> "✗ Error: ${result.message}"
                    }

                    tvResult.text = errorMessage
                    tvResult.setTextColor(getColor(android.R.color.holo_red_dark))

                    Toast.makeText(this@StaffValidationActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnScanQr.isEnabled = true
                }
            }
        }
    }
}
