package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.Compra
import app.src.data.models.EscanearQRResponse
import app.src.data.models.EstadoCompra
import app.src.data.repositories.CompraRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class OrderPickupState {
    object Idle : OrderPickupState()
    object Loading : OrderPickupState()
    data class Success(val compra: Compra) : OrderPickupState()
    data class QRScanned(val response: EscanearQRResponse) : OrderPickupState()
    data class Error(val message: String) : OrderPickupState()
}

class OrderPickupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CompraRepository()

    private val _state = MutableLiveData<OrderPickupState>(OrderPickupState.Idle)
    val state: LiveData<OrderPickupState> = _state

    private val _currentCompra = MutableLiveData<Compra?>()
    val currentCompra: LiveData<Compra?> = _currentCompra

    fun loadCompra(compra: Compra) {
        _currentCompra.value = compra
        _state.value = OrderPickupState.Success(compra)
    }

    /**
     * ✅ Actualiza el estado de una orden (funciona OFFLINE)
     */
    fun actualizarEstado(compraId: Int, nuevoEstado: EstadoCompra) {
        _state.value = OrderPickupState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val estadoStr = when (nuevoEstado) {
                    EstadoCompra.EN_PREPARACION -> "EN_PREPARACION"
                    EstadoCompra.LISTO -> "LISTO"
                    EstadoCompra.ENTREGADO -> "ENTREGADO"
                    else -> nuevoEstado.name
                }

                // ✅ Usar CompraRepository que maneja offline automáticamente
                val result = repository.actualizarEstado(getApplication(), compraId, estadoStr)

                when (result) {
                    is Result.Success -> {
                        val compraActualizada = result.data
                        _currentCompra.postValue(compraActualizada)
                        _state.postValue(OrderPickupState.Success(compraActualizada))
                    }
                    is Result.Error -> {
                        _state.postValue(OrderPickupState.Error(result.message))
                    }
                    else -> {
                        _state.postValue(OrderPickupState.Error("Error desconocido"))
                    }
                }
            } catch (e: Exception) {
                _state.postValue(OrderPickupState.Error(e.message ?: "Error de conexión"))
            }
        }
    }

    /**
     * Escanea QR (requiere conexión)
     */
    fun escanearQR(codigoQrHash: String) {
        _state.value = OrderPickupState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.escanearQR(codigoQrHash)

                when (result) {
                    is Result.Success -> {
                        val qrResponse = result.data
                        _state.postValue(OrderPickupState.QRScanned(qrResponse))
                    }
                    is Result.Error -> {
                        _state.postValue(OrderPickupState.Error(result.message))
                    }
                    else -> {
                        _state.postValue(OrderPickupState.Error("Error desconocido"))
                    }
                }
            } catch (e: Exception) {
                _state.postValue(OrderPickupState.Error(e.message ?: "Error de conexión"))
            }
        }
    }

    fun canTransitionTo(currentEstado: EstadoCompra, targetEstado: EstadoCompra): Boolean {
        return when (currentEstado) {
            EstadoCompra.PAGADO -> targetEstado == EstadoCompra.EN_PREPARACION
            EstadoCompra.EN_PREPARACION -> targetEstado == EstadoCompra.LISTO
            EstadoCompra.LISTO -> targetEstado == EstadoCompra.ENTREGADO
            else -> false
        }
    }

    fun getNextEstado(currentEstado: EstadoCompra): EstadoCompra? {
        return when (currentEstado) {
            EstadoCompra.PAGADO -> EstadoCompra.EN_PREPARACION
            EstadoCompra.EN_PREPARACION -> EstadoCompra.LISTO
            EstadoCompra.LISTO -> EstadoCompra.ENTREGADO
            else -> null
        }
    }
}
