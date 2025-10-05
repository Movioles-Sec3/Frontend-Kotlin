package app.src

import androidx.lifecycle.*
import app.src.data.models.Producto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch

/**
 * Represents the UI contract for the Home screen.
 *
 * States:
 * - [Loading]: Data is being fetched and the UI should show a progress indicator.
 * - [Success]: Data fetched successfully; contains the list of recommended products.
 * - [Error]: A user-facing error occurred; contains a message suitable for display.
 */
sealed class HomeUiState {
    /** Emitted while recommended products are loading. */
    object Loading : HomeUiState()

    /**
     * Emitted when recommended products have been loaded successfully.
     *
     * @property productosRecomendados The list of products to render in the UI.
     */
    data class Success(val productosRecomendados: List<Producto>) : HomeUiState()

    /**
     * Emitted when there is an error loading data.
     *
     * @property message Human-readable message that can be shown to the user.
     */
    data class Error(val message: String) : HomeUiState()
}

/**
 * ViewModel for the Home screen.
 *
 * Responsibilities:
 * - Loads and exposes recommended products via [LiveData].
 * - Exposes a high-level UI state ([HomeUiState]) to simplify rendering logic.
 * - Handles user interactions originating from recommended products (e.g., item clicks).
 *
 * Lifecycle:
 * - Triggers an initial load of recommended products in [init].
 */
class HomeViewModel : ViewModel() {

    /** Repository used to retrieve product data. */
    private val productoRepository = ProductoRepository()

    /**
     * Backing field for the UI state.
     * Use [uiState] to observe state changes from the UI layer.
     */
    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    /**
     * Backing field for the list of recommended products.
     * Use [productosRecomendados] to observe data changes from the UI layer.
     */
    private val _productosRecomendados = MutableLiveData<List<Producto>>()
    val productosRecomendados: LiveData<List<Producto>> = _productosRecomendados

    /**
     * Initializes the ViewModel by starting the initial data load.
     */
    init {
        cargarProductosRecomendados()
    }

    /**
     * Loads recommended products and updates both [productosRecomendados] and [uiState].
     *
     * Flow:
     * 1) Emits [HomeUiState.Loading].
     * 2) Requests data from [ProductoRepository].
     * 3) On success:
     *    - Updates [_productosRecomendados] with the fetched list.
     *    - Emits [HomeUiState.Success] with the same list.
     * 4) On failure:
     *    - Emits [HomeUiState.Error] with a user-facing message.
     * 5) Any unexpected result falls back to a generic error message.
     */
    fun cargarProductosRecomendados() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            when (val result = productoRepository.obtenerProductosRecomendados()) {
                is Result.Success -> {
                    _productosRecomendados.value = result.data
                    _uiState.value = HomeUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = HomeUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = HomeUiState.Error("Error desconocido")
                }
            }
        }
    }

    /**
     * Handles clicks on a recommended product.
     *
     * Extend this to:
     * - Send analytics events.
     * - Navigate to a product detail screen.
     * - Preload images or additional data for a smoother transition.
     *
     * @param producto The product that was clicked.
     */
    fun onProductoRecomendadoClick(producto: Producto) {
        // Aquí podrías agregar lógica adicional si es necesario
        // Por ejemplo, analytics, tracking, etc.
    }
}