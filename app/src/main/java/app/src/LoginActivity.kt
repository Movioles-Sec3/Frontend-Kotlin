package app.src

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.setOnClickListener {
            // Por ahora solo navegamos a Home sin validación
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Cerramos el login para que no se pueda volver con back
        }
    }
}
