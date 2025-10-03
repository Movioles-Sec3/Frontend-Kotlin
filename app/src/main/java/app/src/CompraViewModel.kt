package app.src

import androidx.lifecycle.*
import app.src.data.models.Compra
import app.src.data.models.DetalleCompraRequest
import app.src.data.repositories.CompraRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch

sealed class CompraUiState {
    object Idle : CompraUiState()
    object Loading : CompraUiState()
    data class Success(val compra: Compra) : CompraUiState()
    data class Error(val message: String) : CompraUiState()
}

class CompraViewModel : ViewModel() {

    private val repo = CompraRepository()

    private val _uiState = MutableLiveData<CompraUiState>(CompraUiState.Idle)
    val uiState: LiveData<CompraUiState> = _uiState

    private val _historial = MutableLiveData<List<Compra>>()
    val historial: LiveData<List<Compra>> = _historial

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
                    _uiState.value = CompraUiState.Error("Error desconocido")
                }
            }
        }
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            when (val result = repo.historialCompras()) {
                is Result.Success -> {
                    _historial.value = result.data
                }
                is Result.Error -> {
                    // Manejo de error
                }
                else -> {}
            }
        }
    }
}

