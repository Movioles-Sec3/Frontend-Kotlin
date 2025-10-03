package app.src

import androidx.lifecycle.*
import app.src.data.models.TipoProducto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch

sealed class CategoryUiState {
    object Idle : CategoryUiState()
    object Loading : CategoryUiState()
    data class Success(val categories: List<TipoProducto>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

class CategoryViewModel : ViewModel() {

    private val repo = ProductoRepository()

    private val _uiState = MutableLiveData<CategoryUiState>(CategoryUiState.Idle)
    val uiState: LiveData<CategoryUiState> = _uiState

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
                    _uiState.value = CategoryUiState.Error("Unknown error")
                }
            }
        }
    }
}

