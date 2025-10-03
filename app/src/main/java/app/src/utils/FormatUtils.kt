package app.src.utils

import java.text.NumberFormat
import java.util.*

object FormatUtils {

    private val currencyFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatDate(isoDate: String): String {
        // Formato: "2025-10-03T14:30:00" -> "3 Oct 2025, 2:30 PM"
        return try {
            // Aquí puedes usar SimpleDateFormat o java.time si API >= 26
            isoDate.replace("T", " ").substring(0, 16)
        } catch (e: Exception) {
            isoDate
        }
    }
}

