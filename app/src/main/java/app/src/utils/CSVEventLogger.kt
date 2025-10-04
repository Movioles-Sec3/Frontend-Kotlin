package app.src.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object CSVEventLogger {
    private const val TAG = "CSVEventLogger"
    private const val CSV_FILENAME = "analytics_events.csv"
    private const val CSV_HEADER = "timestamp,event_name,duration_ms,network_type,device_tier,os_api,success,payment_method,screen,app_version,device_model,android_version"

    /**
     * Obtiene el archivo CSV. Si no existe, lo crea con el header.
     */
    private fun getCsvFile(context: Context): File {
        // Usar almacenamiento interno de la app (siempre accesible sin permisos)
        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        val csvFile = File(directory, CSV_FILENAME)

        // Si el archivo no existe, crear con header
        if (!csvFile.exists()) {
            try {
                csvFile.createNewFile()
                FileWriter(csvFile, false).use { writer ->
                    writer.append(CSV_HEADER)
                    writer.append("\n")
                }
                Log.d(TAG, "✅ Archivo CSV creado en: ${csvFile.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "❌ Error al crear archivo CSV", e)
            }
        }

        return csvFile
    }

    /**
     * Obtiene información del dispositivo
     */
    private fun getDeviceInfo(): Pair<String, String> {
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        return Pair(deviceModel, androidVersion)
    }

    /**
     * Formatea un timestamp a formato legible
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Escapa valores para CSV (maneja comas y comillas)
     */
    private fun escapeCsvValue(value: String?): String {
        if (value == null) return ""
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Guarda un evento menu_ready en el CSV
     */
    fun logMenuReady(
        context: Context,
        timestamp: Long,
        durationMs: Long,
        networkType: String,
        deviceTier: String,
        osApi: Int,
        screen: String?,
        appVersion: String?
    ) {
        val (deviceModel, androidVersion) = getDeviceInfo()

        val csvLine = buildString {
            append(formatTimestamp(timestamp)).append(",")
            append("menu_ready").append(",")
            append(durationMs).append(",")
            append(escapeCsvValue(networkType)).append(",")
            append(escapeCsvValue(deviceTier)).append(",")
            append(osApi).append(",")
            append("").append(",") // success (solo para payment)
            append("").append(",") // payment_method (solo para payment)
            append(escapeCsvValue(screen)).append(",")
            append(escapeCsvValue(appVersion)).append(",")
            append(escapeCsvValue(deviceModel)).append(",")
            append(escapeCsvValue(androidVersion))
        }

        writeToCSV(context, csvLine)
    }

    /**
     * Guarda un evento payment_completed en el CSV
     */
    fun logPaymentCompleted(
        context: Context,
        timestamp: Long,
        durationMs: Long,
        success: Boolean,
        networkType: String,
        deviceTier: String,
        osApi: Int,
        paymentMethod: String?
    ) {
        val (deviceModel, androidVersion) = getDeviceInfo()

        val csvLine = buildString {
            append(formatTimestamp(timestamp)).append(",")
            append("payment_completed").append(",")
            append(durationMs).append(",")
            append(escapeCsvValue(networkType)).append(",")
            append(escapeCsvValue(deviceTier)).append(",")
            append(osApi).append(",")
            append(success).append(",")
            append(escapeCsvValue(paymentMethod)).append(",")
            append("").append(",") // screen (solo para menu_ready)
            append("").append(",") // app_version
            append(escapeCsvValue(deviceModel)).append(",")
            append(escapeCsvValue(androidVersion))
        }

        writeToCSV(context, csvLine)
    }

    /**
     * Guarda un evento app_launch_to_menu en el CSV
     */
    fun logAppLaunchToMenu(
        context: Context,
        timestamp: Long,
        durationMs: Long,
        networkType: String,
        deviceTier: String,
        osApi: Int,
        appVersion: String?
    ) {
        val (deviceModel, androidVersion) = getDeviceInfo()

        val csvLine = buildString {
            append(formatTimestamp(timestamp)).append(",")
            append("app_launch_to_menu").append(",")
            append(durationMs).append(",")
            append(escapeCsvValue(networkType)).append(",")
            append(escapeCsvValue(deviceTier)).append(",")
            append(osApi).append(",")
            append("").append(",") // success (solo para payment)
            append("").append(",") // payment_method (solo para payment)
            append("").append(",") // screen (solo para menu_ready)
            append(escapeCsvValue(appVersion)).append(",")
            append(escapeCsvValue(deviceModel)).append(",")
            append(escapeCsvValue(androidVersion))
        }

        writeToCSV(context, csvLine)
    }

    /**
     * Escribe una línea en el archivo CSV
     */
    private fun writeToCSV(context: Context, line: String) {
        try {
            val csvFile = getCsvFile(context)
            FileWriter(csvFile, true).use { writer ->
                writer.append(line)
                writer.append("\n")
            }
            Log.d(TAG, "✅ Evento guardado en CSV: $line")
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error al escribir en CSV", e)
        }
    }

    /**
     * Obtiene la ruta del archivo CSV para compartir
     */
    fun getCSVFilePath(context: Context): String {
        return getCsvFile(context).absolutePath
    }

    /**
     * Obtiene el contenido del CSV como String
     */
    fun getCSVContent(context: Context): String {
        return try {
            getCsvFile(context).readText()
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error al leer CSV", e)
            ""
        }
    }

    /**
     * Obtiene el número de eventos guardados en el CSV
     */
    fun getEventCount(context: Context): Int {
        return try {
            val lines = getCsvFile(context).readLines()
            // Restar 1 por el header
            maxOf(0, lines.size - 1)
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error al contar eventos", e)
            0
        }
    }

    /**
     * Limpia el archivo CSV (mantiene solo el header)
     */
    fun clearCSV(context: Context) {
        try {
            val csvFile = getCsvFile(context)
            FileWriter(csvFile, false).use { writer ->
                writer.append(CSV_HEADER)
                writer.append("\n")
            }
            Log.d(TAG, "✅ CSV limpiado")
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error al limpiar CSV", e)
        }
    }
}
