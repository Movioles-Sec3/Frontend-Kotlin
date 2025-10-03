package app.src.data.repositories

import app.src.data.api.ApiClient
import app.src.data.models.*
import retrofit2.Response

class CompraRepository {

    private val api = ApiClient.compraService

    suspend fun crearCompra(productos: List<DetalleCompraRequest>): Result<Compra> {
        return try {
            val token = ApiClient.getToken()
            if (token == null) {
                return Result.Error("No hay sesión activa")
            }

            val response = api.crearCompra("Bearer $token", CompraRequest(productos))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun historialCompras(): Result<List<Compra>> {
        return try {
            val token = ApiClient.getToken()
            if (token == null) {
                return Result.Error("No hay sesión activa")
            }

            val response = api.historialCompras("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun comprasPendientes(): Result<List<Compra>> {
        return try {
            val response = api.comprasPendientes()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun actualizarEstadoCompra(compraId: Int, estado: String): Result<Compra> {
        return try {
            val response = api.actualizarEstadoCompra(compraId, ActualizarEstadoRequest(estado))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun escanearQR(codigoQrHash: String): Result<EscanearQRResponse> {
        return try {
            val response = api.escanearQR(EscanearQRRequest(codigoQrHash))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    private fun parseError(response: Response<*>): String {
        return when (response.code()) {
            400 -> "Solicitud inválida"
            401 -> "No autorizado"
            404 -> "Compra no encontrada"
            500 -> "Error del servidor"
            else -> "Error: ${response.code()}"
        }
    }
}

