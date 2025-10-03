package app.src

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.src.data.api.ApiClient
import app.src.data.repositories.Result
import app.src.data.repositories.UsuarioRepository
import app.src.utils.SessionManager
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private val usuarioRepo = UsuarioRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Mostrar información del usuario
        val userName = SessionManager.getUserName(this)
        val userSaldo = SessionManager.getUserSaldo(this)

        // Buscar TextView en el layout (si existe)
        findViewById<TextView>(R.id.tv_welcome)?.text = "Hello, $userName"
        findViewById<TextView>(R.id.tv_saldo)?.text = "Balance: $${String.format("%.2f", userSaldo)}"

        // Botón de recarga de saldo
        findViewById<Button>(R.id.btn_recharge)?.setOnClickListener {
            showRechargeDialog()
        }

        // Configurar navegación a cada vista
        findViewById<Button>(R.id.btn_categories).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_products).setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_order_summary).setOnClickListener {
            startActivity(Intent(this, OrderSummaryActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_order_pickup).setOnClickListener {
            startActivity(Intent(this, OrderPickupActivity::class.java))
        }

        // Botón de logout (si existe en el layout)
        findViewById<Button>(R.id.btn_logout)?.setOnClickListener {
            logout()
        }
    }

    private fun showRechargeDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val input = EditText(this).apply {
            hint = "Enter amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(this)
            .setTitle("Recharge Balance")
            .setMessage("Enter the amount you want to add to your balance:")
            .setView(input)
            .setPositiveButton("Recharge") { _, _ ->
                val amountStr = input.text.toString()
                if (amountStr.isNotEmpty()) {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        rechargeBalance(amount)
                    } else {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rechargeBalance(amount: Double) {
        lifecycleScope.launch {
            when (val result = usuarioRepo.recargarSaldo(amount)) {
                is Result.Success -> {
                    val usuario = result.data
                    // Actualizar sesión local
                    SessionManager.saveUserData(
                        this@HomeActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )
                    // Actualizar UI
                    findViewById<TextView>(R.id.tv_saldo)?.text =
                        "Balance: $${String.format("%.2f", usuario.saldo)}"

                    Toast.makeText(
                        this@HomeActivity,
                        "Balance recharged successfully! New balance: $${String.format("%.2f", usuario.saldo)}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is Result.Error -> {
                    Toast.makeText(
                        this@HomeActivity,
                        "Error recharging balance: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(
                        this@HomeActivity,
                        "Unknown error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun logout() {
        SessionManager.clearSession(this)
        ApiClient.setToken(null)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
