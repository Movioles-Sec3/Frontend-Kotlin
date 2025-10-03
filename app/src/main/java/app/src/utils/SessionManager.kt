package app.src.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "TapAndToastPrefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_SALDO = "user_saldo"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPreferences(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }

    fun saveUserData(context: Context, id: Int, nombre: String, email: String, saldo: Double) {
        getPreferences(context).edit().apply {
            putInt(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_EMAIL, email)
            putFloat(KEY_USER_SALDO, saldo.toFloat())
            apply()
        }
    }

    fun getUserName(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_EMAIL, null)
    }

    fun getUserSaldo(context: Context): Double {
        return getPreferences(context).getFloat(KEY_USER_SALDO, 0f).toDouble()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }

    fun clearSession(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}

