package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.CalendarAdapter
import app.src.adapters.OrderItemsAdapter
import app.src.adapters.ReceiptAdapter
import app.src.adapters.TopProductsAdapter
import app.src.adapters.WeeklyPatternAdapter
import app.src.data.local.AppDatabase
import app.src.data.local.entities.OrderEntity
import app.src.data.models.ProductFrequency
import app.src.data.models.UserInsights
import app.src.utils.CartManager
import app.src.utils.SessionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * User Insights Dashboard Activity
 *
 * Muestra estadísticas y patrones de compra del usuario
 * Implementa Business Question: ¿Cuál es el producto más frecuente y patrón semanal?
 *
 * Usa Kotlin Coroutines con viewModelScope.launch para cargar datos
 */
class UserInsightsActivity : BaseActivity() {

    private val viewModel: UserInsightsViewModel by viewModels()
    private var currentInsights: UserInsights? = null

    // Views principales
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: View
    private lateinit var emptyStateLayout: View
    private lateinit var errorLayout: View
    private lateinit var btnRetry: MaterialButton

    // Statistics Cards
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvAvgOrderValue: TextView
    private lateinit var tvUniqueProducts: TextView
    private lateinit var tvLargestOrder: TextView

    // BQ Results
    private lateinit var tvFavoriteProduct: TextView
    private lateinit var tvFavoriteProductCount: TextView
    private lateinit var tvMostActiveDay: TextView
    private lateinit var rvTopProducts: RecyclerView
    private lateinit var rvWeeklyPattern: RecyclerView

    private lateinit var topProductsAdapter: TopProductsAdapter
    private lateinit var weeklyPatternAdapter: WeeklyPatternAdapter

    companion object {
        private const val TAG = "UserInsightsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_insights)

        initializeViews()
        setupRecyclerViews()
        setupObservers()
        setupListeners()

        // Cargar datos usando coroutines en viewModelScope
        viewModel.loadUserInsights()
    }

    private fun initializeViews() {
        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Estadísticas"

        // Layouts
        progressBar = findViewById(R.id.progress_bar)
        contentLayout = findViewById(R.id.content_layout)
        emptyStateLayout = findViewById(R.id.empty_state_layout)
        errorLayout = findViewById(R.id.error_layout)
        btnRetry = findViewById(R.id.btn_retry)

        // Statistics
        tvTotalOrders = findViewById(R.id.tv_total_orders)
        tvTotalSpent = findViewById(R.id.tv_total_spent)
        tvAvgOrderValue = findViewById(R.id.tv_avg_order_value)
        tvUniqueProducts = findViewById(R.id.tv_unique_products)
        tvLargestOrder = findViewById(R.id.tv_largest_order)

        // BQ Results
        tvFavoriteProduct = findViewById(R.id.tv_favorite_product)
        tvFavoriteProductCount = findViewById(R.id.tv_favorite_product_count)
        tvMostActiveDay = findViewById(R.id.tv_most_active_day)
        rvTopProducts = findViewById(R.id.rv_top_products)
        rvWeeklyPattern = findViewById(R.id.rv_weekly_pattern)
    }

    private fun setupRecyclerViews() {
        // Top Products RecyclerView con click listener
        topProductsAdapter = TopProductsAdapter { product ->
            onTopProductClick(product)
        }
        rvTopProducts.apply {
            layoutManager = LinearLayoutManager(this@UserInsightsActivity)
            adapter = topProductsAdapter
        }

        // Weekly Pattern RecyclerView
        weeklyPatternAdapter = WeeklyPatternAdapter()
        rvWeeklyPattern.apply {
            layoutManager = LinearLayoutManager(this@UserInsightsActivity)
            adapter = weeklyPatternAdapter
        }
    }

    private fun setupObservers() {
        viewModel.insightsState.observe(this) { state ->
            when (state) {
                is UserInsightsViewModel.InsightsState.Loading -> {
                    showLoading()
                }
                is UserInsightsViewModel.InsightsState.Success -> {
                    showContent()
                    displayInsights(state.insights)
                }
                is UserInsightsViewModel.InsightsState.Error -> {
                    showError(state.message)
                }
                is UserInsightsViewModel.InsightsState.NoData -> {
                    showEmptyState()
                }
            }
        }
    }

