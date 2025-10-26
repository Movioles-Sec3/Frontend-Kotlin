package app.src

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.CartAdapter
import app.src.data.api.ApiClient
import app.src.data.models.DetalleCompraRequest
import app.src.data.repositories.Result
import app.src.data.repositories.UsuarioRepository
import app.src.utils.AnalyticsLogger
import app.src.utils.CartManager
import app.src.utils.NetworkUtils
import app.src.utils.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class OrderSummaryActivity : BaseActivity() {

    private val viewModel: CompraViewModel by viewModels()
    private val usuarioRepo = UsuarioRepository()
    private lateinit var adapter: CartAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button
    private lateinit var btnBackToHome: Button

    private var payTapStartTime: Long = 0

    companion object {
        private const val TAG = "OrderSummaryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_summary)

        // Load session token
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Initialize views
        recyclerView = findViewById(R.id.rv_cart_items)
        progressBar = findViewById(R.id.progress_bar)
        tvEmptyCart = findViewById(R.id.tv_empty_cart)
        tvSubtotal = findViewById(R.id.tv_subtotal_value)
        tvTotal = findViewById(R.id.tv_total_value)
        btnCheckout = findViewById(R.id.btn_checkout)
        btnBackToHome = findViewById(R.id.btn_back_to_home)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapter(CartManager.getItems()) { productoId ->
            CartManager.removeProduct(productoId)
            updateCartDisplay()
        }
        recyclerView.adapter = adapter

        // Update initial display
        updateCartDisplay()

        // ViewModel Observer
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CompraUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnCheckout.isEnabled = false
                    Log.d(TAG, "⏳ Procesando pago...")
                }
                is CompraUiState.Success -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled = true

                    // Emitir evento de pago completado exitosamente
                    Log.d(TAG, "✅ Pago exitoso - Emitiendo evento payment_completed")
                    emitPaymentCompletedEvent(success = true, paymentMethod = "wallet")

                    // Clear cart
                    CartManager.clear()

                    // Update user balance from API
                    updateUserBalance {
                        // Navigate to OrderPickupActivity with QR code
                        val compra = state.compra
                        val qrCode = compra.qr?.codigoQrHash

                        if (qrCode != null) {
                            val intent = Intent(this, OrderPickupActivity::class.java)
                            intent.putExtra("qr_code", qrCode)
                            intent.putExtra("compra_id", compra.id)
                            intent.putExtra("total", compra.total)
                            intent.putExtra("estado", compra.estado.name)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Order created successfully but no QR code was generated", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
                is CompraUiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled = true

                    // Emitir evento de pago fallido
                    Log.d(TAG, "❌ Pago fallido - Emitiendo evento payment_completed (success=false)")
                    emitPaymentCompletedEvent(success = false, paymentMethod = "wallet")

                    val errorMessage = when {
                        state.message.contains("Saldo insuficiente", ignoreCase = true) ->
                            "Insufficient balance. Please recharge your account."
                        state.message.contains("disponible", ignoreCase = true) ->
                            "One or more products are not available."
                        else -> "Error: ${state.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled = true
                }
            }
        }

        btnCheckout.setOnClickListener {
            if (CartManager.isEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ VALIDAR CONEXIÓN A INTERNET ANTES DE PROCESAR COMPRA
            if (!NetworkUtils.isNetworkAvailable(this)) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Sin Conexión a Internet")
                    .setMessage("No es posible realizar la compra sin conexión a internet.\n\nTu carrito se ha guardado y estará disponible cuando vuelvas a tener conexión.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Entendido") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                Log.w(TAG, "⚠️ Intento de compra sin internet - Carrito guardado")
                return@setOnClickListener
            }

            // Verify user balance
            val userSaldo = SessionManager.getUserSaldo(this)
            val total = CartManager.getTotal()

            if (userSaldo < total) {
                Toast.makeText(
                    this,
                    "Insufficient balance. Please recharge your account.\nCurrent balance: ${String.format(Locale.US, "$%.2f", userSaldo)}\nTotal: ${String.format(Locale.US, "$%.2f", total)}",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Iniciar el timer de pago
            payTapStartTime = System.currentTimeMillis()
            Log.d(TAG, "💳 Pago iniciado - Timer iniciado en: $payTapStartTime")

            // Create products list for the purchase
            val productos = CartManager.getItems().map { item ->
                DetalleCompraRequest(
                    idProducto = item.producto.id,
                    cantidad = item.cantidad
                )
            }

            // Create purchase
            viewModel.crearCompra(productos)
        }

        btnBackToHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    private fun updateCartDisplay() {
        val items = CartManager.getItems()

        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyCart.visibility = View.VISIBLE
            btnCheckout.isEnabled = false
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyCart.visibility = View.GONE
            btnCheckout.isEnabled = true
            adapter.updateItems(items)
        }

        val total = CartManager.getTotal()
        tvSubtotal.text = String.format(Locale.US, "$%.2f", total)
        tvTotal.text = String.format(Locale.US, "$%.2f", total)
    }

    private fun updateUserBalance(onComplete: () -> Unit) {
        lifecycleScope.launch {
            when (val result = usuarioRepo.obtenerPerfil()) {
                is Result.Success -> {
                    val usuario = result.data
                    // Update session with new balance
                    SessionManager.saveUserData(
                        this@OrderSummaryActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )
                    onComplete()
                }
                is Result.Error -> {
                    // If we can't get the updated balance, still proceed
                    Toast.makeText(
                        this@OrderSummaryActivity,
                        "Warning: Could not update balance",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete()
                }
                else -> {
                    onComplete()
                }
            }
        }
    }

    private fun emitPaymentCompletedEvent(success: Boolean, paymentMethod: String?) {
        val duration = System.currentTimeMillis() - payTapStartTime
        Log.d(TAG, "📊 Emitiendo payment_completed - Duration: ${duration}ms, Success: $success")
        AnalyticsLogger.logPaymentCompleted(
            context = this,
            durationMs = duration,
            success = success,
            paymentMethod = paymentMethod
        )
    }
}
