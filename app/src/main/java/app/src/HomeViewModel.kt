package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.Producto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import app.src.utils.ImagePreloader
import app.src.utils.PerformanceMetrics
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.src.data.local.CatalogCacheManager
import app.src.data.local.GuavaCache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    /** Cache manager para almacenamiento local */
    private val catalogCache = CatalogCacheManager(application)
    private val gson = Gson()

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
     * ✅ REQUERIMIENTO 1 & 2: Estrategia de caché multicapa + Multithreading
     * 
     * Capas de caché:
     * 1. Guava Cache (memoria, más rápido, TTL 5 min) - NUEVO
     * 2. Room Database (disco, persistente)
     * 3. Red (API)
     * 
     * Dispatchers utilizados:
     * - Dispatchers.Default: Cálculos pesados (parsing, transformaciones)
     * - Dispatchers.IO: Operaciones de red y BD
     * - Dispatchers.Unconfined: Lecturas de caché muy rápidas
     * - Dispatchers.Main: Actualización de UI
     */
    fun cargarProductosRecomendados() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val startTime = System.currentTimeMillis()

            try {
                // ✅ NIVEL 1: Guava Cache (más rápido, en memoria)
                val guavaData = withContext(Dispatchers.Unconfined) {
                    // Unconfined para lecturas ultra rápidas sin cambio de thread
                    GuavaCache.getRecommended<List<Producto>>("home:recommended:v1")
                }

                if (guavaData != null && guavaData.isNotEmpty()) {
                    Log.d(TAG, "⚡ Cargando desde GUAVA CACHE: ${guavaData.size} productos")
                    val productosLimitados = guavaData.take(5)
                    _productosRecomendados.value = productosLimitados
                    _uiState.value = HomeUiState.Success(productosLimitados)

                    // Precargar imágenes en paralelo usando IO
                    withContext(Dispatchers.IO) {
                        val imageUrls = productosLimitados.mapNotNull { it.imagenUrl }
                        if (imageUrls.isNotEmpty()) {
                            ImagePreloader.preloadImagesParallel(imageUrls)
                        }
                    }

                    // Actualizar desde red en background sin bloquear
                    actualizarDesdeRed(startTime, false)
                    return@launch
                }

                // ✅ NIVEL 2: Room Database (persistente)
                val roomData = withContext(Dispatchers.IO) {
                    val cacheKey = CatalogCacheManager.KEY_HOME_RECOMMENDED
                    try {
                        catalogCache.getFromCache(cacheKey, Array<Producto>::class.java)?.toList()
                    } catch (e: Exception) {
                        null
                    }
                }

                if (roomData != null && roomData.isNotEmpty()) {
                    Log.d(TAG, "💾 Cargando desde ROOM DATABASE: ${roomData.size} productos")

                    // ✅ USAR Dispatchers.Default para procesamiento pesado de datos
                    val productosLimitados = withContext(Dispatchers.Default) {
                        // Filtrado y transformación en thread de CPU
                        roomData.take(5)
                    }

                    _productosRecomendados.value = productosLimitados
                    _uiState.value = HomeUiState.Success(productosLimitados)

                    // Guardar en Guava para próximas lecturas
                    withContext(Dispatchers.Unconfined) {
                        GuavaCache.putRecommended("home:recommended:v1", roomData)
                    }

                    // Precargar imágenes
                    withContext(Dispatchers.IO) {
                        val imageUrls = productosLimitados.mapNotNull { it.imagenUrl }
                        if (imageUrls.isNotEmpty()) {
                            ImagePreloader.preloadImagesParallel(imageUrls)
                        }
                    }

                    // Actualizar desde red en background
                    actualizarDesdeRed(startTime, false)
                    return@launch
                }

                // ✅ NIVEL 3: Red (si no hay caché)
                Log.d(TAG, "🌐 Sin caché disponible, cargando desde RED...")
                actualizarDesdeRed(startTime, true)

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar productos: ${e.message}")
                // Solo mostrar error si no hay datos en caché
                if (_productosRecomendados.value.isNullOrEmpty()) {
                    _uiState.value = HomeUiState.Error("Sin conexión. Verifica tu red WiFi.")
                }
            }
        }
    }

    /**
     * Actualiza datos desde la red y guarda en caché
     */
    private suspend fun actualizarDesdeRed(startTime: Long, showLoadingIfFails: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                if (useParallelLoading) {
                    cargarEnParaleloConCache(startTime)
                } else {
                    cargarEnSecuenciaConCache(startTime)
                }
                useParallelLoading = !useParallelLoading
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error actualizando desde red: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()

                // Si no hay datos en caché, mostrar error con mensaje más descriptivo
                if (showLoadingIfFails && _productosRecomendados.value.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = when {
                            e.message?.contains("failed to connect", ignoreCase = true) == true ->
                                "Servidor no disponible. Verifica que el backend esté funcionando en 192.168.0.9:8080"
                            e.message?.contains("timeout", ignoreCase = true) == true ->
                                "Tiempo de espera agotado. El servidor tardó mucho en responder."
                            else ->
                                "Error al cargar productos: ${e.message}"
                        }
                        _uiState.value = HomeUiState.Error(errorMessage)
                    }
                } else {
                    Log.d(TAG, "⚠️ Error de red ignorado porque hay datos en caché disponibles")
                }
            }
        }
    }

    /**
     * Carga paralela: Catálogo e imágenes simultáneamente + guardar en caché multicapa
     * ✅ REQUERIMIENTO 2: Usa Dispatchers.IO para red, Default para procesamiento
     */
    private suspend fun cargarEnParaleloConCache(startTime: Long) {
        Log.d(TAG, "🚀 Iniciando carga PARALELA con múltiples dispatchers")

        val catalogStartTime = System.currentTimeMillis()

        // 1) Launch catalog fetch in background IO thread
        val catalogDeferred = viewModelScope.async(Dispatchers.IO) {
            productoRepository.obtenerProductosRecomendados(getApplication<Application>())
        }

        // 2) Wait for catalog (back on main after await)
        when (val result = catalogDeferred.await()) {
            is Result.Success -> {
                val catalogLoadTime = System.currentTimeMillis() - catalogStartTime

                // ✅ USAR Dispatchers.Default para procesamiento de datos
                val productosLimitados = withContext(Dispatchers.Default) {
                    Log.d(TAG, "🧮 Procesando ${result.data.size} productos en Dispatchers.Default")
                    result.data.take(5)
                }

                // ✅ Guardar en AMBOS cachés en paralelo usando async
                val saveGuavaJob = viewModelScope.async(Dispatchers.Unconfined) {
                    GuavaCache.putRecommended("home:recommended:v1", result.data)
                    Log.d(TAG, "💾 Guardado en Guava Cache")
                }

                val saveRoomJob = viewModelScope.async(Dispatchers.IO) {
                    val cacheKey = CatalogCacheManager.KEY_HOME_RECOMMENDED
                    catalogCache.saveToCache(
                        cacheKey,
                        result.data,
                        CatalogCacheManager.TTL_RECOMMENDED
                    )
                    Log.d(TAG, "💾 Guardado en Room Database")
                }

                // Preparar URLs de imágenes en Default dispatcher
                val imageUrls = withContext(Dispatchers.Default) {
                    productosLimitados.mapNotNull { it.imagenUrl }
                }

                val imageStartTime = System.currentTimeMillis()
                val imagesDeferred = viewModelScope.async(Dispatchers.IO) {
                    if (imageUrls.isNotEmpty()) {
                        ImagePreloader.preloadImagesParallel(imageUrls)
                    } else {
                        0L
                    }
                }

                // Actualizar UI en Main
                withContext(Dispatchers.Main) {
                    _productosRecomendados.value = productosLimitados
                    _uiState.value = HomeUiState.Success(productosLimitados)
                }

                // Esperar a que terminen todas las tareas en paralelo
                saveGuavaJob.await()
                saveRoomJob.await()
                val imagesLoadTime = imagesDeferred.await()

                val totalTime = System.currentTimeMillis() - startTime
                val menuReadyTime = totalTime

                registrarMetricas(
                    loadType = PerformanceMetrics.LoadType.PARALLEL,
                    catalogLoadTime = catalogLoadTime,
                    imagesLoadTime = imagesLoadTime,
                    totalTime = totalTime,
                    menuReadyTime = menuReadyTime,
                    productCount = productosLimitados.size
                )

                Log.d(TAG, "✅ Carga PARALELA completada (IO + Default + Unconfined dispatchers)")
                GuavaCache.logAllStats() // Mostrar estadísticas
            }
            is Result.Error -> {
                Log.w(TAG, "Error al actualizar desde red: ${result.message}")
            }
            else -> {
                Log.w(TAG, "Resultado desconocido al actualizar desde red")
            }
        }
    }

    /**
     * Carga secuencial: Catálogo primero, luego imágenes + guardar en caché multicapa
     * ✅ REQUERIMIENTO 2: Usa Dispatchers.IO, Default y Unconfined
     */
    private suspend fun cargarEnSecuenciaConCache(startTime: Long) {
        Log.d(TAG, "📦 Iniciando carga SECUENCIAL con múltiples dispatchers")

        val catalogStartTime = System.currentTimeMillis()

        // 1) fetch catalog on IO
        val result = withContext(Dispatchers.IO) {
            productoRepository.obtenerProductosRecomendados(getApplication<Application>())
        }

        when (result) {
            is Result.Success -> {
                val catalogLoadTime = System.currentTimeMillis() - catalogStartTime

                // ✅ Procesamiento en Dispatchers.Default
                val productosLimitados = withContext(Dispatchers.Default) {
                    Log.d(TAG, "🧮 Procesando ${result.data.size} productos en Dispatchers.Default")
                    result.data.take(5)
                }

                // ✅ Guardar en Guava (rápido, Unconfined)
                withContext(Dispatchers.Unconfined) {
                    GuavaCache.putRecommended("home:recommended:v1", result.data)
                    Log.d(TAG, "💾 Guardado en Guava Cache")
                }

                // ✅ Guardar en Room (IO)
                withContext(Dispatchers.IO) {
                    val cacheKey = CatalogCacheManager.KEY_HOME_RECOMMENDED
                    catalogCache.saveToCache(
                        cacheKey,
                        result.data,
                        CatalogCacheManager.TTL_RECOMMENDED
                    )
                    Log.d(TAG, "💾 Guardado en Room Database")
                }

                // Actualizar UI
                withContext(Dispatchers.Main) {
                    _productosRecomendados.value = productosLimitados
                    _uiState.value = HomeUiState.Success(productosLimitados)
                }

                // Precargar imágenes
                val imageStartTime = System.currentTimeMillis()
                val imageUrls = withContext(Dispatchers.Default) {
                    productosLimitados.mapNotNull { it.imagenUrl }
                }

                val imagesLoadTime = if (imageUrls.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        ImagePreloader.preloadImagesParallel(imageUrls)
                    }
                } else {
                    0L
                }

                val totalTime = System.currentTimeMillis() - startTime
                val menuReadyTime = totalTime

                registrarMetricas(
                    loadType = PerformanceMetrics.LoadType.SEQUENTIAL,
                    catalogLoadTime = catalogLoadTime,
                    imagesLoadTime = imagesLoadTime,
                    totalTime = totalTime,
                    menuReadyTime = menuReadyTime,
                    productCount = productosLimitados.size
                )

                Log.d(TAG, "✅ Carga SECUENCIAL completada (IO + Default + Unconfined dispatchers)")
                GuavaCache.logAllStats() // Mostrar estadísticas
            }
            is Result.Error -> {
                Log.w(TAG, "Error al actualizar desde red: ${result.message}")
            }
            else -> {
                Log.w(TAG, "Resultado desconocido al actualizar desde red")
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