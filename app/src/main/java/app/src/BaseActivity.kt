package app.src

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import app.src.utils.BrightnessManager
import app.src.utils.SessionManager
import app.src.utils.MemoryManager
import java.util.Calendar

/**
 * Actividad base que aplica automáticamente el tema (claro/oscuro) según la hora del día
 * y el brillo de la pantalla.
 * Todas las actividades deben heredar de esta clase para tener el comportamiento automático.
 *
 * Reglas de tema:
 * - Antes de las 20:00 (8 PM) = Modo claro
 * - A partir de las 20:00 (8 PM) = Modo oscuro
 *
 * Reglas de brillo:
 * - Antes de las 16:00 (4 PM) = 90%
 * - Después de las 16:00 (4 PM) = 30%
 * - En pantallas QR = 100%
 *
 * Gestión de Memoria:
 * - Responde a eventos de presión de memoria del sistema
 * - Libera cachés automáticamente cuando es necesario
 */
open class BaseActivity : AppCompatActivity() {

    /**
     * Indica si esta actividad es una pantalla de QR
     * Las subclases que muestren QR deben sobrescribir esto y devolver true
     */
    protected open val isQrScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar tema según la hora ANTES de llamar a super.onCreate
        applyThemeByHour()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // Aplicar brillo automático cada vez que la actividad se muestra
        BrightnessManager.applyAutomaticBrightness(this, isQrScreen)
    }

    override fun onPause() {
        super.onPause()
        // Opcional: restaurar brillo del sistema cuando se sale de la actividad
        // Comentado para mantener consistencia entre actividades
        // BrightnessManager.restoreSystemBrightness(this)
    }

    /**
     * Maneja eventos de presión de memoria del sistema
     * Implementa ComponentCallbacks2 para liberar recursos cuando sea necesario
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryManager.handleMemoryPressure(this, level)
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
