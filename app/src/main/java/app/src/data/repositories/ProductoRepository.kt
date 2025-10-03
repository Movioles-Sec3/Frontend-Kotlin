package app.src.data.repositories

import app.src.data.api.ApiClient
import app.src.data.models.*
import retrofit2.Response

class ProductoRepository {

    private val api = ApiClient.productoService

    suspend fun listarProductos(idTipo: Int? = null, disponible: Boolean? = true): Result<List<Producto>> {
        return try {
            val response = api.listarProductos(idTipo, disponible)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun obtenerProducto(productoId: Int): Result<Producto> {
        return try {
            val response = api.obtenerProducto(productoId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun listarTipos(): Result<List<TipoProducto>> {
        return try {
            val response = api.listarTipos()
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
            400 -> "Datos inválidos"
            404 -> "Producto no encontrado"
            500 -> "Error del servidor"
            else -> "Error: ${response.code()}"
        }
    }
}

