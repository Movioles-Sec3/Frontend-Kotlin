package app.src.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object AnalyticsLogger {
    private const val TAG = "AnalyticsLogger"

    // Firestore instance
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

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

    /**
     * Verifica si hay conexion a Internet
     */
    private fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Guarda evento en cache offline (Room Database)
     */
    private fun saveToOfflineCache(context: Context, eventData: OfflineEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AnalyticsDatabase.getDatabase(context)
                val eventId = database.offlineEventDao().insert(eventData)
                Log.d(TAG, "💾 Evento ${eventData.eventType} guardado en caché offline (ID: $eventId)")

                val pendingCount = database.offlineEventDao().getPendingCount()
                Log.d(TAG, "📦 Total eventos pendientes: $pendingCount")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al guardar en caché offline", e)
            }
        }
    }

    /**
     * Intenta enviar a Firebase, si falla guarda en cache
     */
    private fun sendToFirebaseOrCache(context: Context, eventData: OfflineEvent) {
        if (hasInternetConnection(context)) {
            // Hay Internet: intentar enviar a Firestore
            val firestoreData = hashMapOf(
                "event_type" to eventData.eventType,
                "timestamp" to eventData.timestamp,
                "timestamp_readable" to eventData.timestampReadable,
                "duration_ms" to eventData.durationMs,
                "network_type" to eventData.networkType,
                "device_tier" to eventData.deviceTier,
                "os_api" to eventData.osApi,
                "device_model" to eventData.deviceModel,
                "device_brand" to eventData.deviceBrand,
                "os_version" to eventData.osVersion,
                "screen" to (eventData.screen ?: "unknown"),
                "app_version" to (eventData.appVersion ?: "1.0"),
                "success" to eventData.success,
                "payment_method" to eventData.paymentMethod
            )

            firestore.collection("analytics_events")
                .add(firestoreData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "🔥 Evento ${eventData.eventType} guardado en Firestore: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error al guardar en Firestore, guardando en caché offline", e)
                    saveToOfflineCache(context, eventData)
                }
        } else {
            // NO hay Internet: guardar en caché offline
            Log.w(TAG, "⚠️ Sin conexión a Internet - Guardando evento ${eventData.eventType} en caché offline")
            saveToOfflineCache(context, eventData)
        }

        // Programar sincronización automática
        scheduleSyncWork(context)
    }

    /**
     * Programa sincronizacion automatica con WorkManager
     */
    private fun scheduleSyncWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncOfflineEventsWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_offline_events",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
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

            Log.d(TAG, "✅ Evento menu_ready enviado a Firebase Analytics - duration: ${durationMs}ms, network: $networkType, tier: $deviceTier")

            // 2. Crear objeto de evento offline
            val eventData = OfflineEvent(
                eventType = "menu_ready",
                timestamp = timestamp,
                timestampReadable = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
                durationMs = durationMs,
                networkType = networkType,
                deviceTier = deviceTier,
                osApi = osApi,
                deviceModel = Build.MODEL,
                deviceBrand = Build.BRAND,
                osVersion = Build.VERSION.RELEASE,
                screen = screen,
                appVersion = appVersion ?: "1.0"
            )

            // 3. Enviar a Firestore o guardar en caché
            sendToFirebaseOrCache(context, eventData)

            // 4. Guardar en CSV local (backup)
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

            Log.d(TAG, "✅ Evento payment_completed enviado a Firebase Analytics - duration: ${durationMs}ms, success: $success, network: $networkType")

            // 2. Crear objeto de evento offline
            val eventData = OfflineEvent(
                eventType = "payment_completed",
                timestamp = timestamp,
                timestampReadable = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
                durationMs = durationMs,
                networkType = networkType,
                deviceTier = deviceTier,
                osApi = osApi,
                deviceModel = Build.MODEL,
                deviceBrand = Build.BRAND,
                osVersion = Build.VERSION.RELEASE,
                success = success,
                paymentMethod = paymentMethod,
                appVersion = "1.0"
            )

            // 3. Enviar a Firestore o guardar en caché
            sendToFirebaseOrCache(context, eventData)

            // 4. Guardar en CSV local (backup)
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

            // 2. Crear objeto de evento offline
            val eventData = OfflineEvent(
                eventType = "app_launch_to_menu",
                timestamp = timestamp,
                timestampReadable = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
                durationMs = durationMs,
                networkType = networkType,
                deviceTier = deviceTier,
                osApi = osApi,
                deviceModel = Build.MODEL,
                deviceBrand = Build.BRAND,
                osVersion = Build.VERSION.RELEASE,
                appVersion = appVersion ?: "1.0"
            )

            // 3. Enviar a Firestore o guardar en caché
            sendToFirebaseOrCache(context, eventData)

            // 4. Guardar en CSV local (backup)
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
