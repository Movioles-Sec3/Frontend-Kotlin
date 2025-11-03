package app.src

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import app.src.data.local.AppDatabase
import app.src.data.local.CatalogCacheManager
import app.src.data.local.CoilImageCacheManager
import app.src.data.local.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad de debug para inspeccionar el estado del almacenamiento local
 * Acceso: Long press en el botón de Profile durante 3 segundos
 */
class LocalStorageDebugActivity : BaseActivity() {

    private lateinit var tvDebugInfo: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnClearCache: Button
    private lateinit var database: AppDatabase
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var catalogCache: CatalogCacheManager
    private lateinit var imageCache: CoilImageCacheManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_storage_debug)

        tvDebugInfo = findViewById(R.id.tvDebugInfo)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClearCache = findViewById(R.id.btnClearCache)

        // Inicializar managers
        database = AppDatabase.getDatabase(this)
        dataStoreManager = DataStoreManager(this)
        catalogCache = CatalogCacheManager(this)
        imageCache = CoilImageCacheManager(this)

        setupButtons()
        loadDebugInfo()
    }

    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            loadDebugInfo()
            Toast.makeText(this, "🔄 Datos actualizados", Toast.LENGTH_SHORT).show()
        }

        btnClearCache.setOnClickListener {
            lifecycleScope.launch {
                catalogCache.clearAll()
                imageCache.clearCache()
                Toast.makeText(this@LocalStorageDebugActivity, "🗑️ Caché eliminado", Toast.LENGTH_SHORT).show()
                loadDebugInfo()
            }
        }
    }

    private fun loadDebugInfo() {
        lifecycleScope.launch {
            val info = buildString {
                appendLine("═══════════════════════════════════════════")
                appendLine("   📊 LOCAL STORAGE DEBUG INSPECTOR")
                appendLine("═══════════════════════════════════════════")
                appendLine()

                // === ROOM DATABASE ===
                appendLine("🗄️ ROOM DATABASE")
                appendLine("─────────────────────────────────────────────")

                // Orders
                val userId = dataStoreManager.userId.first() ?: 0
                val orderCount = database.orderDao().getOrderCount(userId)
                appendLine("📦 Orders: $orderCount")

                val orders = database.orderDao().getOrdersByUser(userId).first()
                orders.forEach { order ->
                    val items = database.orderDao().getOrderItems(order.id)
                    appendLine("  • Order #${order.id}")
                    appendLine("    Status: ${order.status}")
                    appendLine("    Total: $${order.total}")
                    appendLine("    Items: ${items.size}")
                    appendLine("    Created: ${formatTimestamp(order.createdAt)}")
                    if (order.readyAt != null) {
                        appendLine("    Ready: ${formatTimestamp(order.readyAt)}")
                    }
                    if (order.deliveredAt != null) {
                        appendLine("    Delivered: ${formatTimestamp(order.deliveredAt)}")
                    }
                    appendLine()
                }

                // Outbox
                val pendingCount = database.orderOutboxDao().getPendingCount()
                appendLine("📤 Outbox (pending sync): $pendingCount")

                val outboxItems = database.orderOutboxDao().getAllPending()
                outboxItems.forEach { item ->
                    appendLine("  • Outbox #${item.id}")
                    appendLine("    Retries: ${item.retries}")
                    appendLine("    Created: ${formatTimestamp(item.createdAt)}")
                    if (item.lastAttempt != null) {
                        appendLine("    Last attempt: ${formatTimestamp(item.lastAttempt)}")
                    }
                    appendLine()
                }

                // Catalog Cache
                val cacheCount = catalogCache.getValidCacheCount()
                appendLine("💾 Catalog Cache: $cacheCount valid entries")
                appendLine()

                // === DATASTORE ===
                appendLine("🔑 DATASTORE (Preferences)")
                appendLine("─────────────────────────────────────────────")

                val lastOrderId = dataStoreManager.lastOrderId.first()
                val lastOrderTotal = dataStoreManager.lastOrderTotal.first()
                val activePickupId = dataStoreManager.activePickupOrderId.first()
                val storedUserId = dataStoreManager.userId.first()

                appendLine("Last Order ID: ${lastOrderId ?: "none"}")
                appendLine("Last Order Total: ${lastOrderTotal?.let { "$$it" } ?: "none"}")
                appendLine("Active Pickup Order: ${activePickupId ?: "none"}")
                appendLine("User ID: ${storedUserId ?: "none"}")
                appendLine()

                // === IMAGE CACHE ===
                appendLine("🖼️ IMAGE CACHE (Coil)")
                appendLine("─────────────────────────────────────────────")

                val memoryCacheSize = imageCache.getMemoryCacheSize()
                val diskCacheSize = imageCache.getDiskCacheSize()

                appendLine("Memory Cache: $memoryCacheSize items")
                appendLine("Disk Cache: ${formatBytes(diskCacheSize)}")
                appendLine()

                // === RESUMEN ===
                appendLine("═══════════════════════════════════════════")
                appendLine("📊 STORAGE SUMMARY")
                appendLine("═══════════════════════════════════════════")
                appendLine("Total Orders Cached: $orderCount")
                appendLine("Pending Sync: $pendingCount")
                appendLine("Catalog Pages: $cacheCount")
                appendLine("Image Cache: ${formatBytes(diskCacheSize)}")
                appendLine()
                appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            }

            tvDebugInfo.text = info
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
