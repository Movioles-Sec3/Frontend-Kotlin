package app.src

import app.src.data.repositories.UsuarioRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Repository that centralizes user authentication flows.
 *
 * It delegates data operations to [UsuarioRepository] and normalizes the return values
 * so upper layers (e.g., ViewModels) can consume a consistent [Result] type.
 */
class AuthRepository {
    // Low-level data source for user-related operations.
    private val usuarioRepo = UsuarioRepository()

    /**
     * Attempts to authenticate a user with the provided credentials.
     *
     * On success, it maps the underlying response to a [Result.Success] containing only
     * the access token (String) to limit exposure of transport-layer details.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @return [Result.Success] with the access token if login succeeds,
     *         [Result.Error] with a human-readable message otherwise.
     */
    suspend fun login(username: String, password: String): Result<String> {
        val result = usuarioRepo.login(username, password)
        return when (result) {
            is Result.Success -> {
                // Narrow the surface: expose only the access token to callers.
                Result.Success(result.data.accessToken)
            }
            is Result.Error -> result
            else -> Result.Error("Error inesperado")
        }
    }

    /**
     * Registers a new user account.
     *
     * This delegates to the underlying user repository. The concrete [Result] payload type
     * depends on the repository’s contract.
     *
     * @param nombre Display name or full name.
     * @param email User's email address.
     * @param password Account password.
     */
    suspend fun registrar(nombre: String, email: String, password: String) =
        usuarioRepo.registrarUsuario(nombre, email, password)

    /**
     * Logs out the current user and clears any persisted session data if applicable.
     */
    fun logout() {
        // fire-and-forget, does not block main thread
        GlobalScope.launch(Dispatchers.IO) {
            usuarioRepo.logout()
        }
    }


}