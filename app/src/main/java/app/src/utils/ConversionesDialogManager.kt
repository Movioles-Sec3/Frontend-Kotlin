package app.src.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import app.src.R
import app.src.data.models.ProductoConConversiones
import app.src.data.repositories.ConversionesRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ConversionesDialogManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    
    private val conversionesRepository = ConversionesRepository()
    
    // Mapeo de códigos de moneda a nombres
    private val currencyNames = mapOf(
        "USD" to "Dólar Estadounidense",
        "EUR" to "Euro",
        "MXN" to "Peso Mexicano",
        "GBP" to "Libra Esterlina",
        "JPY" to "Yen Japonés",
        "CAD" to "Dólar Canadiense",
        "AUD" to "Dólar Australiano",
        "CHF" to "Franco Suizo",
        "CNY" to "Yuan Chino",
        "BRL" to "Real Brasileño"
    )
    
    // Colores para cada moneda
    private val currencyColors = mapOf(
        "USD" to "#4CAF50",
        "EUR" to "#2196F3", 
        "MXN" to "#FF9800",
        "GBP" to "#9C27B0",
        "JPY" to "#F44336",
        "CAD" to "#795548",
        "AUD" to "#607D8B",
        "CHF" to "#E91E63",
        "CNY" to "#3F51B5",
        "BRL" to "#8BC34A"
    )
    
    fun mostrarConversiones(productoId: Int, nombreProducto: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_conversiones, null)
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton("Cerrar", null)
            .create()
        
        // Configurar elementos del diálogo
        val tvProductName = dialogView.findViewById<TextView>(R.id.tv_product_name)
        val tvPrecioOriginal = dialogView.findViewById<TextView>(R.id.tv_precio_original)
        val tvMonedaOriginal = dialogView.findViewById<TextView>(R.id.tv_moneda_original)
        val llConversionesContainer = dialogView.findViewById<LinearLayout>(R.id.ll_conversiones_container)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pb_loading)
        val tvError = dialogView.findViewById<TextView>(R.id.tv_error)
        val tvFechaActualizacion = dialogView.findViewById<TextView>(R.id.tv_fecha_actualizacion)
        
        tvProductName.text = nombreProducto
        
        // Mostrar loading
        pbLoading.visibility = View.VISIBLE
        llConversionesContainer.visibility = View.GONE
        tvError.visibility = View.GONE
        
        // Llamar al endpoint con soporte de caché
        lifecycleScope.launch {
            val result = conversionesRepository.obtenerConversiones(productoId, context)
            when (result) {
                is Result.Success -> {
                    mostrarConversionesExitosas(
                        result.data,
                        tvPrecioOriginal,
                        tvMonedaOriginal,
                        llConversionesContainer,
                        tvFechaActualizacion,
                        result.isFromCache,
                        result.isCacheExpired
                    )
                    pbLoading.visibility = View.GONE
                    llConversionesContainer.visibility = View.VISIBLE
                }
                is Result.Error -> {
                    pbLoading.visibility = View.GONE
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Error: ${result.message}"
                }
                is Result.Loading -> {
                    pbLoading.visibility = View.VISIBLE
                    llConversionesContainer.visibility = View.GONE
                    tvError.visibility = View.GONE
                }
            }
        }

        dialog.show()
    }

    private fun mostrarConversionesExitosas(
        data: ProductoConConversiones,
        tvPrecioOriginal: TextView,
        tvMonedaOriginal: TextView,
        container: LinearLayout,
        tvFechaActualizacion: TextView,
        isFromCache: Boolean = false,
        isCacheExpired: Boolean = false
    ) {
        // Mostrar precio original
        tvPrecioOriginal.text = String.format(Locale.US, "%.0f", data.precioOriginal)
        tvMonedaOriginal.text = data.monedaOriginal

        // Limpiar container
        container.removeAllViews()

        // Añadir cada conversión
        data.conversiones.forEach { (currency, price) ->
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_conversion, container, false)

            val tvCurrencyCode = itemView.findViewById<TextView>(R.id.tv_currency_code)
            val tvCurrencyName = itemView.findViewById<TextView>(R.id.tv_currency_name)
            val tvConvertedPrice = itemView.findViewById<TextView>(R.id.tv_converted_price)
            val vCurrencyIndicator = itemView.findViewById<View>(R.id.v_currency_indicator)

            tvCurrencyCode.text = currency
            tvCurrencyName.text = currencyNames[currency] ?: currency
            tvConvertedPrice.text = formatPrice(price, currency)

            // Establecer color del indicador
            val color = currencyColors[currency] ?: "#2196F3"
            vCurrencyIndicator.setBackgroundColor(android.graphics.Color.parseColor(color))

            container.addView(itemView)
        }

        // Mostrar fecha de actualización con indicador de caché
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            val date = inputFormat.parse(data.fechaActualizacion)

            val fechaText = date?.let { outputFormat.format(it) } ?: "Fecha desconocida"

            // Agregar indicador visual de caché
            val cacheIndicator = when {
                isFromCache && isCacheExpired -> " 📦⚠️ (Caché expirado - Sin internet)"
                isFromCache -> " 📦 (Desde caché)"
                else -> " 🌐 (Datos actualizados)"
            }

            tvFechaActualizacion.text = "Actualizado: $fechaText$cacheIndicator"
        } catch (e: Exception) {
            tvFechaActualizacion.text = "Actualizado: ${data.fechaActualizacion}"
        }
    }

    private fun formatPrice(price: Double, currency: String): String {
        return when (currency) {
            "USD", "CAD", "AUD" -> String.format(Locale.US, "$%.2f", price)
            "EUR" -> String.format(Locale.US, "€%.2f", price)
            "GBP" -> String.format(Locale.US, "£%.2f", price)
            "JPY" -> String.format(Locale.US, "¥%.0f", price)
            "MXN" -> String.format(Locale.US, "$%.2f", price)
            "CHF" -> String.format(Locale.US, "CHF %.2f", price)
            "CNY" -> String.format(Locale.US, "¥%.2f", price)
            "BRL" -> String.format(Locale.US, "R$%.2f", price)
            else -> String.format(Locale.US, "%.2f %s", price, currency)
        }
    }
}
