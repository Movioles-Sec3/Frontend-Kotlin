package app.src

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import app.src.data.local.entities.CalificacionEntity
import com.google.android.material.slider.Slider

/**
 * Activity para calificar una orden
 * Permite asignar rating (1-10) y comentario
 * Usa multithreading para operaciones de BD y cache
 */
class CalificarOrdenActivity : BaseActivity() {

    private val viewModel: CalificacionViewModel by viewModels()

    private lateinit var tvOrderInfo: TextView
    private lateinit var sliderRating: Slider
    private lateinit var tvRatingValue: TextView
    private lateinit var etComentario: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutForm: LinearLayout
    private lateinit var tvLoadingMessage: TextView
    
    private var orderId: Int = -1
    private var orderTotal: Double = 0.0
    private var isEditMode = false

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_ORDER_TOTAL = "order_total"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificar_orden)

        // Obtener datos de la orden
        orderId = intent.getIntExtra(EXTRA_ORDER_ID, -1)
        orderTotal = intent.getDoubleExtra(EXTRA_ORDER_TOTAL, 0.0)

        if (orderId == -1) {
            Toast.makeText(this, "Error: Orden no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        observeViewModel()

        // Cargar calificación existente si hay
        viewModel.loadCalificacion(orderId)
    }

    private fun initViews() {
        tvOrderInfo = findViewById(R.id.tv_order_info)
        sliderRating = findViewById(R.id.slider_rating)
        tvRatingValue = findViewById(R.id.tv_rating_value)
        etComentario = findViewById(R.id.et_comentario)
        btnGuardar = findViewById(R.id.btn_guardar)
        btnCancelar = findViewById(R.id.btn_cancelar)
        progressBar = findViewById(R.id.progress_bar)
        layoutForm = findViewById(R.id.layout_form)
        tvLoadingMessage = findViewById(R.id.tv_loading_message)

        // Configurar información de la orden
        tvOrderInfo.text = "Calificar Orden #$orderId - Total: $${"%.2f".format(orderTotal)}"

        // Configurar slider
        sliderRating.valueFrom = 1f
        sliderRating.valueTo = 10f
        sliderRating.stepSize = 1f
        sliderRating.value = 5f
    }

    private fun setupListeners() {
        // Actualizar texto del rating mientras se mueve el slider
        sliderRating.addOnChangeListener { _, value, _ ->
            updateRatingText(value.toInt())
        }

        // Validar comentario en tiempo real
        etComentario.addTextChangedListener {
            validateForm()
        }

        // Botón guardar
        btnGuardar.setOnClickListener {
            saveCalificacion()
        }

        // Botón cancelar
        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        // Observar estado de UI
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CalificacionUiState.Loading -> {
                    showLoading()
                }
                is CalificacionUiState.Empty -> {
                    showForm(null)
                    isEditMode = false
                }
                is CalificacionUiState.Loaded -> {
                    showForm(state.calificacion)
                    isEditMode = true
                }
                is CalificacionUiState.Error -> {
                    showError(state.message)
                }
            }
        }

        // Observar estado de guardado
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is SaveState.Saving -> {
                    btnGuardar.isEnabled = false
                    btnGuardar.text = "Guardando..."
                    progressBar.visibility = View.VISIBLE
                }
                is SaveState.Success -> {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar"
                    progressBar.visibility = View.GONE
                    
                    Toast.makeText(
                        this,
                        "✅ Calificación guardada exitosamente",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Mostrar estadísticas de cache
                    viewModel.logCacheStats()
                    
                    finish()
                }
                is SaveState.Error -> {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar"
                    progressBar.visibility = View.GONE
                    
                    Toast.makeText(
                        this,
                        "❌ Error: ${state.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        layoutForm.visibility = View.GONE
        tvLoadingMessage.visibility = View.VISIBLE
        tvLoadingMessage.text = "Cargando calificación..."
    }

    private fun showForm(calificacion: CalificacionEntity?) {
        progressBar.visibility = View.GONE
        layoutForm.visibility = View.VISIBLE
        tvLoadingMessage.visibility = View.GONE

        if (calificacion != null) {
            // Modo edición - cargar datos existentes
            sliderRating.value = calificacion.calificacion.toFloat()
            etComentario.setText(calificacion.comentario)
            btnGuardar.text = "Actualizar Calificación"
            
            Toast.makeText(
                this,
                "📝 Editando calificación existente",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Modo nuevo
            sliderRating.value = 5f
            etComentario.setText("")
            btnGuardar.text = "Guardar Calificación"
        }
        
        updateRatingText(sliderRating.value.toInt())
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        layoutForm.visibility = View.VISIBLE
        tvLoadingMessage.visibility = View.GONE
        
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }

    private fun updateRatingText(rating: Int) {
        val emoji = when (rating) {
            in 1..2 -> "😡"
            in 3..4 -> "😞"
            in 5..6 -> "😐"
            in 7..8 -> "😊"
            else -> "😍"
        }
        tvRatingValue.text = "$rating/10 $emoji"
    }

    private fun validateForm(): Boolean {
        val comentario = etComentario.text.toString().trim()
        val isValid = comentario.isNotBlank() && comentario.length >= 10
        
        btnGuardar.isEnabled = isValid
        
        if (comentario.isNotEmpty() && comentario.length < 10) {
            etComentario.error = "El comentario debe tener al menos 10 caracteres"
        }
        
        return isValid
    }

    private fun saveCalificacion() {
        if (!validateForm()) {
            Toast.makeText(
                this,
                "Por favor completa el formulario correctamente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rating = sliderRating.value.toInt()
        val comentario = etComentario.text.toString().trim()

        if (isEditMode) {
            viewModel.updateCalificacion(orderId, rating, comentario)
        } else {
            viewModel.saveCalificacion(orderId, rating, comentario)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Log final de estadísticas
        viewModel.logCacheStats()
    }
}
