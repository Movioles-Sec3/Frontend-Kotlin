package app.src

import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import app.src.data.api.ApiClient
import app.src.data.repositories.Result
import app.src.data.repositories.UsuarioRepository
import app.src.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private val usuarioRepo = UsuarioRepository()

    // Sensor variables
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_MANUAL_OVERRIDE = "manual_override"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before calling super.onCreate()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedThemeMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Initialize sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Load session token
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Display user information
        val userName = SessionManager.getUserName(this)
        val userSaldo = SessionManager.getUserSaldo(this)

        findViewById<TextView>(R.id.tv_welcome)?.text = "Hello, $userName"
        findViewById<TextView>(R.id.tv_saldo)?.text = String.format(Locale.US, "Balance: $%.2f", userSaldo)

        // Theme mode buttons
        findViewById<Button>(R.id.btn_theme_light)?.setOnClickListener {
            saveThemePreference(AppCompatDelegate.MODE_NIGHT_NO, true)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this, "Modo Día activado", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_theme_dark)?.setOnClickListener {
            saveThemePreference(AppCompatDelegate.MODE_NIGHT_YES, true)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Toast.makeText(this, "Modo Noche activado", Toast.LENGTH_SHORT).show()
        }

        // Recharge balance button
        findViewById<Button>(R.id.btn_recharge)?.setOnClickListener {
            showRechargeDialog()
        }

        // Navigation to each view
        findViewById<Button>(R.id.btn_categories).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_products).setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }
        
        findViewById<Button>(R.id.btn_order_summary).setOnClickListener {
            startActivity(Intent(this, OrderSummaryActivity::class.java))
        }
        
        // Order Pickup button - can be used to view order history
        findViewById<Button>(R.id.btn_order_pickup)?.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        // Logout button
        findViewById<Button>(R.id.btn_logout)?.setOnClickListener {
            logout()
        }
    }

    private fun saveThemePreference(themeMode: Int, manualOverride: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_THEME_MODE, themeMode)
            putBoolean(KEY_MANUAL_OVERRIDE, manualOverride)
            apply()
        }
    }

    // Sensor event listener
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null && event.sensor.type == Sensor.TYPE_LIGHT) {
                // Get light sensor value in lux
                val lux = event.values[0]

                // Check if theme was manually overridden
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val manualOverride = prefs.getBoolean(KEY_MANUAL_OVERRIDE, false)

                if (!manualOverride) {
                    // Determine theme based on light level
                    val newThemeMode = if (lux > 50) {
                        AppCompatDelegate.MODE_NIGHT_NO
                    } else {
                        AppCompatDelegate.MODE_NIGHT_YES
                    }

                    val currentMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

                    // Only change theme if it's different from current
                    if (newThemeMode != currentMode) {
                        saveThemePreference(newThemeMode, false)
                        AppCompatDelegate.setDefaultNightMode(newThemeMode)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not used
        }
    }

    private fun showRechargeDialog() {
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
                    // Update local session
                    SessionManager.saveUserData(
                        this@HomeActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )
                    // Update UI
                    findViewById<TextView>(R.id.tv_saldo)?.text =
                        String.format(Locale.US, "Balance: $%.2f", usuario.saldo)

                    Toast.makeText(
                        this@HomeActivity,
                        String.format(Locale.US, "Balance recharged successfully! New balance: $%.2f", usuario.saldo),
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

    override fun onResume() {
        super.onResume()
        // Register light sensor listener
        lightSensor?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listener to save battery
        sensorManager.unregisterListener(sensorEventListener)
    }
}
