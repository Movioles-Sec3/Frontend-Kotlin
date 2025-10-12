package app.src

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import app.src.utils.SessionManager
import java.util.Calendar

/**
 * Actividad base que aplica automáticamente el tema (claro/oscuro) según la hora del día.
 * Todas las actividades deben heredar de esta clase para tener el comportamiento automático.
 *
 * Regla:
 * - Antes de las 20:00 (8 PM) = Modo claro
 * - A partir de las 20:00 (8 PM) = Modo oscuro
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar tema según la hora ANTES de llamar a super.onCreate
        applyThemeByHour()
        super.onCreate(savedInstanceState)
    }

    /**
     * Aplica el tema según la hora actual del día
     */
    private fun applyThemeByHour() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNightMode = currentHour >= 20 // 8 PM o después

        // Guardar preferencia
        SessionManager.saveNightMode(this, isNightMode)

        // Aplicar tema
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}

