package app.src.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncOfflineEventsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncOfflineEventsWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Iniciando sincronización automática de eventos pendientes")

            val database = AnalyticsDatabase.getDatabase(applicationContext)
            val offlineEventDao = database.offlineEventDao()
            val firestore = FirebaseFirestore.getInstance()

            val pendingEvents = offlineEventDao.getPendingEvents()

            if (pendingEvents.isEmpty()) {
                Log.d(TAG, "✅ No hay eventos pendientes para sincronizar")
                return@withContext Result.success()
            }

            Log.d(TAG, "📦 Sincronizando ${pendingEvents.size} eventos pendientes")

            var syncedCount = 0
            var failedCount = 0

            for (event in pendingEvents) {
                try {
                    // Crear documento en Firestore
                    val eventData = hashMapOf(
                        "event_type" to event.eventType,
                        "timestamp" to event.timestamp,
                        "timestamp_readable" to event.timestampReadable,
                        "duration_ms" to event.durationMs,
                        "network_type" to event.networkType,
                        "device_tier" to event.deviceTier,
                        "os_api" to event.osApi,
                        "device_model" to event.deviceModel,
                        "device_brand" to event.deviceBrand,
                        "os_version" to event.osVersion,
                        "screen" to (event.screen ?: "unknown"),
                        "app_version" to (event.appVersion ?: "1.0"),
                        "success" to event.success,
                        "payment_method" to event.paymentMethod
                    )

                    // Enviar a Firestore (sincrónico usando Tasks.await())
                    firestore.collection("analytics_events")
                        .add(eventData)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "🔥 Evento ${event.eventType} (ID: ${event.id}) sincronizado: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Error al sincronizar evento ${event.eventType}", e)
                        }

                    // Esperar un poco para que Firebase procese
                    kotlinx.coroutines.delay(100)

                    // Marcar como sincronizado
                    offlineEventDao.markAsSynced(event.id)
                    syncedCount++

                } catch (e: Exception) {
                    failedCount++
                    val newRetryCount = event.retryCount + 1
                    offlineEventDao.updateRetryInfo(event.id, newRetryCount, e.message ?: "Unknown error")
                    Log.e(TAG, "❌ Error al sincronizar evento ${event.eventType}: ${e.message}")
                }
            }

            // Limpiar eventos sincronizados de más de 7 días
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            offlineEventDao.deleteSyncedOlderThan(sevenDaysAgo)

            Log.d(TAG, "🎉 Sincronización completada: $syncedCount exitosos, $failedCount fallidos")

            return@withContext if (failedCount == 0) {
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fatal en sincronización", e)
            return@withContext Result.failure()
        }
    }
}

