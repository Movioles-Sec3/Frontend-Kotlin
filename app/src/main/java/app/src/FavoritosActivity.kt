package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.ProductAdapter
import app.src.data.repositories.FavoritoRepository
import app.src.utils.CartManager
import app.src.utils.ConversionesDialogManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Activity para mostrar productos favoritos
 * Funciona 100% offline usando Room Database
 */
class FavoritosActivity : BaseActivity() {

    private val viewModel: FavoritosViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var emptyState: View
    private lateinit var countTextView: TextView
    private lateinit var btnClearAll: MaterialButton
    private lateinit var conversionesDialogManager: ConversionesDialogManager

    // Repository para manejar favoritos
    private lateinit var favoritoRepository: FavoritoRepository
    private var favoriteProductIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favoritos)

        // Inicializar repositorio de favoritos
        favoritoRepository = FavoritoRepository(this)

        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupButtons()

        // Inicializar manager de conversiones
        conversionesDialogManager = ConversionesDialogManager(this, lifecycleScope)
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rv_favoritos)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.tv_error)
        emptyState = findViewById(R.id.empty_state)
        countTextView = findViewById(R.id.tv_favoritos_count)
        btnClearAll = findViewById(R.id.btn_clear_all)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(
            products = emptyList(),
            onAddToCart = { product ->
                CartManager.addProduct(product, 1)

                // Trackear que se agregó desde Favoritos
                FavoritesAnalyticsActivity.trackAddFromFavorites(this)

                Toast.makeText(
                    this,
                    "${product.nombre} agregado al carrito",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onShowConversions = { product ->
                conversionesDialogManager.mostrarConversiones(product.id, product.nombre)
            },
            onToggleFavorite = { product ->
                // Toggle favorito (eliminar de favoritos)
                toggleFavorite(product)
            },
            favoriteProductIds = favoriteProductIds
        )
        recyclerView.adapter = adapter
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
                    Toast.makeText(this@FavoritosActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
                is app.src.data.repositories.Result.Error -> {
                    Toast.makeText(
                        this@FavoritosActivity,
                        "Error: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    private fun setupObservers() {
        // Observar cambios en favoritos
        viewModel.favoritos.observe(this) { favoritos ->
            // Actualizar IDs de favoritos
            favoriteProductIds = favoritos.map { it.id }.toMutableSet()

            // Actualizar adapter
            adapter.updateProducts(favoritos)
            adapter.updateFavorites(favoriteProductIds)

            updateCount(favoritos.size)

            // Mostrar/ocultar botón de limpiar
            btnClearAll.visibility = if (favoritos.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Observar estado de la UI
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is FavoritosUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                    emptyState.visibility = View.GONE
                }
                is FavoritosUiState.Success -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    errorTextView.visibility = View.GONE
                    emptyState.visibility = View.GONE
                }
                is FavoritosUiState.Empty -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                }
                is FavoritosUiState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = state.message
                    emptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun setupButtons() {
        // Botón volver
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // Botón limpiar todos
        btnClearAll.setOnClickListener {
            showClearAllDialog()
        }
    }

    private fun updateCount(count: Int) {
        countTextView.text = if (count == 1) {
            "$count producto"
        } else {
            "$count productos"
        }
    }

    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar favoritos")
            .setMessage("¿Estás seguro de que quieres eliminar todos los favoritos?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.clearAllFavoritos { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
