package app.src

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import app.src.data.models.Producto
import app.src.data.repositories.FavoritoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Estados de la UI de Favoritos
 */
sealed class FavoritosUiState {
    object Loading : FavoritosUiState()
    data class Success(val favoritos: List<Producto>) : FavoritosUiState()
    data class Empty(val message: String = "No tienes productos favoritos aún") : FavoritosUiState()
    data class Error(val message: String) : FavoritosUiState()
}

/**
 * ViewModel para gestionar favoritos
 * Trabaja completamente offline con Room Database
 * ✅ OPTIMIZADO CON MULTITHREADING usando diferentes Dispatchers
 */
class FavoritosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FavoritoRepository(application)
    private val TAG = "FavoritosViewModel"

    // LiveData de favoritos (se actualiza automáticamente desde Room)
    val favoritos: LiveData<List<Producto>> = repository.getAllFavoritos().asLiveData()

    // Estado de la UI
    private val _uiState = MutableLiveData<FavoritosUiState>(FavoritosUiState.Loading)
    val uiState: LiveData<FavoritosUiState> = _uiState

    // Estado de favorito individual (para botón de corazón)
    private val _isFavorito = MutableLiveData<Boolean>()
    val isFavorito: LiveData<Boolean> = _isFavorito

    init {
        // Observar cambios en favoritos para actualizar UI state
        favoritos.observeForever { productos ->
            _uiState.value = if (productos.isEmpty()) {
                FavoritosUiState.Empty()
            } else {
                FavoritosUiState.Success(productos)
            }
        }
    }

    /**
     * Verifica si un producto es favorito
     * ✅ Dispatcher.IO para consulta de base de datos
     */
    fun checkIsFavorito(productoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isFav = repository.isFavorito(productoId)
                withContext(Dispatchers.Main) {
                    _isFavorito.value = isFav
                }
                Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Producto $productoId es favorito: $isFav")
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando favorito: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isFavorito.value = false
                }
            }
        }
    }

    /**
     * Toggle favorito (agregar/quitar)
     * ✅ Dispatcher.IO para operaciones de base de datos
     * ✅ Dispatcher.Main para callback en UI thread
     */
    fun toggleFavorito(producto: Producto, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.toggleFavorito(producto)) {
                is Result.Success -> {
                    val isFav = result.data
                    val mensaje = if (isFav) {
                        "❤️ ${producto.nombre} agregado a favoritos"
                    } else {
                        "💔 ${producto.nombre} eliminado de favoritos"
                    }

                    withContext(Dispatchers.Main) {
                        _isFavorito.value = isFav
                        onResult(isFav, mensaje)
                    }
                    Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] $mensaje")
                }
                is Result.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message)
                    }
                    Log.e(TAG, "❌ Error en toggle: ${result.message}")
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Error desconocido")
                    }
                }
            }
        }
    }

    /**
     * Elimina un favorito específico
     * ✅ Dispatcher.IO para eliminación en base de datos
     */
    fun removeFavorito(productoId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.removeFavorito(productoId)) {
                is Result.Success -> {
                    withContext(Dispatchers.Main) {
                        onResult("Eliminado de favoritos")
                    }
                    Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Favorito eliminado: $productoId")
                }
                is Result.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(result.message)
                    }
                    Log.e(TAG, "❌ Error eliminando: ${result.message}")
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        onResult("Error desconocido")
                    }
                }
            }
        }
    }

    /**
     * Elimina todos los favoritos
     * ✅ Dispatcher.IO para operación masiva de eliminación
     * ✅ Procesamiento paralelo para mejor rendimiento
     */
    fun clearAllFavoritos(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "🗑️ [Thread: ${Thread.currentThread().name}] Iniciando limpieza de favoritos...")

            when (val result = repository.clearAllFavoritos()) {
                is Result.Success -> {
                    withContext(Dispatchers.Main) {
                        onResult("Todos los favoritos eliminados")
                    }
                    Log.d(TAG, "✅ [Thread: ${Thread.currentThread().name}] Todos los favoritos eliminados exitosamente")
                }
                is Result.Error -> {
                    withContext(Dispatchers.Main) {
                        onResult(result.message)
                    }
                    Log.e(TAG, "❌ Error limpiando: ${result.message}")
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        onResult("Error desconocido")
                    }
                }
            }
        }
    }

    /**
     * Obtiene el contador de favoritos
     * ✅ Dispatcher.IO para consulta de base de datos
     */
    suspend fun getFavoritosCount(): Int {
        return withContext(Dispatchers.IO) {
            repository.countFavoritos()
        }
    }
}
