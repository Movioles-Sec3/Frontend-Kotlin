package app.src

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import app.src.utils.AnalyticsLogger

class MainActivity : BaseActivity() {

    // Variable para medir tiempo completo de app launch
    private var appLaunchStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iniciar timer de app launch
        appLaunchStartTime = System.currentTimeMillis()

        // TEMPORAL: Para poder lanzar la HomeActivity y probarla
        // Crea un botón programáticamente para lanzar HomeActivity
        val button = Button(this)
        button.text = "Ir a Home"
        button.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            // Pasar tiempo de inicio para medición completa
            intent.putExtra("app_launch_start_time", appLaunchStartTime)
            startActivity(intent)
        }
        setContentView(button) // Establecemos el botón como la vista principal por ahora
    }
}