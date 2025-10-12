package app.src.utils

import android.app.Activity
import android.view.WindowManager
import java.util.Calendar

/**
 * Gestor de brillo automático basado en la hora del día
 *
 * Reglas:
 * - Antes de las 16:00 (4 PM): 90% de brillo
 * - Después de las 16:00 (4 PM): 30% de brillo
 * - En pantallas de QR: 100% de brillo (sin importar la hora)
 */
object BrightnessManager {

    /**
     * Aplica el brillo automático según la hora del día
     * @param activity Actividad donde se aplicará el brillo
     * @param isQrScreen Indica si es una pantalla de QR (para poner 100%)
     */
    fun applyAutomaticBrightness(activity: Activity, isQrScreen: Boolean = false) {
        val brightness = if (isQrScreen) {
            1.0f // 100% para pantallas QR
        } else {
            getBrightnessBasedOnTime()
        }

        setBrightness(activity, brightness)
    }

    /**
     * Obtiene el nivel de brillo basado en la hora actual
     * @return Valor float entre 0.0 y 1.0 (0% a 100%)
     */
    private fun getBrightnessBasedOnTime(): Float {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return if (currentHour < 16) {
            0.6f // 90% antes de las 4 PM
        } else {
            0.3f // 30% después de las 4 PM
        }
    }

    /**
     * Establece el brillo de la pantalla
     * @param activity Actividad donde se aplicará el brillo
     * @param brightness Valor entre 0.0 (0%) y 1.0 (100%)
     */
    private fun setBrightness(activity: Activity, brightness: Float) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0.0f, 1.0f)
        activity.window.attributes = layoutParams
    }

    /**
     * Restaura el brillo a la configuración predeterminada del sistema
     * @param activity Actividad donde se restaurará el brillo
     */
    fun restoreSystemBrightness(activity: Activity) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = layoutParams
    }
}

