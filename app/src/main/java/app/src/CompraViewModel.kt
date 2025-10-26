package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.Compra
import app.src.data.models.CompraRequest
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
 * Con soporte para caché offline de historial y códigos QR.
 */
class CompraViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository que maneja operaciones de compra con caché LRU */
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
     * Incluye códigos QR que funcionan offline desde el caché.
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

            val compraRequest = CompraRequest(productos)
            when (val result = repo.crearCompra(getApplication(), compraRequest)) {
                is Result.Success -> {
                    _uiState.value = CompraUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = CompraUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = CompraUiState.Error("Error desconocido")
                }
            }
        }
    }

    /**
     * Carga el historial de compras del usuario.
     * Usa caché LRU cuando no hay conexión (incluye códigos QR).
     */
    fun cargarHistorial() {
        viewModelScope.launch {
            when (val result = repo.obtenerHistorial(getApplication())) {
                is Result.Success -> {
                    _historial.value = result.data
                }
                is Result.Error -> {
                    // Si falla, lista vacía (o mantener la existente)
                    _historial.value = emptyList()
                }
                else -> {
                    _historial.value = emptyList()
                }
            }
        }
    }

    /**
     * Actualiza el estado de una compra (requiere internet)
     */
    fun actualizarEstado(compraId: Int, nuevoEstado: String) {
        viewModelScope.launch {
            when (repo.actualizarEstado(getApplication(), compraId, nuevoEstado)) {
                is Result.Success -> {
                    // Recargar historial para reflejar el cambio
                    cargarHistorial()
                }
                is Result.Error -> {
                    // Manejar error (opcional: emitir estado de error)
                }
                else -> {}
            }
        }
    }
}