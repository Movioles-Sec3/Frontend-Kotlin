package app.src

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.src.data.local.entities.CalificacionEntity
import app.src.data.repositories.CalificacionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestión de calificaciones
 * Usa múltiples dispatchers para optimizar operaciones
 */
class CalificacionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalificacionRepository(application)

    private val _uiState = MutableLiveData<CalificacionUiState>(CalificacionUiState.Loading)
    val uiState: LiveData<CalificacionUiState> = _uiState

    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    companion object {
        private const val TAG = "CalificacionViewModel"
    }

    /**
     * Cargar calificación existente para una orden
     * Usa Dispatchers.IO para BD
     */
    fun loadCalificacion(orderId: Int) {
        viewModelScope.launch {
            _uiState.value = CalificacionUiState.Loading
            Log.d(TAG, "🔍 Cargando calificación para Order #$orderId")

            try {
                // Buscar en BD (Dispatchers.IO)
                val calificacion = withContext(Dispatchers.IO) {
                    repository.getCalificacion(orderId)
                }

                // También verificar cache (Dispatchers.Default)
                val cachedRating = withContext(Dispatchers.Default) {
                    repository.getRatingFromCache(orderId)
                }

                if (calificacion != null) {
                    _uiState.value = CalificacionUiState.Loaded(calificacion)
                    Log.d(TAG, "✅ Calificación cargada: ${calificacion.calificacion}/10")
                } else if (cachedRating != null) {
                    // Si está solo en cache, crear entity temporal
                    val tempCalificacion = CalificacionEntity(
                        orderId = orderId,
                        calificacion = cachedRating,
                        comentario = "",
                        fechaCalificacion = System.currentTimeMillis()
                    )
                    _uiState.value = CalificacionUiState.Loaded(tempCalificacion)
                    Log.d(TAG, "✅ Calificación cargada desde cache: $cachedRating/10")
                } else {
                    _uiState.value = CalificacionUiState.Empty
                    Log.d(TAG, "❌ No hay calificación para esta orden")
                }
            } catch (e: Exception) {
                _uiState.value = CalificacionUiState.Error(e.message ?: "Error desconocido")
                Log.e(TAG, "❌ Error cargando calificación: ${e.message}", e)
            }
        }
    }

    /**
     * Guardar nueva calificación
     * Usa múltiples dispatchers para operaciones paralelas
     */
    fun saveCalificacion(orderId: Int, rating: Int, comentario: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            Log.d(TAG, "💾 Guardando calificación: Order #$orderId, Rating: $rating/10")

            try {
                // Validación en Dispatchers.Default (CPU)
                withContext(Dispatchers.Default) {
                    require(rating in 1..10) { "La calificación debe estar entre 1 y 10" }
                    require(comentario.isNotBlank()) { "El comentario no puede estar vacío" }
                    Log.d(TAG, "✅ Validación pasada en Dispatchers.Default")
                }

                // Guardar en BD y cache (Dispatchers.IO + Default)
                withContext(Dispatchers.IO) {
                    repository.saveCalificacion(orderId, rating, comentario)
                }

                _saveState.value = SaveState.Success
                Log.d(TAG, "🎉 Calificación guardada exitosamente")

                // Recargar para mostrar actualización
                loadCalificacion(orderId)
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Error al guardar")
                Log.e(TAG, "❌ Error guardando calificación: ${e.message}", e)
            }
        }
    }

    /**
     * Actualizar calificación existente
     */
    fun updateCalificacion(orderId: Int, rating: Int, comentario: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            Log.d(TAG, "🔄 Actualizando calificación: Order #$orderId")

            try {
                withContext(Dispatchers.Default) {
                    require(rating in 1..10) { "La calificación debe estar entre 1 y 10" }
                    require(comentario.isNotBlank()) { "El comentario no puede estar vacío" }
                }

                withContext(Dispatchers.IO) {
                    repository.updateCalificacion(orderId, rating, comentario)
                }

                _saveState.value = SaveState.Success
                Log.d(TAG, "✅ Calificación actualizada exitosamente")

                loadCalificacion(orderId)
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Error al actualizar")
                Log.e(TAG, "❌ Error actualizando calificación: ${e.message}", e)
            }
        }
    }

    /**
     * Verificar si existe calificación (para mostrar/ocultar botón)
     */
    fun checkIfCalificacionExists(orderId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = withContext(Dispatchers.IO) {
                repository.hasCalificacion(orderId)
            }
            withContext(Dispatchers.Main) {
                callback(exists)
            }
        }
    }

    /**
     * Obtener estadísticas de cache
     */
    fun logCacheStats() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                repository.logCacheStats()
            }
        }
    }
}

/**
 * Estados de UI para la pantalla de calificación
 */
sealed class CalificacionUiState {
    object Loading : CalificacionUiState()
    object Empty : CalificacionUiState()
    data class Loaded(val calificacion: CalificacionEntity) : CalificacionUiState()
    data class Error(val message: String) : CalificacionUiState()
}

/**
 * Estados del proceso de guardado
 */
sealed class SaveState {
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

