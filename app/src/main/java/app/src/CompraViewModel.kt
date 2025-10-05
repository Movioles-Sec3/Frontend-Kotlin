package app.src

import androidx.lifecycle.*
import app.src.data.models.Compra
import app.src.data.models.DetalleCompraRequest
import app.src.data.repositories.CompraRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch

/**
 * UI state contract for the Purchase (Compra) screen.
 *
 * Represents mutually exclusive states the view can observe to render the screen.
 */
sealed class CompraUiState {
    /** No active operation. Useful as the initial state. */
    object Idle : CompraUiState()

    /** A purchase-related operation is in progress. */
    object Loading : CompraUiState()

    /**
     * Operation completed successfully.
     *
     * @param compra The resulting purchase entity.
     */
    data class Success(val compra: Compra) : CompraUiState()

    /**
     * An error occurred while performing the operation.
     *
     * @param message Human-readable error message suitable for UI display.
     */
    data class Error(val message: String) : CompraUiState()
}

/**
 * ViewModel responsible for creating purchases and loading purchase history.
 *
 * Responsibilities:
 * - Coordinates data operations with [CompraRepository].
 * - Exposes a unidirectional UI state via [uiState] for create operations.
 * - Exposes a separate observable stream for purchase history via [historial].
 * - Encapsulates mutable state and exposes immutable [LiveData] to the view layer.
 */
class CompraViewModel : ViewModel() {

    /** Repository that provides purchase-related data operations. */
    private val repo = CompraRepository()

    // Backing property for UI state of "create purchase" flow.
    private val _uiState = MutableLiveData<CompraUiState>(CompraUiState.Idle)

    /**
     * Public, immutable LiveData the view observes to render create-purchase states.
     */
    val uiState: LiveData<CompraUiState> = _uiState

    // Backing property for the user's purchase history.
    private val _historial = MutableLiveData<List<Compra>>()

    /**
     * Public, immutable LiveData containing the user's purchase history.
     */
    val historial: LiveData<List<Compra>> = _historial

    /**
     * Creates a purchase from the provided list of product details.
     *
     * Flow:
     * 1) Emits [CompraUiState.Loading].
     * 2) Requests creation via repository.
     * 3) Emits:
     *    - [CompraUiState.Success] with the resulting [Compra] on success.
     *    - [CompraUiState.Error] with a message on failure or unexpected result.
     *
     * Note: This method updates only the create-purchase UI state; it does not
     * automatically refresh [historial]. Call [cargarHistorial] if you need the
     * history list reflected after a successful creation.
     *
     * @param productos Line items to include in the purchase request.
     */
    fun crearCompra(productos: List<DetalleCompraRequest>) {
        viewModelScope.launch {
            _uiState.value = CompraUiState.Loading
            when (val result = repo.crearCompra(productos)) {
                is Result.Success -> {
                    _uiState.value = CompraUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = CompraUiState.Error(result.message)
                }
                else -> {
                    // Fallback guard for any unhandled Result variants.
                    _uiState.value = CompraUiState.Error("Error desconocido")
                }
            }
        }
    }

    /**
     * Loads the user's purchase history and posts it to [historial].
     *
     * This method is independent from the create-purchase UI state ([uiState]).
     * Any errors are currently ignored in UI state, but can be surfaced by
     * extending this method to emit a separate error signal if needed.
     */
    fun cargarHistorial() {
        viewModelScope.launch {
            when (val result = repo.historialCompras()) {
                is Result.Success -> {
                    _historial.value = result.data
                }
                is Result.Error -> {
                    // Optional: surface an error via a dedicated LiveData or event channel.
                }
                else -> {
                    // No-op for unhandled variants; consider logging if necessary.
                }
            }
        }
    }
}