package app.src

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import app.src.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity secreta para mostrar analytics de favoritos
 * Se accede manteniendo presionado el botón de Logout
 */
class FavoritesAnalyticsActivity : BaseActivity() {

    private lateinit var tvTotalFavorites: TextView
    private lateinit var tvAddFromFavoritesCount: TextView
    private lateinit var tvAddFromProductsCount: TextView
    private lateinit var tvFrequency: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvAveragePerDay: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites_analytics)

        initializeViews()
        setupButtons()
        loadAnalytics()
    }

    private fun initializeViews() {
        tvTotalFavorites = findViewById(R.id.tv_total_favorites)
        tvAddFromFavoritesCount = findViewById(R.id.tv_add_from_favorites_count)
        tvAddFromProductsCount = findViewById(R.id.tv_add_from_products_count)
        tvFrequency = findViewById(R.id.tv_frequency)
        tvLastUpdate = findViewById(R.id.tv_last_update)
        tvAveragePerDay = findViewById(R.id.tv_average_per_day)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnClose = findViewById(R.id.btn_close)
    }

    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            loadAnalytics()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun loadAnalytics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@FavoritesAnalyticsActivity)
            val favoritoDao = db.favoritoDao()

            // Obtener estadísticas
            val totalFavorites = favoritoDao.countFavoritos()
            val allFavorites = favoritoDao.getAllFavoritesOnce()

            // Calcular estadísticas de tiempo
            val now = System.currentTimeMillis()
            val oneDayAgo = now - (24 * 60 * 60 * 1000)
            val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)

            val favoritesLast24h = allFavorites.count { it.fechaAgregado > oneDayAgo }
            val favoritesLastWeek = allFavorites.count { it.fechaAgregado > oneWeekAgo }

            // Calcular promedio por día
            val oldestFavorite = allFavorites.minByOrNull { it.fechaAgregado }
            val daysSinceFirst = if (oldestFavorite != null) {
                ((now - oldestFavorite.fechaAgregado) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
            } else {
                1
            }
            val averagePerDay = if (totalFavorites > 0) {
                totalFavorites.toFloat() / daysSinceFirst
            } else {
                0f
            }

            // Simular contadores de "agregar al carrito desde favoritos"
            // En una implementación real, estos datos vendrían de un sistema de tracking
            val addFromFavoritesCount = getAddFromFavoritesCount()
            val addFromProductsCount = getAddFromProductsCount()

            val totalAdds = addFromFavoritesCount + addFromProductsCount
            val frequencyPercent = if (totalAdds > 0) {
                (addFromFavoritesCount.toFloat() / totalAdds * 100)
            } else {
                0f
            }

            withContext(Dispatchers.Main) {
                tvTotalFavorites.text = "Total de favoritos: $totalFavorites"
                tvAddFromFavoritesCount.text = "Agregados al carrito desde Favoritos: $addFromFavoritesCount"
                tvAddFromProductsCount.text = "Agregados al carrito desde Productos: $addFromProductsCount"

                val frequencyText = if (totalAdds > 0) {
                    String.format("%.1f%% de los productos agregados al carrito provienen de Favoritos", frequencyPercent)
                } else {
                    "Sin datos de agregar al carrito"
                }
                tvFrequency.text = frequencyText

                tvAveragePerDay.text = String.format("Promedio: %.2f favoritos por día", averagePerDay)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                tvLastUpdate.text = "Actualizado: ${dateFormat.format(Date())}"
            }
        }
    }

    /**
     * Obtiene el contador de productos agregados al carrito desde Favoritos
     * En una implementación real, esto vendría de un sistema de analytics
     */
    private fun getAddFromFavoritesCount(): Int {
        val prefs = getSharedPreferences("favorites_analytics", MODE_PRIVATE)
        return prefs.getInt("add_from_favorites_count", 0)
    }

    /**
     * Obtiene el contador de productos agregados al carrito desde vista de Productos
     */
    private fun getAddFromProductsCount(): Int {
        val prefs = getSharedPreferences("favorites_analytics", MODE_PRIVATE)
        return prefs.getInt("add_from_products_count", 0)
    }

    companion object {
        /**
         * Incrementa el contador cuando se agrega un producto al carrito desde Favoritos
         */
        fun trackAddFromFavorites(activity: android.app.Activity) {
            val prefs = activity.getSharedPreferences("favorites_analytics", MODE_PRIVATE)
            val current = prefs.getInt("add_from_favorites_count", 0)
            prefs.edit().putInt("add_from_favorites_count", current + 1).apply()
        }

        /**
         * Incrementa el contador cuando se agrega un producto al carrito desde Productos
         */
        fun trackAddFromProducts(activity: android.app.Activity) {
            val prefs = activity.getSharedPreferences("favorites_analytics", MODE_PRIVATE)
            val current = prefs.getInt("add_from_products_count", 0)
            prefs.edit().putInt("add_from_products_count", current + 1).apply()
        }
    }
}

