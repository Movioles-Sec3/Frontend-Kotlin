package app.src

import androidx.lifecycle.*
import app.src.data.models.TipoProducto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch

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
class CategoryViewModel : ViewModel() {

    /** Repository that provides product-related data operations. */
    private val repo = ProductoRepository()

    // Backing property that holds the current UI state.
    private val _uiState = MutableLiveData<CategoryUiState>(CategoryUiState.Idle)

    /**
     * Public, immutable LiveData that the view observes to render the screen.
     */
    val uiState: LiveData<CategoryUiState> = _uiState

    /**
     * Initiates the loading of product categories.
     *
     * Behavior:
     * 1) Emits [CategoryUiState.Loading].
     * 2) Requests the category list from the repository.
     * 3) Emits:
     *    - [CategoryUiState.Success] with the data on success.
     *    - [CategoryUiState.Error] with a message on failure or unexpected result.
     */
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading
            when (val result = repo.listarTipos()) {
                is Result.Success -> {
                    _uiState.value = CategoryUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = CategoryUiState.Error(result.message)
                }
                else -> {
                    // Fallback guard for any unhandled Result variants.
                    _uiState.value = CategoryUiState.Error("Unknown error")
                }
            }
        }
    }
}