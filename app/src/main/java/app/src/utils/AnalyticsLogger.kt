package app.src.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object AnalyticsLogger {
    private const val TAG = "AnalyticsLogger"

    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Offline"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Intentar detectar 4G/5G
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ puede detectar mejor las tecnologías
                        when {
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> "Cellular"
                            else -> getCellularSubtype(context)
                        }
                    } else {
                        getCellularSubtype(context)
                    }
                } catch (e: Exception) {
                    "Cellular"
                }
            }
            else -> "Unknown"
        }
    }

    private fun getCellularSubtype(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            when (tm.networkType) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G" // API 29+
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                else -> "Cellular"
            }
        } catch (e: Exception) {
            "Cellular"
        }
    }

    private fun getDeviceTier(): String {
        val ram = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        return when {
            ram < 2000 -> "low"
            ram < 4000 -> "mid"
            else -> "high"
        }
    }

    fun logMenuReady(
        context: Context,
        durationMs: Long,
        screen: String? = null,
        appVersion: String? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val networkType = getNetworkType(context)
        val deviceTier = getDeviceTier()
        val osApi = Build.VERSION.SDK_INT

        try {
            // 1. Enviar a Firebase Analytics
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putLong("duration_ms", durationMs)
                putString("network_type", networkType)
                putString("device_tier", deviceTier)
                putInt("os_api", osApi)
                screen?.let { putString("screen", it) }
                appVersion?.let { putString("app_version", it) }
            }
            analytics.logEvent("menu_ready", bundle)

            Log.d(TAG, "✅ Evento menu_ready enviado a Firebase - duration: ${durationMs}ms, network: $networkType, tier: $deviceTier")

            // Verificar conectividad
            if (networkType == "Offline") {
                Log.w(TAG, "⚠️ Dispositivo sin conexión - El evento se enviará cuando haya red")
            }

            // 2. Guardar en CSV local
            CSVEventLogger.logMenuReady(
                context = context,
                timestamp = timestamp,
                durationMs = durationMs,
                networkType = networkType,
                deviceTier = deviceTier,
                osApi = osApi,
                screen = screen,
                appVersion = appVersion
            )

            Log.d(TAG, "📄 Evento menu_ready guardado en CSV - Total eventos: ${CSVEventLogger.getEventCount(context)}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar evento menu_ready", e)
        }
    }

    fun logPaymentCompleted(
        context: Context,
        durationMs: Long,
        success: Boolean,
        paymentMethod: String? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val networkType = getNetworkType(context)
        val deviceTier = getDeviceTier()
        val osApi = Build.VERSION.SDK_INT

        try {
            // 1. Enviar a Firebase Analytics
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putLong("duration_ms", durationMs)
                putBoolean("success", success)
                putString("network_type", networkType)
                putString("device_tier", deviceTier)
                putInt("os_api", osApi)
                paymentMethod?.let { putString("payment_method", it) }
            }
            analytics.logEvent("payment_completed", bundle)

            Log.d(TAG, "✅ Evento payment_completed enviado a Firebase - duration: ${durationMs}ms, success: $success, network: $networkType")

            // Verificar conectividad
            if (networkType == "Offline") {
                Log.w(TAG, "⚠️ Dispositivo sin conexión - El evento se enviará cuando haya red")
            }

            // 2. Guardar en CSV local
            CSVEventLogger.logPaymentCompleted(
                context = context,
                timestamp = timestamp,
                durationMs = durationMs,
                success = success,
                networkType = networkType,
                deviceTier = deviceTier,
                osApi = osApi,
                paymentMethod = paymentMethod
            )

            Log.d(TAG, "📄 Evento payment_completed guardado en CSV - Total eventos: ${CSVEventLogger.getEventCount(context)}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar evento payment_completed", e)
        }
    }

    // NUEVA FUNCIÓN: Para medir tiempo completo desde app launch
    fun logAppLaunchToMenu(
        context: Context,
        durationMs: Long,
        appVersion: String? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val networkType = getNetworkType(context)
        val deviceTier = getDeviceTier()
        val osApi = Build.VERSION.SDK_INT

        try {
            // 1. Enviar a Firebase Analytics
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putLong("duration_ms", durationMs)
                putString("network_type", networkType)
                putString("device_tier", deviceTier)
                putInt("os_api", osApi)
                appVersion?.let { putString("app_version", it) }
            }
            analytics.logEvent("app_launch_to_menu", bundle)

            // 2. Guardar en CSV local
            CSVEventLogger.logAppLaunchToMenu(
                context = context,
                timestamp = timestamp,
                durationMs = durationMs,
                networkType = networkType,
                deviceTier = deviceTier,
                osApi = osApi,
                appVersion = appVersion
            )

            Log.d(TAG, "✅ app_launch_to_menu event logged: ${durationMs}ms, $networkType, $deviceTier")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging app_launch_to_menu event", e)
        }
    }

    /**
     * Obtiene la ruta del archivo CSV con todos los eventos
     */
    fun getCSVFilePath(context: Context): String {
        return CSVEventLogger.getCSVFilePath(context)
    }

    /**
     * Obtiene el número de eventos guardados en CSV
     */
    fun getEventCount(context: Context): Int {
        return CSVEventLogger.getEventCount(context)
    }
}
