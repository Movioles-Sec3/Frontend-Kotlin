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
import app.src.adapters.ProductAdapter
import app.src.data.api.ApiClient
import app.src.utils.SessionManager

class ProductActivity : AppCompatActivity() {

    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var categoryNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)
        
        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Obtener filtro de categoría desde Intent
        val categoryId = intent.getIntExtra("category_id", -1)
        val categoryName = intent.getStringExtra("category_name")

        // Inicializar vistas
        recyclerView = findViewById(R.id.rv_products)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.tv_error)
        categoryNameTextView = findViewById(R.id.tv_category_name)

        // Mostrar nombre de categoría o "All Products"
        if (categoryId != -1 && categoryName != null) {
            categoryNameTextView.text = categoryName
        } else {
            categoryNameTextView.text = "All Products"
        }

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(emptyList()) { product ->
            // Click en "Add to Cart"
            Toast.makeText(
                this,
                "${product.nombre} added to cart!",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: Implementar lógica de carrito
        }
        recyclerView.adapter = adapter

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
                        errorTextView.text = "No products found in this category"
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

        // Botón para volver al Home
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}
