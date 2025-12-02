package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.ProductAdapter
import app.src.data.api.ApiClient
import app.src.data.repositories.FavoritoRepository
import app.src.utils.CartManager
import app.src.utils.ConversionesDialogManager
import app.src.utils.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ProductActivity : BaseActivity() {

    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var categoryNameTextView: TextView
    private lateinit var fabCart: FloatingActionButton
    private lateinit var conversionesDialogManager: ConversionesDialogManager
    private lateinit var searchView: SearchView

    // Repository para manejar favoritos
    private lateinit var favoritoRepository: FavoritoRepository
    private var favoriteProductIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)
        
        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Inicializar manager de conversiones
        conversionesDialogManager = ConversionesDialogManager(this, lifecycleScope)

        // Inicializar repositorio de favoritos
        favoritoRepository = FavoritoRepository(this)

        // Obtener filtro de categoría desde Intent
        val categoryId = intent.getIntExtra("category_id", -1)
        val categoryName = intent.getStringExtra("category_name")

        // Inicializar vistas
        recyclerView = findViewById(R.id.rv_products)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.tv_error)
        categoryNameTextView = findViewById(R.id.tv_category_name)
        fabCart = findViewById(R.id.fab_cart)
        searchView = findViewById(R.id.search_view)

        // Mostrar nombre de categoría o "All Products"
        if (categoryId != -1 && categoryName != null) {
            categoryNameTextView.text = categoryName
        } else {
            categoryNameTextView.text = "All Products"
        }

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(
            products = emptyList(),
            onAddToCart = { product ->
                // Agregar producto al carrito
                CartManager.addProduct(product, 1)

                // Trackear que se agregó desde Productos
                FavoritesAnalyticsActivity.trackAddFromProducts(this)

                Toast.makeText(
                    this,
                    "${product.nombre} added to cart!",
                    Toast.LENGTH_SHORT
                ).show()
                updateCartBadge()
            },
            onShowConversions = { product ->
                // Mostrar conversiones de precio
                conversionesDialogManager.mostrarConversiones(product.id, product.nombre)
            },
            onToggleFavorite = { product ->
                // Toggle favorito
                toggleFavorite(product)
            },
            favoriteProductIds = favoriteProductIds
        )
        recyclerView.adapter = adapter

        // Observar favoritos desde la base de datos
        observeFavorites()

        // Observer del ViewModel
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProductoUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                }
                is ProductoUiState.Success -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    errorTextView.visibility = View.GONE

                    if (state.productos.isEmpty()) {
                        errorTextView.visibility = View.VISIBLE
                        errorTextView.text = "No products found"
                        recyclerView.visibility = View.GONE
                    } else {
                        adapter.updateProducts(state.productos)
                    }
                }
                is ProductoUiState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = "Error: ${state.message}"
                }
                else -> {}
            }
        }

        // Cargar productos (con o sin filtro)
        if (categoryId != -1) {
            viewModel.cargarProductos(categoryId)
        } else {
            viewModel.cargarProductos(null)
        }

        // Configurar SearchView: buscar al enviar, resetear si el texto queda vacío
        setupSearchView(categoryId)

        // FAB para ir al carrito
        fabCart.setOnClickListener {
            val intent = Intent(this, OrderSummaryActivity::class.java)
            startActivity(intent)
        }

        // Botón para volver al Home o a la Categoría
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            if (categoryId != -1) {
                // Vino de una categoría, volver a Categoría
                val intent = Intent(this, CategoryActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Vino del menú principal, volver a Home
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
        }

        // Actualizar badge inicial
        updateCartBadge()
    }

    private fun observeFavorites() {
        // Observar cambios en favoritos desde Room Database
        lifecycleScope.launch {
            favoritoRepository.getAllFavoritos().collect { favoritos ->
                favoriteProductIds = favoritos.map { it.id }.toMutableSet()
                adapter.updateFavorites(favoriteProductIds)
            }
        }
    }

    private fun toggleFavorite(product: app.src.data.models.Producto) {
        lifecycleScope.launch {
            val result = favoritoRepository.toggleFavorito(product)
            when (result) {
                is app.src.data.repositories.Result.Success -> {
                    val isFavorite = result.data
                    val mensaje = if (isFavorite) {
                        "❤️ ${product.nombre} agregado a favoritos"
                    } else {
                        "💔 ${product.nombre} eliminado de favoritos"
                    }
                    Toast.makeText(this@ProductActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
                is app.src.data.repositories.Result.Error -> {
                    Toast.makeText(
                        this@ProductActivity,
                        "Error: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    private fun setupSearchView(categoryId: Int) {
        // Cuando el usuario envía la búsqueda
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val q = query ?: ""
                if (q.isNotBlank()) {
                    viewModel.buscarProductos(q)
                    searchView.clearFocus()
                } else {
                    // si está vacío, recargar la lista por defecto
                    if (categoryId != -1) viewModel.cargarProductos(categoryId) else viewModel.cargarProductos(null)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val t = newText ?: ""
                if (t.isBlank()) {
                    // Si el usuario borró el texto, restaurar la lista completa
                    if (categoryId != -1) viewModel.cargarProductos(categoryId) else viewModel.cargarProductos(null)
                    return true
                }
                // No buscar en cada pulsación para evitar llamadas excesivas; esperar al submit
                return false
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

    private fun updateCartBadge() {
        val itemCount = CartManager.getItemCount()
        fabCart.contentDescription = if (itemCount > 0) {
            "Cart: $itemCount items"
        } else {
            "Cart is empty"
        }
    }
}
