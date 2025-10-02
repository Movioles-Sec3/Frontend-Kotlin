package app.src

class AuthRepository {
    // Aquí luego conectas Retrofit/Firebase/etc.
    fun login(username: String, password: String): Boolean {
        // Regla simple temporal: user no vacío y password == "123456" o >= 6 caracteres con un número
        if (username.isBlank()) return false
        if (password == "123456") return true
        return password.any { it.isDigit() } && password.length >= 6
    }
}
