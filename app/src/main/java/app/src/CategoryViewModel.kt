package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.TipoProducto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.src.data.local.CatalogCacheManager
import android.util.Log

/**
 * UI state contract for the Category screen.
 *
 * Represents mutually exclusive render states the view can observe and render.
 */
sealed class CategoryUiState {
    /** No active operation. Useful as the initial state. */
    object Idle : CategoryUiState()

    /** Categories are being loaded. */
    object Loading : CategoryUiState()

    /**
     * Categories loaded successfully.
     *
     * @param categories List of available product types.
     */
    data class Success(val categories: List<TipoProducto>) : CategoryUiState()

    /**
     * An error occurred while loading categories.
     *
     * @param message Human-readable error message suitable for UI display.
     */
    data class Error(val message: String) : CategoryUiState()
}

/**
 * ViewModel responsible for loading and exposing product categories to the UI.
 *
 * Responsibilities:
 * - Coordinates data fetching from [ProductoRepository].
 * - Exposes lifecycle-aware [LiveData] with a unidirectional state model via [CategoryUiState].
 * - Encapsulates mutable state and exposes it as an immutable observable for the view layer.
 */
class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository that provides product-related data operations. */
    private val repo = ProductoRepository()

    /** Cache manager para almacenamiento local */
    private val catalogCache = CatalogCacheManager(application)

    // Backing property that holds the current UI state.
    private val _uiState = MutableLiveData<CategoryUiState>(CategoryUiState.Idle)

    /**
     * Public, immutable LiveData that the view observes to render the screen.
     */
    val uiState: LiveData<CategoryUiState> = _uiState

    companion object {
        private const val TAG = "CategoryViewModel"
    }

    /**
     * Initiates the loading of product categories.
     *
     * Estrategia: Cache-First
     * 1. Lee del caché local primero (instantáneo)
     * 2. Muestra datos cacheados si existen
     * 3. Actualiza desde red en background
     */
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading

            try {
                // 1) Intentar cargar desde caché primero
                val cachedData = withContext(Dispatchers.IO) {
                    val cacheKey = CatalogCacheManager.KEY_CATEGORIES_LIST
                    try {
                        catalogCache.getFromCache(cacheKey, Array<TipoProducto>::class.java)?.toList()
                    } catch (e: Exception) {
                        null
                    }
                }

                if (cachedData != null && cachedData.isNotEmpty()) {
                    // Mostrar datos del caché inmediatamente
                    Log.d(TAG, "✅ Cargando categorías desde CACHÉ: ${cachedData.size} categorías")
                    _uiState.value = CategoryUiState.Success(cachedData)
                }

                // 2) Actualizar desde red en background
                actualizarCategoriasDesdeRed(cachedData == null)

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar categorías: ${e.message}")
                // Solo mostrar error si no hay datos en caché
                if (_uiState.value !is CategoryUiState.Success) {
                    _uiState.value = CategoryUiState.Error("Sin conexión. Verifica tu red WiFi.")
                }
            }
        }
    }

    /**
     * Actualiza categorías desde la red y guarda en caché
     */
    private suspend fun actualizarCategoriasDesdeRed(showErrorIfFails: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val result = repo.listarTipos(getApplication())

                when (result) {
                    is Result.Success -> {
                        // Guardar en caché
                        val cacheKey = CatalogCacheManager.KEY_CATEGORIES_LIST
                        catalogCache.saveToCache(
                            cacheKey,
                            result.data,
                            CatalogCacheManager.TTL_CATEGORY_LIST
                        )

                        // Actualizar UI
                        withContext(Dispatchers.Main) {
                            _uiState.value = CategoryUiState.Success(result.data)
                        }

                        Log.d(TAG, "✅ Categorías actualizadas desde RED y guardadas en caché")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Error al actualizar categorías desde red: ${result.message}")
                        if (showErrorIfFails) {
                            withContext(Dispatchers.Main) {
                                _uiState.value = CategoryUiState.Error(result.message)
                            }
                        }
                    }
                    else -> {
                        Log.w(TAG, "Resultado desconocido al actualizar categorías")
                        if (showErrorIfFails) {
                            withContext(Dispatchers.Main) {
                                _uiState.value = CategoryUiState.Error("Error desconocido")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Excepción al actualizar categorías: ${e.message}")
                if (showErrorIfFails) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = CategoryUiState.Error("Sin conexión a internet")
                    }
                }
            }
        }
    }
}