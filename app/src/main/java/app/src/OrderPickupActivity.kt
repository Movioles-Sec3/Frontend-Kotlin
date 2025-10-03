package app.src

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.annotation.AttrRes
import com.google.android.material.color.MaterialColors
import app.src.databinding.ActivityOrderPickupBinding

class OrderPickupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderPickupBinding
    private val vm: OrderPickupViewModel by viewModels()

    private var originalBrightness: Float? = null
    private var highContrastOn = false

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
            val code = binding.tvPickupCode.text?.toString().orEmpty()
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_pickup_code, code))
            }
            startActivity(Intent.createChooser(share, getString(R.string.share_via)))
        }

        // Copiar al tocar el código
        binding.tvPickupCode.setOnClickListener {
            val code = binding.tvPickupCode.text?.toString().orEmpty()
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("pickup_code", code))
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        // Alternar alto contraste + brillo máximo al tocar largo la tarjeta
        binding.cardOrder.setOnLongClickListener {
            highContrastOn = !highContrastOn
            toggleHighContrast(highContrastOn)
            true
        }

        // Cargar datos
        vm.ui.observe(this) { ui ->
            binding.tvTitle.text = ui.title
            binding.tvSubtitle.text = ui.subtitle
            binding.tvPickupCode.text = ui.code
        }

        val code = intent.getStringExtra("pickup_code")
        val title = intent.getStringExtra("title")
        val subtitle = intent.getStringExtra("subtitle")
        vm.load(code, title, subtitle)
    }

    private fun toggleHighContrast(on: Boolean) {
        if (on) {
            // Modo alto contraste para entornos oscuros/ruidosos
            binding.cardOrder.setCardBackgroundColor(getColor(android.R.color.black))
            binding.tvPickupCode.setTextColor(getColor(android.R.color.white))
            binding.tvTitle.setTextColor(getColor(android.R.color.white))
            binding.tvSubtitle.setTextColor(getColor(android.R.color.white))

            // Brillo máximo temporal
            originalBrightness = window.attributes.screenBrightness
            window.attributes = window.attributes.apply { screenBrightness = 1.0f }
        } else {
            // Volver a colores del tema Material 3 (colorSurface / colorOnSurface)
            val surface = themeColor(com.google.android.material.R.attr.colorSurface)
            val onSurface = themeColor(com.google.android.material.R.attr.colorOnSurface)

            binding.cardOrder.setCardBackgroundColor(surface)
            binding.tvPickupCode.setTextColor(onSurface)
            binding.tvTitle.setTextColor(onSurface)
            binding.tvSubtitle.setTextColor(onSurface)

            // Restaurar brillo
            window.attributes = window.attributes.apply {
                screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    // Helper para obtener colores del tema actual (incluye modo oscuro/dinámico)
    private fun themeColor(@AttrRes attrId: Int): Int =
        MaterialColors.getColor(binding.root, attrId)

    override fun onDestroy() {
        super.onDestroy()
        // Asegurar restauración del brillo
        window.attributes = window.attributes.apply {
            screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }
}
