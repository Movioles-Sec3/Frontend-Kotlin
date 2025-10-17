package app.src

import android.app.Application
import android.os.Build
import androidx.lifecycle.*
import app.src.data.models.Producto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import app.src.utils.ImagePreloader
import app.src.utils.PerformanceMetrics
import app.src.utils.NetworkUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import android.util.Log

/**
 * Represents the UI contract for the Home screen.
 *
 * States:
 * - [Loading]: Data is being fetched and the UI should show a progress indicator.
 * - [Success]: Data fetched successfully; contains the list of recommended products.
 * - [Error]: A user-facing error occurred; contains a message suitable for display.
 */
sealed class HomeUiState {
    /** Emitted while recommended products are loading. */
    object Loading : HomeUiState()

    /**
     * Emitted when recommended products have been loaded successfully.
     *
     * @property productosRecomendados The list of products to render in the UI.
     */
    data class Success(val productosRecomendados: List<Producto>) : HomeUiState()

    /**
     * Emitted when there is an error loading data.
     *
     * @property message Human-readable message that can be shown to the user.
     */
    data class Error(val message: String) : HomeUiState()
}

/**
 * ViewModel for the Home screen.
 *
 * Responsibilities:
 * - Loads and exposes recommended products via [LiveData].
 * - Exposes a high-level UI state ([HomeUiState]) to simplify rendering logic.
 * - Handles user interactions originating from recommended products (e.g., item clicks).
 *
 * Lifecycle:
 * - Triggers an initial load of recommended products in [init].
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository used to retrieve product data. */
    private val productoRepository = ProductoRepository()

    /**
     * Backing field for the UI state.
     * Use [uiState] to observe state changes from the UI layer.
     */
    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    /**
     * Backing field for the list of recommended products.
     * Use [productosRecomendados] to observe data changes from the UI layer.
     */
    private val _productosRecomendados = MutableLiveData<List<Producto>>()
    val productosRecomendados: LiveData<List<Producto>> = _productosRecomendados

    companion object {
        private const val TAG = "HomeViewModel"

        // Flag para alternar entre carga paralela y secuencial
        // En producción, esto se puede controlar con A/B testing o configuración remota
        var useParallelLoading = true
    }

    /**
     * Initializes the ViewModel by starting the initial data load.
     */
    init {
        cargarProductosRecomendados()
    }

    /**
     * Loads recommended products and updates both [productosRecomendados] and [uiState].
     *
     * Flow:
     * 1) Emits [HomeUiState.Loading].
     * 2) Requests data from [ProductoRepository].
     * 3) On success:
     *    - Updates [_productosRecomendados] with the fetched list.
     *    - Emits [HomeUiState.Success] with the same list.
     * 4) On failure:
     *    - Emits [HomeUiState.Error] with a user-facing message.
     * 5) Any unexpected result falls back to a generic error message.
     */
    fun cargarProductosRecomendados() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            // Tiempos de medición
            val startTime = System.currentTimeMillis()
            var catalogLoadTime = 0L
            var imagesLoadTime = 0L

            try {
                if (useParallelLoading) {
                    // CARGA PARALELA: Catálogo e imágenes simultáneamente
                    cargarEnParalelo(startTime)
                } else {
                    // CARGA SECUENCIAL: Catálogo primero, luego imágenes
                    cargarEnSecuencia(startTime)
                }

                // Alternar método para la próxima carga (para comparar)
                useParallelLoading = !useParallelLoading

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar productos: ${e.message}")
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Carga paralela: Catálogo e imágenes simultáneamente
     */
    private suspend fun cargarEnParalelo(startTime: Long) {
        Log.d(TAG, "🚀 Iniciando carga PARALELA")

        val catalogStartTime = System.currentTimeMillis()

        // Lanzar ambas operaciones en paralelo
        val catalogDeferred = viewModelScope.async {
            productoRepository.obtenerProductosRecomendados(getApplication<Application>())
        }

        // Esperar resultado del catálogo
        when (val result = catalogDeferred.await()) {
            is Result.Success -> {
                val catalogLoadTime = System.currentTimeMillis() - catalogStartTime
                val productosLimitados = result.data.take(5)

                // Iniciar carga de imágenes en paralelo
                val imageStartTime = System.currentTimeMillis()
                val imageUrls = productosLimitados.mapNotNull { it.imagenUrl }
                val imagesLoadTime = if (imageUrls.isNotEmpty()) {
                    ImagePreloader.preloadImagesParallel(imageUrls)
                } else {
                    0L
                }

                val totalTime = System.currentTimeMillis() - startTime
                val menuReadyTime = totalTime // El menú está listo cuando todo está cargado

                // Actualizar UI
                _productosRecomendados.value = productosLimitados
                _uiState.value = HomeUiState.Success(productosLimitados)

                // Registrar métricas
                registrarMetricas(
                    loadType = PerformanceMetrics.LoadType.PARALLEL,
                    catalogLoadTime = catalogLoadTime,
                    imagesLoadTime = imagesLoadTime,
                    totalTime = totalTime,
                    menuReadyTime = menuReadyTime,
                    productCount = productosLimitados.size
                )

                Log.d(TAG, """
                    ✅ Carga PARALELA completada:
                    - Catálogo: ${catalogLoadTime}ms
                    - Imágenes: ${imagesLoadTime}ms
                    - Total: ${totalTime}ms
                """.trimIndent())
            }
            is Result.Error -> {
                _uiState.value = HomeUiState.Error(result.message)
            }
            else -> {
                _uiState.value = HomeUiState.Error("Error desconocido")
            }
        }
    }

    /**
     * Carga secuencial: Catálogo primero, luego imágenes
     */
    private suspend fun cargarEnSecuencia(startTime: Long) {
        Log.d(TAG, "📦 Iniciando carga SECUENCIAL")

        // 1. Cargar catálogo primero
        val catalogStartTime = System.currentTimeMillis()
        when (val result = productoRepository.obtenerProductosRecomendados(getApplication<Application>())) {
            is Result.Success -> {
                val catalogLoadTime = System.currentTimeMillis() - catalogStartTime
                val productosLimitados = result.data.take(5)

                // Actualizar UI con productos (menú ya es usable)
                _productosRecomendados.value = productosLimitados
                _uiState.value = HomeUiState.Success(productosLimitados)

                // 2. Luego cargar imágenes
                val imageStartTime = System.currentTimeMillis()
                val imageUrls = productosLimitados.mapNotNull { it.imagenUrl }
                val imagesLoadTime = if (imageUrls.isNotEmpty()) {
                    ImagePreloader.preloadImagesSequential(imageUrls)
                } else {
                    0L
                }

                val totalTime = System.currentTimeMillis() - startTime
                val menuReadyTime = catalogLoadTime // El menú está listo después del catálogo

                // Registrar métricas
                registrarMetricas(
                    loadType = PerformanceMetrics.LoadType.SEQUENTIAL,
                    catalogLoadTime = catalogLoadTime,
                    imagesLoadTime = imagesLoadTime,
                    totalTime = totalTime,
                    menuReadyTime = menuReadyTime,
                    productCount = productosLimitados.size
                )

                Log.d(TAG, """
                    ✅ Carga SECUENCIAL completada:
                    - Catálogo: ${catalogLoadTime}ms
                    - Imágenes: ${imagesLoadTime}ms
                    - Total: ${totalTime}ms
                    - Menú listo en: ${menuReadyTime}ms
                """.trimIndent())
            }
            is Result.Error -> {
                _uiState.value = HomeUiState.Error(result.message)
            }
            else -> {
                _uiState.value = HomeUiState.Error("Error desconocido")
            }
        }
    }

    /**
     * Registra métricas de rendimiento
     */
    private suspend fun registrarMetricas(
        loadType: PerformanceMetrics.LoadType,
        catalogLoadTime: Long,
        imagesLoadTime: Long,
        totalTime: Long,
        menuReadyTime: Long,
        productCount: Int
    ) {
        val context = getApplication<Application>()
        val networkType = getNetworkType()
        val deviceTier = getDeviceTier()

        PerformanceMetrics.recordMeasurement(
            context = context,
            loadType = loadType,
            catalogLoadTime = catalogLoadTime,
            imagesLoadTime = imagesLoadTime,
            totalTime = totalTime,
            menuReadyTime = menuReadyTime,
            productCount = productCount,
            networkType = networkType,
            deviceTier = deviceTier
        )
    }

    /**
     * Obtiene el tipo de red actual
     */
    private fun getNetworkType(): String {
        val context = getApplication<Application>()
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return "Offline"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Unknown"
        }
    }

    /**
     * Obtiene el tier del dispositivo
     */
    private fun getDeviceTier(): String {
        val ram = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        return when {
            ram < 2000 -> "low"
            ram < 4000 -> "mid"
            else -> "high"
        }
    }

    /**
     * Handles clicks on a recommended product.
     *
     * Extend this to:
     * - Send analytics events.
     * - Navigate to a product detail screen.
     * - Preload images or additional data for a smoother transition.
     *
     * @param producto The product that was clicked.
     */
    fun onProductoRecomendadoClick(producto: Producto) {
        // Aquí podrías agregar lógica adicional si es necesario
        // Por ejemplo, analytics, tracking, etc.
    }
}