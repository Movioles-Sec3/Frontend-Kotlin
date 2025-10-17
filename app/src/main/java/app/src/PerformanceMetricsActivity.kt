package app.src

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import app.src.utils.PerformanceMetrics
import kotlinx.coroutines.launch

/**
 * Activity para visualizar métricas de rendimiento
 * Compara carga paralela vs. secuencial
 */
class PerformanceMetricsActivity : BaseActivity() {

    private lateinit var tvReport: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance_metrics)

        initializeViews()
        setupListeners()
        loadMetrics()
    }

    private fun initializeViews() {
        tvReport = findViewById(R.id.tv_performance_report)
        btnRefresh = findViewById(R.id.btn_refresh_metrics)
        btnClear = findViewById(R.id.btn_clear_metrics)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun setupListeners() {
        btnRefresh.setOnClickListener {
            loadMetrics()
            Toast.makeText(this, "Métricas actualizadas", Toast.LENGTH_SHORT).show()
        }

        btnClear.setOnClickListener {
            showClearConfirmation()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadMetrics() {
        lifecycleScope.launch {
            try {
                val report = PerformanceMetrics.generateReport(this@PerformanceMetricsActivity)
                tvReport.text = report
            } catch (e: Exception) {
                tvReport.text = "Error al cargar métricas: ${e.message}"
            }
        }
    }

    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Métricas")
            .setMessage("¿Estás seguro de que deseas eliminar todas las métricas de rendimiento?")
            .setPositiveButton("Sí") { _, _ ->
                clearMetrics()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearMetrics() {
        lifecycleScope.launch {
            try {
                PerformanceMetrics.clearAllMeasurements(this@PerformanceMetricsActivity)
                loadMetrics()
                Toast.makeText(
                    this@PerformanceMetricsActivity,
                    "Métricas limpiadas exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PerformanceMetricsActivity,
                    "Error al limpiar métricas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

