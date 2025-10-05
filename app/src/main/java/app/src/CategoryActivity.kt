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

/**
 * Activity responsible for displaying the list of product categories.
 *
 * Responsibilities:
 * - Initializes session token (if available) so API requests are authenticated.
 * - Sets up a RecyclerView with a [CategoryAdapter] to render categories.
 * - Observes [CategoryViewModel.uiState] to update UI according to loading/success/error states.
 * - Navigates to the product listing screen when a category is selected.
 * - Provides a button to return to the Home screen.
 */
class CategoryActivity : AppCompatActivity() {

    // Lazily scoped ViewModel tied to this Activity's lifecycle.
    private val viewModel: CategoryViewModel by viewModels()

    // UI components
    private lateinit var adapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    /**
     * Android lifecycle callback. Sets up the UI and binds it to the ViewModel.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Load and inject the session token into the API client if it exists.
        // This enables authenticated requests for subsequent API calls made by repositories.
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Initialize views
        recyclerView = findViewById(R.id.rv_categories)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.tv_error)

        // Configure RecyclerView with a vertical linear layout.
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Create the adapter. When a category is clicked, navigate to the product list
        // filtered by the selected category.
        adapter = CategoryAdapter(emptyList()) { category ->
            val intent = Intent(this, ProductActivity::class.java).apply {
                putExtra("category_id", category.id)
                putExtra("category_name", category.nombre)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Observe the UI state exposed by the ViewModel and render the appropriate screen.
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CategoryUiState.Loading -> {
                    // Show progress while loading, hide content and error.
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                }
                is CategoryUiState.Success -> {
                    // Show the list and update the adapter with fetched categories.
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    errorTextView.visibility = View.GONE

                    adapter.updateCategories(state.categories)
                }
                is CategoryUiState.Error -> {
                    // Show an error message if the request failed.
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = "Error: ${state.message}"
                }
                else -> {
                    // No-op for additional states (if any) not handled explicitly.
                }
            }
        }

        // Trigger initial load of categories when the screen starts.
        viewModel.loadCategories()

        // Navigate back to the Home screen. CLEAR_TOP ensures we don't stack multiple Home instances.
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
    }
}