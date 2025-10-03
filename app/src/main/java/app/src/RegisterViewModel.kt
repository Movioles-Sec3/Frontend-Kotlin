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

            when (val result = repo.registrar(name, email, password)) {
                is Result.Success -> {
                    _uiState.postValue(RegisterUiState.Success)
                }
                is Result.Error -> {
                    _uiState.postValue(RegisterUiState.Error(message = R.string.register_error))
                }
                else -> {
                    _uiState.postValue(RegisterUiState.Error(message = R.string.register_error))
                }
            }
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

