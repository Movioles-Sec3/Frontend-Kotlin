package app.src.utils

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_events")
data class OfflineEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val eventType: String,
    val timestamp: Long,
    val timestampReadable: String,
    val durationMs: Long,
    val networkType: String,
    val deviceTier: String,
    val osApi: Int,
    val deviceModel: String,
    val deviceBrand: String,
    val osVersion: String,
    val screen: String? = null,
    val appVersion: String? = null,
    val success: Boolean? = null,
    val paymentMethod: String? = null,

    val isSynced: Boolean = false,
    val retryCount: Int = 0,
    val lastError: String? = null
)

