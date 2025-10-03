package app.src

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import app.src.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val vm: RegisterViewModel by viewModels {
        RegisterViewModel.factory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listeners de campos
        binding.etName.addTextChangedListener(fieldWatcher { vm.onNameChanged(it) })
        binding.etEmail.addTextChangedListener(fieldWatcher { vm.onEmailChanged(it) })
        binding.etPassword.addTextChangedListener(fieldWatcher { vm.onPasswordChanged(it) })
        binding.etPasswordConfirm.addTextChangedListener(fieldWatcher { vm.onPasswordConfirmChanged(it) })

        // Botón de registro
        binding.btnRegister.setOnClickListener {
            clearErrors()
            vm.register()
        }

        // Botón volver al login
        binding.btnBackToLogin.setOnClickListener {
            finish()
        }

        // Observer del estado
        vm.uiState.observe(this) { state ->
            binding.progress.isVisible = state is RegisterUiState.Loading
            binding.btnRegister.isEnabled = state !is RegisterUiState.Loading

            when (state) {
                is RegisterUiState.Idle -> Unit
                is RegisterUiState.Loading -> Unit
                is RegisterUiState.Error -> {
                    // Mostrar errores de formulario
                    binding.tilName.error = state.nameError?.let { getString(it) }
                    binding.tilEmail.error = state.emailError?.let { getString(it) }
                    binding.tilPassword.error = state.passError?.let { getString(it) }
                    binding.tilPasswordConfirm.error = state.passConfirmError?.let { getString(it) }

                    if (state.message != null) {
                        Toast.makeText(this, getString(state.message), Toast.LENGTH_SHORT).show()
                    }
                }
                is RegisterUiState.Success -> {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG).show()

                    // Volver al login después de registro exitoso
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        // Pasar el email para pre-llenarlo en el login
                        putExtra("email", binding.etEmail.text.toString())
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilPasswordConfirm.error = null
    }

    private fun fieldWatcher(onChange: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChange(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) {}
    }
}

