package app.src

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import app.src.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels {
        LoginViewModel.factory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
                    if (state.message != null) {
                        Toast.makeText(this, getString(state.message), Toast.LENGTH_SHORT).show()
                    }
                }
                is LoginUiState.Success -> {
                    // Navegación a Home
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun attemptLogin() {
        binding.tilUsername.error = null
        binding.tilPassword.error = null
        vm.login()
    }

    private fun fieldWatcher(onChange: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChange(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) {}
    }
}
