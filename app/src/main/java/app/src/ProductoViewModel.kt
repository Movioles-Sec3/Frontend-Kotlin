package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.Producto
import app.src.data.models.TipoProducto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch

sealed class ProductoUiState {
    object Idle : ProductoUiState()
    object Loading : ProductoUiState()
    data class Success(val productos: List<Producto>) : ProductoUiState()
    data class Error(val message: String) : ProductoUiState()
}

class ProductoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProductoRepository()

    private val _uiState = MutableLiveData<ProductoUiState>(ProductoUiState.Idle)
    val uiState: LiveData<ProductoUiState> = _uiState

    private val _categorias = MutableLiveData<List<TipoProducto>>()
    val categorias: LiveData<List<TipoProducto>> = _categorias

    fun cargarProductos(idTipo: Int? = null) {
        viewModelScope.launch {
            _uiState.value = ProductoUiState.Loading
            when (val result = repo.listarProductos(getApplication(), idTipo, true)) {
                is Result.Success -> {
                    _uiState.value = ProductoUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = ProductoUiState.Error(result.message)
                }
                else -> {
                    _uiState.value = ProductoUiState.Error("Error desconocido")
                }
            }
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            when (val result = repo.listarTipos(getApplication())) {
                is Result.Success -> {
                    _categorias.value = result.data
                }
                is Result.Error -> {
                    // Manejo silencioso o mostrar error
                }
                else -> {}
            }
        }
    }
}
