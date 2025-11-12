package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.Compra
import app.src.data.models.CompraRequest
import app.src.data.models.DetalleCompraRequest
import app.src.data.repositories.CompraRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val result = withContext(Dispatchers.IO) {
                repo.crearCompra(getApplication(), compraRequest)
            }

            _uiState.value = when (result) {
                is Result.Success -> CompraUiState.Success(result.data)
                is Result.Error -> CompraUiState.Error(result.message)
                else -> CompraUiState.Error("Error desconocido")
            }
        }
    }

    /**
     * Carga el historial de compras del usuario.
     * Usa caché LRU cuando no hay conexión (incluye códigos QR).
     */

    fun cargarHistorial() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.obtenerHistorial(getApplication())
            }

            _historial.value = when (result) {
                is Result.Success -> result.data
                is Result.Error -> emptyList()
                else -> emptyList()
            }
        }
    }

    /**
     * ✅ REQUERIMIENTO 3: Sincroniza órdenes pendientes del outbox
     * Se ejecuta automáticamente al cargar el historial si hay conexión
     */
    suspend fun sincronizarOrdenesOffline(): Int {
        return withContext(Dispatchers.IO) {
            repo.sincronizarOrdenesOffline(getApplication())
        }
    }

    /**
     * ✅ Obtiene el número de órdenes pendientes de sincronizar
     */
    suspend fun getOrdenesOfflinePendientes(): Int {
        return withContext(Dispatchers.IO) {
            repo.getOrdenesOfflinePendientes(getApplication())
        }
    }

    /**
     * Actualiza el estado de una compra (requiere internet)
     */
    fun actualizarEstado(compraId: Int, nuevoEstado: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repo.actualizarEstado(getApplication(), compraId, nuevoEstado)
            }

            if (result is Result.Success) {
                cargarHistorial() // already launches coroutine internally
            }
        }
    }
}