package app.src

import androidx.annotation.StringRes
import androidx.lifecycle.*
import app.src.data.repositories.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Error(
        @StringRes val message: Int? = null,
        @StringRes val nameError: Int? = null,
        @StringRes val emailError: Int? = null,
        @StringRes val passError: Int? = null,
        @StringRes val passConfirmError: Int? = null
    ) : RegisterUiState()
    object Success : RegisterUiState()
}

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<RegisterUiState>(RegisterUiState.Idle)
    val uiState: LiveData<RegisterUiState> = _uiState

    private var name: String = ""
    private var email: String = ""
    private var password: String = ""
    private var passwordConfirm: String = ""

    fun onNameChanged(value: String) { name = value }
    fun onEmailChanged(value: String) { email = value }
    fun onPasswordChanged(value: String) { password = value }
    fun onPasswordConfirmChanged(value: String) { passwordConfirm = value }

    fun register() {
        // Validación local
        val nameErr = if (name.isBlank()) R.string.err_name_required else null
        val emailErr = when {
            email.isBlank() -> R.string.err_email_required
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.err_email_invalid
            else -> null
        }
        val passErr = when {
            password.isBlank() -> R.string.err_password_required
            password.length < 6 -> R.string.err_pass_min
            else -> null
        }
        val passConfirmErr = if (password != passwordConfirm) R.string.err_password_mismatch else null

        if (nameErr != null || emailErr != null || passErr != null || passConfirmErr != null) {
            _uiState.value = RegisterUiState.Error(
                nameError = nameErr,
                emailError = emailErr,
                passError = passErr,
                passConfirmError = passConfirmErr
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.postValue(RegisterUiState.Loading)

            android.util.Log.d("RegisterViewModel", "=== INICIO REGISTRO ===")
            android.util.Log.d("RegisterViewModel", "Nombre: $name")
            android.util.Log.d("RegisterViewModel", "Email: $email")
            android.util.Log.d("RegisterViewModel", "Password length: ${password.length}")

            // Verificar conectividad básica
            try {
                val url = java.net.URL("http://192.168.4.202:8080/")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.requestMethod = "GET"
                val canConnect = try {
                    connection.responseCode
                    true
                } catch (e: Exception) {
                    android.util.Log.e("RegisterViewModel", "No se puede conectar al backend: ${e.message}")
                    false
                } finally {
                    connection.disconnect()
                }

                if (!canConnect) {
                    android.util.Log.e("RegisterViewModel", "⚠️ BACKEND NO ACCESIBLE")
                } else {
                    android.util.Log.d("RegisterViewModel", "✓ Backend accesible")
                }
            } catch (e: Exception) {
                android.util.Log.e("RegisterViewModel", "Error verificando conectividad: ${e.message}", e)
            }

            android.util.Log.d("RegisterViewModel", "Iniciando petición de registro...")
            when (val result = repo.registrar(name, email, password)) {
                is Result.Success -> {
                    android.util.Log.d("RegisterViewModel", "✓ Registro exitoso: ${result.data}")
                    _uiState.postValue(RegisterUiState.Success)
                }
                is Result.Error -> {
                    android.util.Log.e("RegisterViewModel", "✗ Error en registro: ${result.message}, code: ${result.code}")
                    _uiState.postValue(RegisterUiState.Error(message = R.string.register_error))
                }
                else -> {
                    android.util.Log.e("RegisterViewModel", "✗ Resultado inesperado: $result")
                    _uiState.postValue(RegisterUiState.Error(message = R.string.register_error))
                }
            }
            android.util.Log.d("RegisterViewModel", "=== FIN REGISTRO ===")
        }
    }

    companion object {
        fun factory(repo: AuthRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RegisterViewModel(repo) as T
            }
        }
    }
}

