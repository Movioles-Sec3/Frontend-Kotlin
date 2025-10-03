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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.CartAdapter
import app.src.data.api.ApiClient
import app.src.data.models.DetalleCompraRequest
import app.src.utils.CartManager
import app.src.utils.SessionManager

class OrderSummaryActivity : AppCompatActivity() {

    private val viewModel: CompraViewModel by viewModels()
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

        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Inicializar vistas
        recyclerView = findViewById(R.id.rv_cart_items)
        progressBar = findViewById(R.id.progress_bar)
        tvEmptyCart = findViewById(R.id.tv_empty_cart)
        tvSubtotal = findViewById(R.id.tv_subtotal_value)
        tvTotal = findViewById(R.id.tv_total_value)
        btnCheckout = findViewById(R.id.btn_checkout)
        btnBackToHome = findViewById(R.id.btn_back_to_home)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapter(CartManager.getItems()) { productoId ->
            CartManager.removeProduct(productoId)
            updateCartDisplay()
        }
        recyclerView.adapter = adapter

        // Actualizar display inicial
        updateCartDisplay()

        // Observer del ViewModel
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CompraUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnCheckout.isEnabled = false
                }
                is CompraUiState.Success -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled = true

                    // Limpiar carrito
                    CartManager.clear()

                    // Navegar a OrderPickupActivity con el código QR
                    val compra = state.compra
                    val qrCode = compra.qr?.codigoQrHash

                    if (qrCode != null) {
                        val intent = Intent(this, OrderPickupActivity::class.java)
                        intent.putExtra("qr_code", qrCode)
                        intent.putExtra("compra_id", compra.id)
                        intent.putExtra("total", compra.total)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Order created successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                is CompraUiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnCheckout.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
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

            // Verificar saldo del usuario
            val userSaldo = SessionManager.getUserSaldo(this)
            val total = CartManager.getTotal()

            if (userSaldo < total) {
                Toast.makeText(
                    this,
                    "Insufficient balance. Please recharge your account.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Crear lista de productos para la compra
            val productos = CartManager.getItems().map { item ->
                DetalleCompraRequest(
                    idProducto = item.producto.id,
                    cantidad = item.cantidad
                )
            }

            // Crear compra
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
        tvSubtotal.text = "$${String.format("%.2f", total)}"
        tvTotal.text = "$${String.format("%.2f", total)}"
    }
}
