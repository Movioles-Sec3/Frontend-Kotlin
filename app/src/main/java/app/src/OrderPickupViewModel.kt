package app.src

import androidx.lifecycle.*
import app.src.data.api.ApiClient
import app.src.data.models.ActualizarEstadoRequest
import app.src.data.models.Compra
import app.src.data.models.EscanearQRRequest
import app.src.data.models.EscanearQRResponse
import app.src.data.models.EstadoCompra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class OrderPickupState {
    object Idle : OrderPickupState()
    object Loading : OrderPickupState()
    data class Success(val compra: Compra) : OrderPickupState()
    data class QRScanned(val response: EscanearQRResponse) : OrderPickupState()
    data class Error(val message: String) : OrderPickupState()
}

class OrderPickupViewModel : ViewModel() {

    private val _state = MutableLiveData<OrderPickupState>(OrderPickupState.Idle)
    val state: LiveData<OrderPickupState> = _state

    private val _currentCompra = MutableLiveData<Compra?>()
    val currentCompra: LiveData<Compra?> = _currentCompra

    fun loadCompra(compra: Compra) {
        _currentCompra.value = compra
        _state.value = OrderPickupState.Success(compra)
    }

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

                val request = ActualizarEstadoRequest(estado = estadoStr)
                val response = ApiClient.compraService.actualizarEstadoCompra(compraId, request)

                if (response.isSuccessful && response.body() != null) {
                    val compraActualizada = response.body()!!
                    _currentCompra.postValue(compraActualizada)
                    _state.postValue(OrderPickupState.Success(compraActualizada))
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error al actualizar estado"
                    _state.postValue(OrderPickupState.Error(errorMsg))
                }
            } catch (e: Exception) {
                _state.postValue(OrderPickupState.Error(e.message ?: "Error de conexión"))
            }
        }
    }

    fun escanearQR(codigoQrHash: String) {
        _state.value = OrderPickupState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = EscanearQRRequest(codigoQrHash = codigoQrHash)
                val response = ApiClient.compraService.escanearQR(request)

                if (response.isSuccessful && response.body() != null) {
                    val qrResponse = response.body()!!
                    _state.postValue(OrderPickupState.QRScanned(qrResponse))
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error al escanear QR"
                    _state.postValue(OrderPickupState.Error(errorMsg))
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
