package app.src

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import app.src.utils.BrightnessManager
import app.src.utils.SessionManager
import app.src.utils.ImagePreloader
import app.src.utils.cache.LruCacheManager
import android.util.Log
import java.util.Calendar

/**
 * Actividad base que aplica automáticamente el tema (claro/oscuro) según la hora del día
 * y el brillo de la pantalla.
 * Todas las actividades deben heredar de esta clase para tener el comportamiento automático.
 *
 * GESTIÓN DE MEMORIA:
 * - Implementa ComponentCallbacks2 para manejar presión de memoria del sistema
 * - Limpia cachés automáticamente cuando el sistema lo requiere
 *
 * Reglas de tema:
 * - Antes de las 20:00 (8 PM) = Modo claro
 * - A partir de las 20:00 (8 PM) = Modo oscuro
 *
 * Reglas de brillo:
 * - Antes de las 16:00 (4 PM) = 90%
 * - Después de las 16:00 (4 PM) = 30%
 * - En pantallas QR = 100%
 */
open class BaseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BaseActivity"
    }

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
     * ═══════════════════════════════════════════════════════════════════════════
     * ESTRATEGIA DE GESTIÓN DE MEMORIA #1: MANEJO DE PRESIÓN DE MEMORIA DEL SISTEMA
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * ComponentCallbacks2.onTrimMemory() es llamado por el sistema Android cuando:
     *
     * 1. TRIM_MEMORY_RUNNING_CRITICAL (nivel 15)
     *    - La app está en foreground y el sistema está EXTREMADAMENTE bajo de memoria
     *    - Riesgo ALTO de que el sistema mate procesos en background
     *    - ACCIÓN: Liberar TODA la memoria caché posible inmediatamente
     *
     * 2. TRIM_MEMORY_RUNNING_LOW (nivel 10)
     *    - La app está en foreground y el sistema está bajo de memoria
     *    - Otros procesos están siendo eliminados
     *    - ACCIÓN: Liberar cachés grandes (imágenes, entradas expiradas)
     *
     * 3. TRIM_MEMORY_RUNNING_MODERATE (nivel 5)
     *    - La app está en foreground y el sistema necesita memoria
     *    - Advertencia temprana
     *    - ACCIÓN: Reducir cachés a la mitad, limpiar entradas antiguas
     *
     * 4. TRIM_MEMORY_BACKGROUND (nivel 40)
     *    - La app pasó a background y el sistema necesita memoria
     *    - ACCIÓN: Liberar cachés no críticos
     *
     * 5. TRIM_MEMORY_UI_HIDDEN (nivel 20)
     *    - La UI ya no es visible para el usuario
     *    - ACCIÓN: Momento ideal para liberar recursos de UI
     *
     * POR QUÉ ES CRÍTICO IMPLEMENTAR ESTO:
     * - Sin onTrimMemory: Android mata la app abruptamente → mala UX
     * - Con onTrimMemory: La app libera memoria proactivamente → sobrevive en background
     * - Reduce crashes por OutOfMemoryError en dispositivos low-end
     * - Mejora retención de usuarios (menos reinicios de app)
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            // ═════════════════════════════════════════════════════════
            // NIVELES CRÍTICOS - La app está en FOREGROUND
            // ═════════════════════════════════════════════════════════

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "🚨 CRITICAL: Sistema sin memoria - Liberando TODO el caché")
                // Limpiar TODOS los cachés inmediatamente
                ImagePreloader.clearCache()
                LruCacheManager.clearAllCaches(this)
                // Sugerir al GC que ejecute (no bloquea, solo hint)
                System.gc()
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.w(TAG, "⚠️ LOW: Sistema bajo de memoria - Liberando cachés grandes")
                // Limpiar caché de imágenes (lo más pesado)
                ImagePreloader.clearCache()
                // Limpiar entradas expiradas de productos/conversiones
                LruCacheManager.cleanExpiredEntries(this)
            }

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.i(TAG, "ℹ️ MODERATE: Reduciendo cachés preventivamente")
                // Reducir caché de imágenes a 50%
                ImagePreloader.trimCache(50)
                // Limpiar solo entradas expiradas
                LruCacheManager.cleanExpiredEntries(this)
            }

            // ═════════════════════════════════════════════════════════
            // NIVELES DE BACKGROUND - La app NO está visible
            // ═════════════════════════════════════════════════════════

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "🔄 BACKGROUND: App en segundo plano - Limpiando cachés")
                // Liberar cachés para mejorar probabilidad de sobrevivir en background
                ImagePreloader.clearCache()
                LruCacheManager.cleanExpiredEntries(this)
            }

            // UI ya no es visible pero la app aún está en memoria
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "👁️ UI HIDDEN: UI no visible - Monitoreando")
                // Momento ideal para liberar recursos de UI si los hubiera
                // Por ahora solo logueamos para debugging
            }
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * ESTRATEGIA DE GESTIÓN DE MEMORIA #2: CALLBACK LEGACY PARA DISPOSITIVOS ANTIGUOS
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * onLowMemory() es el callback legacy (pre-Android 4.0) para baja memoria
     *
     * Diferencia con onTrimMemory:
     * - onTrimMemory: Niveles granulares (0-100), Android 4.0+
     * - onLowMemory: Binario (sí/no), Android 1.0+
     *
     * ¿Por qué implementar ambos?
     * - Compatibilidad con dispositivos MUY antiguos (API < 14)
     * - onLowMemory = equivalente a TRIM_MEMORY_COMPLETE
     * - Garantiza cobertura del 100% de dispositivos Android
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Log.e(TAG, "🆘 LOW MEMORY (legacy): Sistema crítico - Limpiando TODO")
        ImagePreloader.clearCache()
        LruCacheManager.clearAllCaches(this)
        System.gc()
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
