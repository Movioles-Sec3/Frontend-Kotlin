package app.src

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.src.utils.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileBalance: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnCamera: FloatingActionButton

    private var currentPhotoPath: String? = null

    // Launcher para solicitar permiso de cámara
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara para tomar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para tomar la foto
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentPhotoPath != null) {
            loadProfileImage()
            saveProfileImagePath(currentPhotoPath!!)
            Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        loadUserData()
        loadProfileImage()
        setupListeners()
    }

    private fun initializeViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileBalance = findViewById(R.id.tvProfileBalance)
        btnBack = findViewById(R.id.btnBack)
        btnCamera = findViewById(R.id.btnCamera)
    }

    private fun setupListeners() {
        // Botón para volver al menú principal
        btnBack.setOnClickListener {
            finish() // Cierra la actividad y vuelve a HomeActivity
        }

        // Botón de cámara
        btnCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        // Long press en botón de cámara para abrir inspector de almacenamiento (debug)
        btnCamera.setOnLongClickListener {
            val intent = Intent(this, LocalStorageDebugActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "🔍 Abriendo inspector de almacenamiento", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loadUserData() {
        // Obtener datos del usuario desde SessionManager
        val userName = SessionManager.getUserName(this) ?: "Usuario"
        val userEmail = SessionManager.getUserEmail(this) ?: "correo@ejemplo.com"
        val userBalance = SessionManager.getUserSaldo(this)

        // Mostrar los datos en la UI
        tvProfileName.text = userName
        tvProfileEmail.text = userEmail
        tvProfileBalance.text = String.format(Locale.US, "Balance: $%.2f", userBalance)
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya otorgado, abrir cámara
                openCamera()
            }
            else -> {
                // Solicitar permiso
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        // Crear archivo temporal para la foto
        val photoFile = createImageFile()

        if (photoFile != null) {
            currentPhotoPath = photoFile.absolutePath

            val photoUri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            takePictureLauncher.launch(photoUri)
        } else {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val storageDir = getExternalFilesDir(null)
            File(storageDir, "profile_image.jpg")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveProfileImagePath(path: String) {
        val sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
        sharedPreferences.edit().putString("profile_image_path", path).apply()
    }

    private fun getProfileImagePath(): String? {
        val sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
        return sharedPreferences.getString("profile_image_path", null)
    }

    private fun loadProfileImage() {
        val imagePath = currentPhotoPath ?: getProfileImagePath()

        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    ivProfileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Si hay error, mantener la imagen por defecto
                }
            }
        }
    }
}
