package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.adapters.OrderHistoryAdapter
import app.src.data.api.ApiClient
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

        // Observer
        viewModel.historial.observe(this) { compras ->
            progressBar.visibility = View.GONE

            if (compras.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvError.visibility = View.GONE
            } else {
                recyclerView.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                tvError.visibility = View.GONE

                val adapter = OrderHistoryAdapter(compras) { compra ->
                    // Click on order - show details
                    val qrCode = compra.qr?.codigoQrHash
                    if (qrCode != null) {
                        val intent = Intent(this, OrderPickupActivity::class.java)
                        intent.putExtra("qr_code", qrCode)
                        intent.putExtra("compra_id", compra.id)
                        intent.putExtra("total", compra.total)
                        intent.putExtra("estado", compra.estado.name)
                        startActivity(intent)
                    }
                }
                recyclerView.adapter = adapter
            }
        }

        // Load history
        viewModel.cargarHistorial()

        // Back button
        findViewById<Button>(R.id.btn_back_to_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}
