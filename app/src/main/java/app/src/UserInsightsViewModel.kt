package app.src

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.src.data.models.UserInsights
import app.src.data.repositories.InsightsRepository
import app.src.data.repositories.Result
import app.src.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * ViewModel para User Insights Dashboard
 *
 * Maneja la lógica de negocio para análisis de patrones de compra
 * usando Kotlin Coroutines y StateFlow
 */
class UserInsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InsightsRepository(application)

    private val _insightsState = MutableLiveData<InsightsState>()
    val insightsState: LiveData<InsightsState> = _insightsState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    sealed class InsightsState {
        object Loading : InsightsState()
        data class Success(val insights: UserInsights) : InsightsState()
        data class Error(val message: String) : InsightsState()
        object NoData : InsightsState()
    }

    /**
     * Cargar insights del usuario usando coroutines
     * Se ejecuta en background con Dispatchers.IO en el repository
     */
    fun loadUserInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            _insightsState.value = InsightsState.Loading

            val userId = SessionManager.getUserId(getApplication())
            if (userId == -1) {
                _insightsState.value = InsightsState.Error("Usuario no autenticado")
                _isLoading.value = false
                return@launch
            }

            when (val result = repository.getUserInsights(userId)) {
                is Result.Success -> {
                    val insights = result.data
                    if (insights.totalOrders == 0) {
                        _insightsState.value = InsightsState.NoData
                    } else {
                        _insightsState.value = InsightsState.Success(insights)
                    }
                }
                is Result.Error -> {
                    _insightsState.value = InsightsState.Error(
                        result.message ?: "Error al cargar estadísticas"
                    )
                }
                is Result.Loading -> {
                    // Ya manejado arriba
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Refrescar insights (útil para pull-to-refresh)
     */
    fun refreshInsights() {
        loadUserInsights()
    }
}
