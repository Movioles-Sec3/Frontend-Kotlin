package app.src

import app.src.data.repositories.UsuarioRepository
import app.src.data.repositories.Result

class AuthRepository {
    private val usuarioRepo = UsuarioRepository()

    suspend fun login(username: String, password: String): Result<String> {
        val result = usuarioRepo.login(username, password)
        return when (result) {
            is Result.Success -> Result.Success(result.data.accessToken)
            is Result.Error -> result
            else -> Result.Error("Error inesperado")
        }
    }

    suspend fun registrar(nombre: String, email: String, password: String) =
        usuarioRepo.registrarUsuario(nombre, email, password)

    fun logout() {
        usuarioRepo.logout()
    }
}
