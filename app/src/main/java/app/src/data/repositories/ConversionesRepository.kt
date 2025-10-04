package app.src.data.repositories

import app.src.data.api.ApiClient
import app.src.data.models.ProductoConConversiones

class ConversionesRepository {

    private val productoService = ApiClient.productoService

    suspend fun obtenerConversiones(productoId: Int): Result<ProductoConConversiones> {
        return try {
            val response = productoService.obtenerConversionesProducto(productoId)

            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error al obtener conversiones: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Error de conexión: ${e.message}")
        }
    }
}
