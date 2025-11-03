package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.OrderHistoryAdapter
import app.src.data.api.ApiClient
import app.src.utils.NetworkUtils
import app.src.utils.SessionManager

class OrderHistoryActivity : BaseActivity() {

    private val viewModel: CompraViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        // Load session token
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }

        // Initialize views
        recyclerView = findViewById(R.id.rv_order_history)
        progressBar = findViewById(R.id.progress_bar)
        tvEmpty = findViewById(R.id.tv_empty)
        tvError = findViewById(R.id.tv_error)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Check if offline and show toast
        val isOffline = !NetworkUtils.isNetworkAvailable(this)
        if (isOffline) {
            Toast.makeText(
                this,
                "📱 Modo Offline: Mostrando historial y códigos QR desde caché",
                Toast.LENGTH_LONG
            ).show()
        }

        // Observer
        viewModel.historial.observe(this) { compras ->
            progressBar.visibility = View.GONE

            if (compras.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvError.visibility = View.GONE

                // Si está offline y no hay datos, mostrar mensaje específico
                if (isOffline) {
                    tvEmpty.text = "No hay historial en caché. Conéctate a internet para ver tus pedidos."
                }
            } else {
                recyclerView.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                tvError.visibility = View.GONE

                val adapter = OrderHistoryAdapter(compras) { compra ->
                    // ✅ Click on order - show details with QR (siempre disponible offline)
                    val intent = Intent(this, OrderPickupActivity::class.java)
                    intent.putExtra("qr_code", compra.qr?.codigoQrHash ?: "")
                    intent.putExtra("compra_id", compra.id)
                    intent.putExtra("total", compra.total)
                    intent.putExtra("estado", compra.estado.name)
                    startActivity(intent)
                }
                recyclerView.adapter = adapter
            }
        }

        // Load history (con caché automático)
        viewModel.cargarHistorial()

        // Back button
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}
