package app.src

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import app.src.databinding.ActivityLoginBinding
import app.src.data.api.ApiClient
import app.src.data.repositories.UsuarioRepository
import app.src.data.repositories.Result
import app.src.utils.SessionManager
import app.src.utils.NetworkUtils
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels {
        LoginViewModel.factory(AuthRepository())
    }
    private val usuarioRepo = UsuarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Verificar si ya hay sesión activa
        if (SessionManager.isLoggedIn(this)) {
            val token = SessionManager.getToken(this)
            ApiClient.setToken(token)
            navigateToHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Accesibilidad básica
        binding.btnLogin.contentDescription = getString(R.string.cd_login_button)

        // Listeners
        binding.etUsername.addTextChangedListener(fieldWatcher { vm.onUserChanged(it) })
        binding.etPassword.addTextChangedListener(fieldWatcher { vm.onPassChanged(it) })

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnLogin.isEnabled) {
                attemptLogin()
                true
            } else false
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }

        // Botón para ir a registro
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Observers
        vm.uiState.observe(this) { state ->
            binding.progress.isVisible = state is LoginUiState.Loading
            binding.btnLogin.isEnabled = state !is LoginUiState.Loading

            when (state) {
                is LoginUiState.Idle -> Unit
                is LoginUiState.Loading -> Unit
                is LoginUiState.Error -> {
                    // Muestra errores de formulario si aplica
                    binding.tilUsername.error = state.userError?.let { getString(it) }
                    binding.tilPassword.error = state.passError?.let { getString(it) }

                    // ✅ MOSTRAR MENSAJE PERSONALIZADO O MENSAJE DE RECURSO
                    val errorMessage = state.customMessage ?: state.message?.let { getString(it) }
                    if (errorMessage != null) {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                is LoginUiState.Success -> {
                    // Guardar token y obtener datos del usuario
                    val token = ApiClient.getToken()
                    if (token != null) {
                        SessionManager.saveToken(this, token)
                        obtenerDatosUsuario()
                    } else {
                        navigateToHome()
                    }
                }
            }
        }
    }

    private fun attemptLogin() {
        // ✅ NO BLOQUEAR EL LOGIN - Dejar que intente conectar y manejar errores después
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        vm.login()
    }

    private fun obtenerDatosUsuario() {
        lifecycleScope.launch {
            when (val result = usuarioRepo.obtenerPerfil()) {
                is Result.Success -> {
                    val usuario = result.data
                    SessionManager.saveUserData(
                        this@LoginActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )

                    // ✅ GUARDAR userId EN DATASTORE PARA ROOM
                    val dataStore = app.src.data.local.DataStoreManager(this@LoginActivity)
                    dataStore.saveUserId(usuario.id)

                    navigateToHome()
                }
                else -> {
                    // Si falla obtener perfil, igual navegamos
                    navigateToHome()
                }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun fieldWatcher(onChange: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChange(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) {}
    }
}
