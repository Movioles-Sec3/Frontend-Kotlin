package app.src

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aquí puedes establecer un layout simple para esta MainActivity
        // Por ahora, para que compile, no vamos a establecer un layout específico.
        // Más adelante, crearemos un layout para ella.
        // setContentView(R.layout.activity_main) // Esto lo haremos en el siguiente paso

        // TEMPORAL: Para poder lanzar la CategoryActivity y probarla
        // Crea un botón programáticamente para lanzar CategoryActivity
        val button = Button(this)
        button.text = "Ir a Categorías"
        button.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java)
            startActivity(intent)
        }
        setContentView(button) // Establecemos el botón como la vista principal por ahora
    }
}