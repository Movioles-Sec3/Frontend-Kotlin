package app.src

import android.app.Application
import androidx.lifecycle.*
import app.src.data.models.Producto
import app.src.data.models.TipoProducto
import app.src.data.repositories.ProductoRepository
import app.src.data.repositories.Result
import kotlinx.coroutines.launch
import app.src.data.local.CatalogCacheManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ProductoUiState {
    object Idle : ProductoUiState()
    object Loading : ProductoUiState()
    data class Success(val productos: List<Producto>) : ProductoUiState()
    data class Error(val message: String) : ProductoUiState()
}

class ProductoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProductoRepository()
    private val catalogCache = CatalogCacheManager(application)

    private val _uiState = MutableLiveData<ProductoUiState>(ProductoUiState.Idle)
    val uiState: LiveData<ProductoUiState> = _uiState

    private val _categorias = MutableLiveData<List<TipoProducto>>()
    val categorias: LiveData<List<TipoProducto>> = _categorias

    companion object {
        private const val TAG = "ProductoViewModel"
    }

    fun cargarProductos(idTipo: Int? = null) {
        viewModelScope.launch {
            _uiState.value = ProductoUiState.Loading

            try {
                // 1) Intentar cargar desde caché primero
                val cacheKey = if (idTipo != null) {
                    CatalogCacheManager.keyCategoryProducts(idTipo, 1)
                } else {
                    CatalogCacheManager.KEY_HOME_RECOMMENDED
                }

                val cachedData = withContext(Dispatchers.IO) {
                    try {
                        catalogCache.getFromCache(cacheKey, Array<Producto>::class.java)?.toList()
                    } catch (e: Exception) {
                        null
                    }
                }

                if (cachedData != null && cachedData.isNotEmpty()) {
                    Log.d(TAG, "✅ Cargando productos desde CACHÉ: ${cachedData.size} productos")
                    _uiState.value = ProductoUiState.Success(cachedData)
                }

                // 2) Actualizar desde red en background
                actualizarProductosDesdeRed(idTipo, cacheKey, cachedData == null)

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar productos: ${e.message}")
                if (_uiState.value !is ProductoUiState.Success) {
                    _uiState.value = ProductoUiState.Error("Sin conexión. Verifica tu red WiFi.")
                }
            }
        }
    }

    private suspend fun actualizarProductosDesdeRed(idTipo: Int?, cacheKey: String, showErrorIfFails: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                when (val result = repo.listarProductos(getApplication(), idTipo, true)) {
                    is Result.Success -> {
                        // Guardar en caché
                        val ttl = if (idTipo != null) {
                            CatalogCacheManager.TTL_CATEGORY_PRODUCTS
                        } else {
                            CatalogCacheManager.TTL_RECOMMENDED
                        }
                        catalogCache.saveToCache(cacheKey, result.data, ttl)

                        // Actualizar UI
                        withContext(Dispatchers.Main) {
                            _uiState.value = ProductoUiState.Success(result.data)
                        }

                        Log.d(TAG, "✅ Productos actualizados desde RED y guardados en caché")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Error al actualizar productos desde red: ${result.message}")
                        if (showErrorIfFails) {
                            withContext(Dispatchers.Main) {
                                _uiState.value = ProductoUiState.Error(result.message)
                            }
                        }
                    }
                    else -> {
                        Log.w(TAG, "Resultado desconocido al actualizar productos")
                        if (showErrorIfFails) {
                            withContext(Dispatchers.Main) {
                                _uiState.value = ProductoUiState.Error("Error desconocido")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Excepción al actualizar productos: ${e.message}")
                if (showErrorIfFails) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = ProductoUiState.Error("Sin conexión a internet")
                    }
                }
            }
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            when (val result = repo.listarTipos(getApplication())) {
                is Result.Success -> {
                    _categorias.value = result.data
                }
                is Result.Error -> {
                    // Manejo silencioso o mostrar error
                }
                else -> {}
            }
        }
    }
}
