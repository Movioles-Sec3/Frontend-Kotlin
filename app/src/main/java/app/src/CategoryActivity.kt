package app.src

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() { // Extiende AppCompatActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category) // Establece el layout que crearemos
    }
}