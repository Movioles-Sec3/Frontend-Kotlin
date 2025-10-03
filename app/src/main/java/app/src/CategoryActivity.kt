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
import app.src.adapters.CategoryAdapter
import app.src.data.api.ApiClient
import app.src.utils.SessionManager

class CategoryActivity : AppCompatActivity() {

    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var adapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        
        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Inicializar vistas
        recyclerView = findViewById(R.id.rv_categories)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.tv_error)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CategoryAdapter(emptyList()) { category ->
            // Click en categoría - navegar a productos filtrados por categoría
            val intent = Intent(this, ProductActivity::class.java)
            intent.putExtra("category_id", category.id)
            intent.putExtra("category_name", category.nombre)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Observer del ViewModel
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CategoryUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                }
                is CategoryUiState.Success -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    errorTextView.visibility = View.GONE

                    adapter.updateCategories(state.categories)
                }
                is CategoryUiState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = "Error: ${state.message}"
                }
                else -> {}
            }
        }

        // Cargar categorías
        viewModel.loadCategories()

        // Botón para volver al Home
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}