    private fun setupListeners() {
        btnRetry.setOnClickListener {
            viewModel.refreshInsights()
        }

        // 6. Órdenes totales -> Order History (clickear en el card parent)
        tvTotalOrders.parent.let { parent ->
            (parent.parent as? View)?.setOnClickListener {
                startActivity(Intent(this, OrderHistoryActivity::class.java))
            }
        }

        // 7. Gasto total -> Mostrar recibo detallado
        tvTotalSpent.parent.let { parent ->
            (parent.parent as? View)?.setOnClickListener {
                currentInsights?.let { showTotalSpentBreakdown(it) }
            }
        }

        // Promedio por orden
        tvAvgOrderValue.parent.let { parent ->
            (parent.parent as? View)?.setOnClickListener {
                currentInsights?.let {
                    Toast.makeText(this, "Promedio por orden: ${tvAvgOrderValue.text}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. Productos únicos -> Mostrar lista de productos únicos
        tvUniqueProducts.parent.let { parent ->
            (parent.parent as? View)?.setOnClickListener {
                showUniqueProductsDialog()
            }
        }

        // 8. Orden más grande -> Mostrar detalles
        tvLargestOrder.parent.let { parent ->
            (parent.parent as? View)?.setOnClickListener {
                showLargestOrderDetails()
            }
        }

        // 3. Producto favorito -> Mostrar imagen y mensaje
        tvFavoriteProduct.setOnClickListener {
            currentInsights?.topProducts?.firstOrNull()?.let { product ->
                showFavoriteProductDialog(product)
            }
        }

        tvFavoriteProductCount.setOnClickListener {
            currentInsights?.topProducts?.firstOrNull()?.let { product ->
                showFavoriteProductDialog(product)
            }
        }

        // 2. Día más activo -> Mostrar calendario con intensidad
        tvMostActiveDay.setOnClickListener {
            showOrderCalendarDialog()
        }

        // 4. Patrón semanal card -> Historial de órdenes
        findViewById<View>(R.id.rv_weekly_pattern)?.parent?.let { parent ->
            (parent as? View)?.setOnClickListener {
                startActivity(Intent(this, OrderHistoryActivity::class.java))
            }
        }
    }

    private fun displayInsights(insights: UserInsights) {
        currentInsights = insights
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        // Display general statistics
        tvTotalOrders.text = insights.totalOrders.toString()
        tvTotalSpent.text = currencyFormat.format(insights.totalSpent)
        tvAvgOrderValue.text = currencyFormat.format(insights.averageOrderValue)
        tvUniqueProducts.text = insights.uniqueProducts.toString()
        tvLargestOrder.text = currencyFormat.format(insights.largestOrderValue)

        // BQ1: Display favorite product
        val favoriteProduct = insights.topProducts.firstOrNull()
        if (favoriteProduct != null) {
            tvFavoriteProduct.text = favoriteProduct.productName
            tvFavoriteProductCount.text = "Comprado ${favoriteProduct.orderCount} veces"
        } else {
            tvFavoriteProduct.text = "N/A"
            tvFavoriteProductCount.text = ""
        }

        // BQ2: Display most active day
        val mostActiveDay = insights.weeklyPattern.firstOrNull()
        if (mostActiveDay != null) {
            tvMostActiveDay.text = "${mostActiveDay.getDayName()} (${mostActiveDay.orderCount} órdenes)"
        } else {
            tvMostActiveDay.text = "N/A"
        }

        // Display top products list
        topProductsAdapter.submitList(insights.topProducts)

        // Display weekly pattern
        weeklyPatternAdapter.submitList(insights.weeklyPattern)
    }

    // 1. Click en producto del top 5 -> Agregar al carrito
    private fun onTopProductClick(product: ProductFrequency) {
        lifecycleScope.launch {
            try {
                // Obtener el producto completo desde la base de datos o API
                val productoCompleto = withContext(Dispatchers.IO) {
                    // Crear producto con la imagen que viene en ProductFrequency
                    app.src.data.models.Producto(
                        id = product.productId,
                        nombre = product.productName,
                        descripcion = null,
                        imagenUrl = product.imagenUrl, // ✅ Ahora incluye la imagen
                        precio = product.totalSpent / product.totalQuantity,
                        disponible = true,
                        idTipo = 1,
                        tipoProducto = app.src.data.models.TipoProducto(1, "General")
                    )
                }

                // Agregar al carrito
                CartManager.addProduct(productoCompleto, 1)

                Toast.makeText(
                    this@UserInsightsActivity,
                    "✅ ${product.productName} agregado al carrito",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@UserInsightsActivity,
                    "Error al agregar al carrito",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 2. Mostrar calendario con intensidad de órdenes
    private fun showOrderCalendarDialog() {
        lifecycleScope.launch {
            val userId = SessionManager.getUserId(this@UserInsightsActivity)
            val orders = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@UserInsightsActivity)
                    .orderDao()
                    .getOrdersByUser(userId)
                    .firstOrNull() // Flow<List<OrderEntity>> -> List<OrderEntity>?
            } ?: emptyList()

            // Agrupar órdenes por fecha
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val ordersByDate = orders.groupBy { order ->
                dateFormat.format(Date(order.createdAt))
            }

            // Crear el diálogo
            val dialogView = layoutInflater.inflate(R.layout.dialog_order_calendar, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rv_calendar)

            val calendarItems = ordersByDate.map { (date, orderList) ->
                CalendarItem(date, orderList.size)
            }.sortedByDescending { it.orderCount }

            recyclerView.layoutManager = LinearLayoutManager(this@UserInsightsActivity)
            recyclerView.adapter = CalendarAdapter(calendarItems)

            AlertDialog.Builder(this@UserInsightsActivity)
                .setTitle("📅 Historial de Actividad")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    // 3. Mostrar producto favorito con imagen
    private fun showFavoriteProductDialog(product: ProductFrequency) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_favorite_product, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.iv_product_image)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)

        tvMessage.text = "¡Gracias por ordenar\n${product.productName}\n${product.orderCount} veces! 🎉"

        // Cargar imagen del producto directamente desde ProductFrequency
        // que ahora incluye la URL de la imagen almacenada en order_items
        Glide.with(this@UserInsightsActivity)
            .load(product.imagenUrl)
            .placeholder(R.drawable.ic_store_24)
            .error(R.drawable.ic_store_24)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(imageView)

        // Mostrar el diálogo
        AlertDialog.Builder(this@UserInsightsActivity)
            .setTitle("⭐ Tu Producto Favorito")
            .setView(dialogView)
            .setPositiveButton("Agregar al Carrito") { _, _ ->
                onTopProductClick(product)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // 5. Mostrar productos únicos
    private fun showUniqueProductsDialog() {
        lifecycleScope.launch {
            val userId = SessionManager.getUserId(this@UserInsightsActivity)
            val uniqueProducts = withContext(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(this@UserInsightsActivity).insightsDao()
                dao.getMostFrequentProducts(userId, limit = 100) // Todos los productos
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_unique_products, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rv_unique_products)

            recyclerView.layoutManager = LinearLayoutManager(this@UserInsightsActivity)
            recyclerView.adapter = TopProductsAdapter { product ->
                onTopProductClick(product)
            }.apply {
                submitList(uniqueProducts)
            }

            AlertDialog.Builder(this@UserInsightsActivity)
                .setTitle("🎯 Productos Únicos (${uniqueProducts.size})")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    // 7. Mostrar desglose de gasto total
    private fun showTotalSpentBreakdown(insights: UserInsights) {
        lifecycleScope.launch {
            val userId = SessionManager.getUserId(this@UserInsightsActivity)
            val orders = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@UserInsightsActivity)
                    .orderDao()
                    .getOrdersByUser(userId)
                    .firstOrNull() // Flow<List<OrderEntity>> -> List<OrderEntity>?
            } ?: emptyList()

            val dialogView = layoutInflater.inflate(R.layout.dialog_spending_receipt, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rv_receipt_items)
            val tvTotal = dialogView.findViewById<TextView>(R.id.tv_receipt_total)

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            recyclerView.layoutManager = LinearLayoutManager(this@UserInsightsActivity)

            val receiptItems = orders.map { order ->
                ReceiptItem(
                    orderNumber = order.id,
                    date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(order.createdAt)),
                    amount = order.total
                )
            }

            recyclerView.adapter = ReceiptAdapter(receiptItems)
            tvTotal.text = currencyFormat.format(insights.totalSpent)

            AlertDialog.Builder(this@UserInsightsActivity)
                .setTitle("💰 Desglose de Gasto Total")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    // 8. Mostrar detalles de la orden más grande
    private fun showLargestOrderDetails() {
        lifecycleScope.launch {
            val userId = SessionManager.getUserId(this@UserInsightsActivity)
            val largestOrder = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@UserInsightsActivity)
                    .insightsDao()
                    .getLargestOrder(userId)
            }

            if (largestOrder == null) {
                Toast.makeText(this@UserInsightsActivity, "No hay órdenes disponibles", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val orderItems = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@UserInsightsActivity)
                    .orderDao()
                    .getOrderItems(largestOrder.id)
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_largest_order, null)
            val tvOrderNumber = dialogView.findViewById<TextView>(R.id.tv_order_number)
            val tvOrderDate = dialogView.findViewById<TextView>(R.id.tv_order_date)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rv_order_items)
            val tvOrderTotal = dialogView.findViewById<TextView>(R.id.tv_order_total)

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            tvOrderNumber.text = "Orden #${largestOrder.id}"
            tvOrderDate.text = dateFormat.format(Date(largestOrder.createdAt))

            recyclerView.layoutManager = LinearLayoutManager(this@UserInsightsActivity)
            recyclerView.adapter = OrderItemsAdapter(orderItems)

            tvOrderTotal.text = currencyFormat.format(largestOrder.total)

            AlertDialog.Builder(this@UserInsightsActivity)
                .setTitle("🏆 Orden Más Grande")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        errorLayout.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        errorLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        contentLayout.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE

        findViewById<TextView>(R.id.tv_error_message).text = message
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// Data classes para los adapters
data class CalendarItem(val date: String, val orderCount: Int)
data class ReceiptItem(val orderNumber: Int, val date: String, val amount: Double)
