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
    private const val KEY_NIGHT_MODE = "night_mode"

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

    /**
     * ✅ REQUERIMIENTO 3: Obtiene el saldo del usuario (alias para compatibilidad)
     */
    fun getUserBalance(context: Context): Double {
        return getUserSaldo(context)
    }

    /**
     * ✅ REQUERIMIENTO 3: Actualiza el saldo del usuario localmente
     * Se usa cuando se hace checkout offline para descontar el saldo
     */
    fun updateBalance(context: Context, newBalance: Double) {
        getPreferences(context).edit().putFloat(KEY_USER_SALDO, newBalance.toFloat()).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }

    fun clearSession(context: Context) {
        getPreferences(context).edit().clear().apply()
    }

    // Funciones para modo nocturno
    fun saveNightMode(context: Context, isNightMode: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_NIGHT_MODE, isNightMode).apply()
    }

    fun getNightMode(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NIGHT_MODE, false)
    }

    // Función para limpiar la imagen de perfil al hacer logout
    fun clearProfileImage(context: Context) {
        // Limpiar la ruta de la imagen en SharedPreferences del perfil
        val profilePrefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val imagePath = profilePrefs.getString("profile_image_path", null)

        // Si existe una imagen guardada, eliminar el archivo físico
        if (imagePath != null) {
            val imageFile = java.io.File(imagePath)
            if (imageFile.exists()) {
                imageFile.delete()
            }
        }

        // Limpiar la referencia en SharedPreferences
        profilePrefs.edit().clear().apply()
    }
}
