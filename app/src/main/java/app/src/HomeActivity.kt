package app.src

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.RecommendedProductsAdapter
import app.src.data.api.ApiClient
import app.src.data.models.Producto
import app.src.data.repositories.Result
import app.src.data.repositories.UsuarioRepository
import app.src.utils.AnalyticsLogger
import app.src.utils.SessionManager
import app.src.utils.ConversionesDialogManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private val usuarioRepo = UsuarioRepository()
    private val homeViewModel: HomeViewModel by viewModels()

    // Views para productos recomendados
    private lateinit var rvRecommendedProducts: RecyclerView
    private lateinit var pbRecommendedLoading: ProgressBar
    private lateinit var tvRecommendedError: TextView
    private lateinit var tvVerTodos: TextView

    // Botón de modo nocturno
    private lateinit var btnNightMode: MaterialButton

    private var recommendedProductsAdapter: RecommendedProductsAdapter? = null

    private var menuLoadStartTime: Long = 0
    private var menuReadyEventEmitted = false

    // NUEVO: Manager para mostrar conversiones de precio
    private lateinit var conversionesDialogManager: ConversionesDialogManager

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar el tema antes de setContentView
        applyThemeFromPreferences()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Load session token
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Inicializar el manager de conversiones
        conversionesDialogManager = ConversionesDialogManager(this, lifecycleScope)

        menuLoadStartTime = System.currentTimeMillis()
        Log.d(TAG, "🚀 HomeActivity iniciada - Timer iniciado en: $menuLoadStartTime")

        initializeViews()
        setupRecommendedProducts()
        setupObservers()
        setupExistingFunctionality()
        setupNightModeButton()
    }

    private fun applyThemeFromPreferences() {
        val isNightMode = SessionManager.getNightMode(this)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun initializeViews() {
        // Views existentes
        val userName = SessionManager.getUserName(this)
        val userSaldo = SessionManager.getUserSaldo(this)

        findViewById<TextView>(R.id.tv_welcome)?.text = "Hello, $userName"
        findViewById<TextView>(R.id.tv_saldo)?.text = String.format(Locale.US, "Balance: $%.2f", userSaldo)

        // Views para productos recomendados
        rvRecommendedProducts = findViewById(R.id.rv_recommended_products)
        pbRecommendedLoading = findViewById(R.id.pb_recommended_loading)
        tvRecommendedError = findViewById(R.id.tv_recommended_error)
        tvVerTodos = findViewById(R.id.tv_ver_todos)

        // Botón de modo nocturno
        btnNightMode = findViewById(R.id.btn_night_mode)
    }

    private fun setupRecommendedProducts() {
        // Configurar RecyclerView con LinearLayoutManager horizontal
        rvRecommendedProducts.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Click en "Ver todos" - navegar a ProductActivity
        tvVerTodos.setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }
    }

    private fun setupObservers() {
        // Observer para el estado de productos recomendados
        homeViewModel.uiState.observe(this) { state ->
            when (state) {
                is HomeUiState.Loading -> {
                    pbRecommendedLoading.isVisible = true
                    rvRecommendedProducts.isVisible = false
                    tvRecommendedError.isVisible = false
                    Log.d(TAG, "⏳ Cargando productos recomendados...")
                }
                is HomeUiState.Success -> {
                    pbRecommendedLoading.isVisible = false
                    rvRecommendedProducts.isVisible = true
                    tvRecommendedError.isVisible = false

                    setupRecommendedProductsAdapter(state.productosRecomendados)

                    // Emitir evento menu_ready cuando el menú está listo
                    if (!menuReadyEventEmitted) {
                        Log.d(TAG, "✅ Productos cargados - Emitiendo evento menu_ready")
                        emitMenuReadyEvent()
                        menuReadyEventEmitted = true
                    } else {
                        Log.d(TAG, "⚠️ Evento menu_ready ya fue emitido anteriormente")
                    }
                }
                is HomeUiState.Error -> {
                    pbRecommendedLoading.isVisible = false
                    rvRecommendedProducts.isVisible = false
                    tvRecommendedError.isVisible = true
                    tvRecommendedError.text = "Error: ${state.message}"
                    Log.e(TAG, "❌ Error al cargar productos: ${state.message}")

                    // NUEVO: Emitir evento menu_ready incluso si hay error
                    // El menú principal sigue siendo usable (puede navegar a categorías, productos, etc.)
                    if (!menuReadyEventEmitted) {
                        Log.d(TAG, "⚠️ Error al cargar productos recomendados, pero emitiendo evento menu_ready (menú base es usable)")
                        emitMenuReadyEvent()
                        menuReadyEventEmitted = true
                    }
                }
            }
        }
    }

    private fun setupRecommendedProductsAdapter(productos: List<Producto>) {
        recommendedProductsAdapter = RecommendedProductsAdapter(
            productos = productos,
            onProductClick = { producto ->
                // Navegar a detalles del producto (si tienes una activity de detalles)
                homeViewModel.onProductoRecomendadoClick(producto)
                Toast.makeText(this, "Producto: ${producto.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAddToCartClick = { producto ->
                // Agregar al carrito y navegar a OrderSummaryActivity
                agregarAlCarrito(producto)
            },
            onShowConversions = { producto ->
                // Mostrar conversiones de precio
                conversionesDialogManager.mostrarConversiones(producto.id, producto.nombre)
            }
        )

        rvRecommendedProducts.adapter = recommendedProductsAdapter
    }

    private fun agregarAlCarrito(producto: Producto) {
        if (!producto.disponible) {
            Toast.makeText(this, "Product not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar diálogo de confirmación con cantidad
        showAddToCartDialog(producto)
    }

    private fun showAddToCartDialog(producto: Producto) {
        val input = EditText(this).apply {
            hint = "Quantity"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        AlertDialog.Builder(this)
            .setTitle("Add to Cart")
            .setMessage("${producto.nombre}\nPrice: $${String.format(Locale.US, "%.0f", producto.precio)}\n\nEnter quantity:")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val cantidadStr = input.text.toString()
                val cantidad = cantidadStr.toIntOrNull()

                if (cantidad != null && cantidad > 0) {
                    // Usar CartManager para agregar el producto al carrito
                    app.src.utils.CartManager.addProduct(producto, cantidad)

                    Toast.makeText(this, "Added $cantidad ${producto.nombre} to cart", Toast.LENGTH_SHORT).show()

                    // Opcional: Navegar directamente al carrito
                    startActivity(Intent(this, OrderSummaryActivity::class.java))
                } else {
                    Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupExistingFunctionality() {
        // Recharge balance button
        findViewById<Button>(R.id.btn_recharge)?.setOnClickListener {
            showRechargeDialog()
        }

        // Navigation to each view
        findViewById<Button>(R.id.btn_categories).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_products).setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_order_summary).setOnClickListener {
            startActivity(Intent(this, OrderSummaryActivity::class.java))
        }
        
        // Order Pickup button - can be used to view order history
        findViewById<Button>(R.id.btn_order_pickup)?.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        // Logout button
        findViewById<Button>(R.id.btn_logout)?.setOnClickListener {
            logout()
        }

        // NUEVO: Botón para ver estadísticas de eventos (long press en el balance)
        findViewById<TextView>(R.id.tv_saldo)?.setOnLongClickListener {
            showAnalyticsInfo()
            true
        }
    }

    private fun showAnalyticsInfo() {
        val eventCount = AnalyticsLogger.getEventCount(this)
        val csvPath = AnalyticsLogger.getCSVFilePath(this)

        AlertDialog.Builder(this)
            .setTitle("📊 Analytics Events")
            .setMessage(
                "Total events saved: $eventCount\n\n" +
                "CSV file location:\n$csvPath\n\n" +
                "You can pull the CSV file using:\n" +
                "adb pull \"$csvPath\" ."
            )
            .setPositiveButton("OK", null)
            .setNeutralButton("Show in Logs") { _, _ ->
                Log.d(TAG, "📊 Analytics CSV Path: $csvPath")
                Log.d(TAG, "📊 Total Events: $eventCount")
                Toast.makeText(this, "CSV path logged. Check Logcat.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showRechargeDialog() {
        val input = EditText(this).apply {
            hint = "Enter amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(this)
            .setTitle("Recharge Balance")
            .setMessage("Enter the amount you want to add to your balance:")
            .setView(input)
            .setPositiveButton("Recharge") { _, _ ->
                val amountStr = input.text.toString()
                if (amountStr.isNotEmpty()) {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        rechargeBalance(amount)
                    } else {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rechargeBalance(amount: Double) {
        lifecycleScope.launch {
            when (val result = usuarioRepo.recargarSaldo(amount)) {
                is Result.Success -> {
                    val usuario = result.data
                    // Update local session
                    SessionManager.saveUserData(
                        this@HomeActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )
                    // Update UI
                    findViewById<TextView>(R.id.tv_saldo)?.text =
                        String.format(Locale.US, "Balance: $%.2f", usuario.saldo)

                    Toast.makeText(
                        this@HomeActivity,
                        String.format(Locale.US, "Balance recharged successfully! New balance: $%.2f", usuario.saldo),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is Result.Error -> {
                    Toast.makeText(
                        this@HomeActivity,
                        "Error recharging balance: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(
                        this@HomeActivity,
                        "Unknown error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun logout() {
        SessionManager.clearSession(this)
        ApiClient.setToken(null)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupNightModeButton() {
        updateNightModeButton()

        btnNightMode.setOnClickListener {
            toggleNightMode()
        }
    }

    private fun updateNightModeButton() {
        val isNightMode = SessionManager.getNightMode(this)

        if (isNightMode) {
            btnNightMode.setIconResource(R.drawable.ic_day_mode)
            btnNightMode.contentDescription = "Cambiar a modo día"
        } else {
            btnNightMode.setIconResource(R.drawable.ic_night_mode)
            btnNightMode.contentDescription = "Cambiar a modo noche"
        }
    }

    private fun toggleNightMode() {
        val currentMode = SessionManager.getNightMode(this)
        val newMode = !currentMode

        // Guardar la nueva preferencia
        SessionManager.saveNightMode(this, newMode)

        // Aplicar el nuevo tema
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Mostrar mensaje de confirmación
        val modeText = if (newMode) "nocturno" else "día"
        Toast.makeText(this, "Modo $modeText activado", Toast.LENGTH_SHORT).show()

        // Actualizar el botón (aunque la actividad se recargará)
        updateNightModeButton()
    }

    private fun emitMenuReadyEvent() {
        val duration = System.currentTimeMillis() - menuLoadStartTime
        Log.d(TAG, "📊 Emitiendo menu_ready - Duration: ${duration}ms")
        AnalyticsLogger.logMenuReady(
            context = this,
            durationMs = duration,
            screen = "HomeActivity",
            appVersion = "1.0"
        )
    }
}
