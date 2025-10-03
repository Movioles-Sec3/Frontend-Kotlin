package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.CartAdapter
import app.src.data.api.ApiClient
import app.src.data.models.DetalleCompraRequest
import app.src.data.repositories.Result
import app.src.data.repositories.UsuarioRepository
import app.src.utils.CartManager
import app.src.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Locale

class OrderSummaryActivity : AppCompatActivity() {

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
                }
                is CompraUiState.Success -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled = true

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
}
