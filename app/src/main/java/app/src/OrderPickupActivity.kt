package app.src

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.src.databinding.ActivityOrderPickupBinding

class OrderPickupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderPickupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityOrderPickupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnBackToHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnShareCode.setOnClickListener {
            val code = binding.tvPickupCode.text?.toString().orEmpty().ifEmpty { "ABX-9321" }
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Mi código de recogida: $code")
            }
            startActivity(Intent.createChooser(share, "Compartir vía"))
        }

        // binding.tvPickupCode.text = intent.getStringExtra("pickup_code") ?: "ABX-9321"
        // binding.tvTitle.text = intent.getStringExtra("title") ?: "¡Tu orden está lista!"
        // binding.tvSubtitle.text = intent.getStringExtra("subtitle") ?: "Acércate al punto de entrega y muestra tu código."
    }
}
