package app.src

import androidx.annotation.StringRes
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Error(
        @StringRes val message: Int? = null,
        @StringRes val userError: Int? = null,
        @StringRes val passError: Int? = null
    ) : LoginUiState()
    object Success : LoginUiState()
}

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    private var user: String = ""
    private var pass: String = ""

    fun onUserChanged(value: String) { user = value }
    fun onPassChanged(value: String) { pass = value }

    fun login() {
        // Validación local
        val uErr = if (user.isBlank()) R.string.err_user_required else null
        val pErr = if (pass.length < 6) R.string.err_pass_min else null
        if (uErr != null || pErr != null) {
            _uiState.value = LoginUiState.Error(userError = uErr, passError = pErr)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.postValue(LoginUiState.Loading)
            // Simula red
            delay(600)
            val ok = repo.login(user, pass)
            if (ok) {
                _uiState.postValue(LoginUiState.Success)
            } else {
                _uiState.postValue(LoginUiState.Error(message = R.string.err_bad_credentials))
            }
        }
    }

    companion object {
        fun factory(repo: AuthRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(repo) as T
            }
        }
    }
}
