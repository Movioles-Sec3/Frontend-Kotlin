package app.src.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Manager para preferencias ligeras
 * Usado para acceso rápido a valores sin consultar Room
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tapandtoast_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        // Keys para órdenes
        val LAST_ORDER_ID = intPreferencesKey("last_order_id")
        val LAST_ORDER_TOTAL = doublePreferencesKey("last_order_total")
        val LAST_ACTIVE_PICKUP_ORDER_ID = intPreferencesKey("last_active_pickup_order_id")

        // Keys para cache de historial
        val ORDERS_LIST_DIGEST = stringPreferencesKey("orders_list_digest")

        // Keys para usuario
        val USER_ID = intPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
    }

    // === ÓRDENES ===
    suspend fun saveLastOrder(orderId: Int, total: Double) {
        context.dataStore.edit { prefs ->
            prefs[LAST_ORDER_ID] = orderId
            prefs[LAST_ORDER_TOTAL] = total
        }
    }

    val lastOrderId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[LAST_ORDER_ID]
    }

    val lastOrderTotal: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[LAST_ORDER_TOTAL]
    }

    suspend fun saveActivePickupOrder(orderId: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_ACTIVE_PICKUP_ORDER_ID] = orderId
        }
    }

    val activePickupOrderId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[LAST_ACTIVE_PICKUP_ORDER_ID]
    }

    suspend fun clearActivePickupOrder() {
        context.dataStore.edit { prefs ->
            prefs.remove(LAST_ACTIVE_PICKUP_ORDER_ID)
        }
    }

    // === HISTORIAL DE ÓRDENES ===
    suspend fun saveOrdersDigest(digest: String) {
        context.dataStore.edit { prefs ->
            prefs[ORDERS_LIST_DIGEST] = digest
        }
    }

    val ordersDigest: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ORDERS_LIST_DIGEST]
    }

    // === USUARIO ===
    suspend fun saveUserId(userId: Int) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = userId
        }
    }

    val userId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID]
    }

    // === LIMPIEZA ===
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

