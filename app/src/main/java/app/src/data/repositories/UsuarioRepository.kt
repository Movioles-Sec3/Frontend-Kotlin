package app.src.data.repositories

import app.src.data.api.ApiClient
import app.src.data.models.*
import retrofit2.Response

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class UsuarioRepository {

    private val api = ApiClient.usuarioService

    suspend fun registrarUsuario(nombre: String, email: String, password: String): Result<Usuario> {
        return try {
            val response = api.registrarUsuario(UsuarioCreate(nombre, email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                ApiClient.setToken(token.accessToken)
                Result.Success(token)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun obtenerPerfil(): Result<Usuario> {
        return try {
            val token = ApiClient.getToken()
            if (token == null) {
                return Result.Error("No hay sesión activa")
            }

            val response = api.obtenerPerfil("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun recargarSaldo(monto: Double): Result<Usuario> {
        return try {
            val token = ApiClient.getToken()
            if (token == null) {
                return Result.Error("No hay sesión activa")
            }

            val response = api.recargarSaldo("Bearer $token", RecargarSaldoRequest(monto))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(parseError(response), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    fun logout() {
        ApiClient.setToken(null)
    }

    private fun parseError(response: Response<*>): String {
        return when (response.code()) {
            400 -> "Datos inválidos"
            401 -> "Credenciales incorrectas"
            404 -> "Usuario no encontrado"
            422 -> "Datos con formato incorrecto"
            500 -> "Error del servidor"
            else -> "Error: ${response.code()}"
        }
    }
}

