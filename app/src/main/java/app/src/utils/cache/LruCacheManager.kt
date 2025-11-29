package app.src.utils.cache

import android.content.Context
import android.util.LruCache
import android.util.Log
import app.src.data.models.Producto
import app.src.data.models.ProductoConConversiones
import app.src.data.models.TipoProducto
import app.src.data.models.Compra
import com.google.gson.Gson

/**
 * ============================================================================
 * LRU CACHE MANAGER - Sistema de Caché en Memoria con Algoritmo LRU
 * ============================================================================
 *
 * ESTRUCTURA DEL LRU CACHE:
 * -------------------------
 * El LRU (Least Recently Used) Cache es una estructura de datos que mantiene
 * un número limitado de elementos en memoria y automáticamente elimina el
 * elemento menos recientemente usado cuando se alcanza la capacidad máxima.
 *
 * IMPLEMENTACIÓN INTERNA (android.util.LruCache):
 * ------------------------------------------------
 * - Internamente usa un LinkedHashMap<K, V> con accessOrder=true
 * - Cada vez que se accede a un elemento (get), se mueve al final de la lista
 * - Los elementos al inicio son los "menos recientemente usados"
 * - Cuando se necesita espacio, se eliminan elementos del inicio (LRU)
 * - Es thread-safe: todos los métodos están sincronizados
 *
 * VENTAJAS DEL LRU vs OTRAS ESTRATEGIAS:
 * ---------------------------------------
 * 1. LRU vs FIFO: Mantiene datos frecuentes en cache (FIFO no considera uso)
 * 2. LRU vs LFU: Más simple y se adapta mejor a cambios de patrón de acceso
 * 3. LRU vs Random: Predecible y con mejor hit rate en patrones típicos
 *
 * PARÁMETROS DE CONFIGURACIÓN:
 * ----------------------------
 *
 * 1. MEMORY_CACHE_PERCENTAGE = 0.125 (12.5%)
 *    ¿Por qué 12.5%?
 *    - Android recomienda entre 1/8 y 1/4 de la memoria heap disponible
 *    - 12.5% (1/8) es conservador para evitar OutOfMemoryError
 *    - Suficiente para cachear ~50-200 productos según el dispositivo
 *    - Deja memoria para otras operaciones de la app (imágenes, UI, etc.)
 *
 * 2. MIN_CACHE_SIZE = 4 MB
 *    ¿Por qué 4MB mínimo?
 *    - Garantiza funcionamiento en dispositivos low-end (512MB RAM)
 *    - Permite cachear al menos ~20-30 productos con conversiones
 *    - Balance entre utilidad y compatibilidad con dispositivos antiguos
 *
 * 3. MAX_CACHE_SIZE = 32 MB
 *    ¿Por qué 32MB máximo?
 *    - Previene uso excesivo en dispositivos high-end (8GB+ RAM)
 *    - 32MB puede almacenar ~500-1000 productos
 *    - Evita que el cache consuma recursos innecesarios
 *
 * 4. sizeOf(key, value)
 *    ¿Cómo se calcula el tamaño?
 *    - Serializa el objeto a JSON y cuenta los bytes
 *    - Incluye overhead del wrapper CacheEntry (timestamp, etc.)
 *    - Convierte a KB para el LruCache
 *    - Justificación: Cálculo preciso del uso de memoria real
 *
 * 5. entryRemoved(evicted, key, oldValue, newValue)
 *    ¿Para qué sirve este callback?
 *    - evicted=true: El LRU eliminó la entrada por falta de espacio
 *    - evicted=false: Se eliminó manualmente (clear, remove)
 *    - Útil para logging, debugging y análisis de comportamiento
 *    - Permite detectar si el cache es muy pequeño (muchas evictions)
 *
 * CONFIGURACIÓN DE TTL (Time To Live):
 * ------------------------------------
 *
 * 1. Productos: 1 hora (3,600,000 ms)
 *    Justificación:
 *    - Los productos pueden cambiar disponibilidad frecuentemente
 *    - Precio puede actualizarse en horarios pico
 *    - Balance entre freshness y reducción de llamadas API
 *
 * 2. Conversiones de Moneda: 24 horas (86,400,000 ms)
 *    Justificación:
 *    - Tasas de cambio son relativamente estables día a día
 *    - Datos menos críticos para la experiencia del usuario
 *    - Reducción significativa de llamadas API
 *
 * 3. Tipos/Categorías: 6 horas (21,600,000 ms)
 *    Justificación:
 *    - Categorías raramente cambian (estables)
 *    - Datos pequeños, no saturan el cache
 *    - 6 horas cubre horarios de servicio típicos
 *
 * DECISIONES DE IMPLEMENTACIÓN:
 * ------------------------------
 *
 * 1. MÚLTIPLES CACHES SEPARADOS (uno por tipo de dato)
 *    ¿Por qué no un solo cache para todo?
 *    - Evita que productos saturen y eliminen conversiones
 *    - Permite configurar tamaños y TTL específicos por tipo
 *    - Mejora organización y mantenibilidad del código
 *    - Facilita debugging (logs separados por tipo)
 *
 * 2. SINGLETON PATTERN
 *    ¿Por qué singleton?
 *    - Garantiza una única instancia en toda la app
 *    - Evita duplicación de datos en memoria
 *    - Thread-safe con @Volatile y synchronized
 *    - Fácil acceso desde cualquier repository
 *
 * 3. WRAPPER CacheEntry<T>
 *    ¿Por qué no cachear directamente los objetos?
 *    - Permite agregar timestamp sin modificar modelos
 *    - Incluye tamaño en bytes para sizeOf()
 *    - Facilita validación de TTL
 *    - Separación de concerns (modelos limpios)
 *
 * 4. ESTRATEGIA DE INVALIDACIÓN
 *    - Lazy Eviction: Se valida TTL al hacer get()
 *    - Si expiró, se elimina y retorna null (cache miss)
 *    - evictExpired(): Limpieza manual de todos los expirados
 *    - clear(): Limpieza total del cache
 *
 * 5. MÉTRICAS Y MONITORING
 *    ¿Por qué trackear hits/misses?
 *    - Permite evaluar efectividad del cache (hit rate)
 *    - Detecta problemas (hit rate bajo = cache ineficiente)
 *    - Ayuda a optimizar tamaños y TTL
 *    - Útil para debugging y optimización
 *
 * VENTAJAS vs SharedPreferences:
 * -------------------------------
 * ✅ 100x más rápido (RAM vs I/O a disco)
 * ✅ No requiere serialización en cada acceso
 * ✅ Gestión automática de memoria (eviction)
 * ✅ Thread-safe por diseño
 * ✅ Mejor para datos temporales/volátiles
 * ✅ No causa ANR (Application Not Responding)
 *
 * DESVENTAJAS vs SharedPreferences:
 * ----------------------------------
 * ❌ Se pierde al cerrar la app (volátil)
 * ❌ Limitado por memoria RAM disponible
 * ❌ No persiste entre sesiones
 *
 * CUANDO USAR LRU CACHE:
 * ----------------------
 * ✅ Datos consultados frecuentemente en una sesión
 * ✅ Datos que pueden regenerarse (API disponible)
 * ✅ Optimización de performance (reducir latencia)
 * ✅ Reducir carga del servidor (menos llamadas API)
 *
 * ============================================================================
 */
class LruCacheManager private constructor(private val context: Context) {
    // sprint 4
    companion object {
        private const val TAG = "LruCacheManager"

        // =====================================================================
        // CONFIGURACIÓN DE TAMAÑOS DE CACHE
        // =====================================================================

        /**
         * Porcentaje de memoria heap a usar para el cache (12.5% = 1/8)
         * Android recomienda entre 1/8 y 1/4 de la memoria disponible
         */
        private const val MEMORY_CACHE_PERCENTAGE = 0.125

        /**
         * Tamaño mínimo del cache: 4 MB
         * Garantiza funcionamiento en dispositivos low-end
         */
        private const val MIN_CACHE_SIZE = 4 * 1024 * 1024 // 4MB en bytes

        /**
         * Tamaño máximo del cache: 32 MB
         * Previene uso excesivo en dispositivos high-end
         */
        private const val MAX_CACHE_SIZE = 32 * 1024 * 1024 // 32MB en bytes

        // =====================================================================
        // CONFIGURACIÓN DE TTL (Time To Live)
        // =====================================================================

        /**
         * TTL para productos: 1 hora
         * Datos volátiles que pueden cambiar frecuentemente
         */
        private const val PRODUCTOS_TTL = 60 * 60 * 1000L // 1 hora

        /**
         * TTL para conversiones de moneda: 24 horas
         * Tasas de cambio estables día a día
         */
        private const val CONVERSIONES_TTL = 24 * 60 * 60 * 1000L // 24 horas

        /**
         * TTL para tipos/categorías: 6 horas
         * Datos muy estables que raramente cambian
         */
        private const val TIPOS_TTL = 6 * 60 * 60 * 1000L // 6 horas

        // =====================================================================
        // SINGLETON INSTANCE
        // =====================================================================

        @Volatile
        private var INSTANCE: LruCacheManager? = null

        /**
         * Obtiene la instancia única del LruCacheManager (thread-safe)
         */
        fun getInstance(context: Context): LruCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LruCacheManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // =========================================================================
    // GSON para serialización (cálculo de tamaños)
    // =========================================================================
    private val gson = Gson()

    // =========================================================================
    // MÉTRICAS DE RENDIMIENTO
    // =========================================================================
    private var totalHits: Long = 0
    private var totalMisses: Long = 0
    private var totalEvictions: Long = 0

    // =========================================================================
    // CACHES LRU SEPARADOS POR TIPO DE DATO
    // =========================================================================

    /**
     * Cache LRU para listas de productos
     * Key: String generada con filtros (ej: "productos_tipo_1_disponible_true")
     * Value: CacheEntry con List<Producto>
     */
    private val productosCache: LruCache<String, CacheEntry<List<Producto>>>

    /**
     * Cache LRU para conversiones de moneda por producto
     * Key: Int (productoId)
     * Value: CacheEntry con ProductoConConversiones
     */
    private val conversionesCache: LruCache<Int, CacheEntry<ProductoConConversiones>>

    /**
     * Cache LRU para tipos/categorías de productos
     * Key: String (siempre "tipos")
     * Value: CacheEntry con List<TipoProducto>
     */
    private val tiposCache: LruCache<String, CacheEntry<List<TipoProducto>>>

    /**
     * Cache LRU para productos individuales
     * Key: Int (productoId)
     * Value: CacheEntry con Producto
     */
    private val productoIndividualCache: LruCache<Int, CacheEntry<Producto>>

    /**
     * Cache LRU para historial de compras/pedidos
     * Key: String (siempre "historial_compras")
     * Value: CacheEntry con List<Compra>
     */
    private val comprasCache: LruCache<String, CacheEntry<List<Compra>>>

    // =========================================================================
    // INICIALIZACIÓN DE CACHES
    // =========================================================================

    init {
        // Calcular tamaño óptimo del cache basado en memoria disponible
        val maxMemory = Runtime.getRuntime().maxMemory() // bytes
        val calculatedCacheSize = (maxMemory * MEMORY_CACHE_PERCENTAGE).toLong()

        // Aplicar límites MIN y MAX
        val finalCacheSize = calculatedCacheSize.coerceIn(
            MIN_CACHE_SIZE.toLong(),
            MAX_CACHE_SIZE.toLong()
        ).toInt()

        // Convertir a KB para LruCache (trabaja en KB internamente)
        val cacheSizeKB = finalCacheSize / 1024

        Log.d(TAG, "════════════════════════════════════════════════════════")
        Log.d(TAG, "🚀 Inicializando LRU Cache Manager")
        Log.d(TAG, "════════════════════════════════════════════════════════")
        Log.d(TAG, "📱 Max Memory: ${maxMemory / (1024 * 1024)} MB")
        Log.d(TAG, "📦 Cache Size: ${finalCacheSize / (1024 * 1024)} MB (${cacheSizeKB} KB)")
        Log.d(TAG, "⏱️ TTL Productos: ${PRODUCTOS_TTL / (60 * 1000)} minutos")
        Log.d(TAG, "⏱️ TTL Conversiones: ${CONVERSIONES_TTL / (60 * 60 * 1000)} horas")
        Log.d(TAG, "⏱️ TTL Tipos: ${TIPOS_TTL / (60 * 60 * 1000)} horas")
        Log.d(TAG, "════════════════════════════════════════════════════════")

        // Dividir el cache entre los diferentes tipos
        // Productos: 50%, Conversiones: 30%, Individual: 15%, Tipos: 5%
        val productosCacheSize = (cacheSizeKB * 0.50).toInt()
        val conversionesCacheSize = (cacheSizeKB * 0.30).toInt()
        val individualCacheSize = (cacheSizeKB * 0.15).toInt()
        val tiposCacheSize = (cacheSizeKB * 0.05).toInt()

        // =====================================================================
        // CACHE DE PRODUCTOS (Listas)
        // =====================================================================
        productosCache = object : LruCache<String, CacheEntry<List<Producto>>>(productosCacheSize) {
            /**
             * Calcula el tamaño de una entrada en KB
             * Serializa el objeto a JSON y cuenta los bytes
             */
            override fun sizeOf(key: String, value: CacheEntry<List<Producto>>): Int {
                return value.sizeInBytes / 1024 // Convertir bytes a KB
            }

            /**
             * Callback cuando una entrada es eliminada del cache
             * @param evicted true si fue eliminado por LRU policy, false si fue manual
             */
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: CacheEntry<List<Producto>>,
                newValue: CacheEntry<List<Producto>>?
            ) {
                if (evicted) {
                    totalEvictions++
                    val ageMinutes = oldValue.getAge() / (60 * 1000)
                    Log.d(TAG, "🗑️ [PRODUCTOS] Evicted del cache: $key (edad: $ageMinutes min)")
                }
            }
        }

        // =====================================================================
        // CACHE DE CONVERSIONES
        // =====================================================================
        conversionesCache = object : LruCache<Int, CacheEntry<ProductoConConversiones>>(conversionesCacheSize) {
            override fun sizeOf(key: Int, value: CacheEntry<ProductoConConversiones>): Int {
                return value.sizeInBytes / 1024
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: Int,
                oldValue: CacheEntry<ProductoConConversiones>,
                newValue: CacheEntry<ProductoConConversiones>?
            ) {
                if (evicted) {
                    totalEvictions++
                    val ageHours = oldValue.getAge() / (60 * 60 * 1000)
                    Log.d(TAG, "🗑️ [CONVERSIONES] Evicted del cache: producto $key (edad: $ageHours h)")
                }
            }
        }

        // =====================================================================
        // CACHE DE PRODUCTOS INDIVIDUALES
        // =====================================================================
        productoIndividualCache = object : LruCache<Int, CacheEntry<Producto>>(individualCacheSize) {
            override fun sizeOf(key: Int, value: CacheEntry<Producto>): Int {
                return value.sizeInBytes / 1024
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: Int,
                oldValue: CacheEntry<Producto>,
                newValue: CacheEntry<Producto>?
            ) {
                if (evicted) {
                    totalEvictions++
                    Log.d(TAG, "🗑️ [PRODUCTO_INDIVIDUAL] Evicted del cache: producto $key")
                }
            }
        }

        // =====================================================================
        // CACHE DE TIPOS/CATEGORÍAS
        // =====================================================================
        tiposCache = object : LruCache<String, CacheEntry<List<TipoProducto>>>(tiposCacheSize) {
            override fun sizeOf(key: String, value: CacheEntry<List<TipoProducto>>): Int {
                return value.sizeInBytes / 1024
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: CacheEntry<List<TipoProducto>>,
                newValue: CacheEntry<List<TipoProducto>>?
            ) {
                if (evicted) {
                    totalEvictions++
                    Log.d(TAG, "🗑️ [TIPOS] Evicted del cache: $key")
                }
            }
        }

        // =====================================================================
        // CACHE DE COMPRAS (HISTORIAL)
        // =====================================================================
        comprasCache = object : LruCache<String, CacheEntry<List<Compra>>>(tiposCacheSize) {
            override fun sizeOf(key: String, value: CacheEntry<List<Compra>>): Int {
                return value.sizeInBytes / 1024
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: CacheEntry<List<Compra>>,
                newValue: CacheEntry<List<Compra>>?
            ) {
                if (evicted) {
                    totalEvictions++
                    Log.d(TAG, "🗑️ [COMPRAS] Evicted del cache: $key")
                }
            }
        }
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS - PRODUCTOS (LISTAS)
    // =========================================================================

    /**
     * Guarda una lista de productos en el cache
     */
    fun putProductos(key: String, productos: List<Producto>) {
        try {
            val json = gson.toJson(productos)
            val sizeInBytes = json.toByteArray().size

            val entry = CacheEntry(
                data = productos,
                timestamp = System.currentTimeMillis(),
                sizeInBytes = sizeInBytes
            )

            productosCache.put(key, entry)
            Log.d(TAG, "✅ [PRODUCTOS] Guardados en cache: $key (${productos.size} items, ${sizeInBytes / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar productos en cache: ${e.message}")
        }
    }

    /**
     * Obtiene una lista de productos del cache
     * Valida TTL automáticamente
     * @return CacheEntry con los productos o null si no existe o expiró
     */
    fun getProductos(key: String): CacheEntry<List<Producto>>? {
        val entry = productosCache.get(key)

        if (entry == null) {
            totalMisses++
            Log.d(TAG, "❌ [PRODUCTOS] Cache MISS: $key")
            return null
        }

        // Validar TTL
        if (entry.isExpired(PRODUCTOS_TTL)) {
            totalMisses++
            productosCache.remove(key)
            val ageMinutes = entry.getAge() / (60 * 1000)
            Log.d(TAG, "⏰ [PRODUCTOS] Cache EXPIRADO: $key (edad: $ageMinutes min)")
            return null
        }

        totalHits++
        val ageMinutes = entry.getAge() / (60 * 1000)
        Log.d(TAG, "✅ [PRODUCTOS] Cache HIT: $key (edad: $ageMinutes min)")
        return entry
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS - CONVERSIONES
    // =========================================================================

    fun putConversion(productoId: Int, data: ProductoConConversiones) {
        try {
            val json = gson.toJson(data)
            val sizeInBytes = json.toByteArray().size

            val entry = CacheEntry(
                data = data,
                timestamp = System.currentTimeMillis(),
                sizeInBytes = sizeInBytes
            )

            conversionesCache.put(productoId, entry)
            Log.d(TAG, "✅ [CONVERSIONES] Guardadas en cache: producto $productoId (${sizeInBytes / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar conversiones en cache: ${e.message}")
        }
    }

    fun getConversion(productoId: Int): CacheEntry<ProductoConConversiones>? {
        val entry = conversionesCache.get(productoId)

        if (entry == null) {
            totalMisses++
            Log.d(TAG, "❌ [CONVERSIONES] Cache MISS: producto $productoId")
            return null
        }

        if (entry.isExpired(CONVERSIONES_TTL)) {
            totalMisses++
            conversionesCache.remove(productoId)
            val ageHours = entry.getAge() / (60 * 60 * 1000)
            Log.d(TAG, "⏰ [CONVERSIONES] Cache EXPIRADO: producto $productoId (edad: $ageHours h)")
            return null
        }

        totalHits++
        val ageHours = entry.getAge() / (60 * 60 * 1000)
        Log.d(TAG, "✅ [CONVERSIONES] Cache HIT: producto $productoId (edad: $ageHours h)")
        return entry
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS - TIPOS/CATEGORÍAS
    // =========================================================================

    fun putTipos(tipos: List<TipoProducto>) {
        try {
            val json = gson.toJson(tipos)
            val sizeInBytes = json.toByteArray().size

            val entry = CacheEntry(
                data = tipos,
                timestamp = System.currentTimeMillis(),
                sizeInBytes = sizeInBytes
            )

            tiposCache.put("tipos", entry)
            Log.d(TAG, "✅ [TIPOS] Guardados en cache: ${tipos.size} tipos (${sizeInBytes / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar tipos en cache: ${e.message}")
        }
    }

    fun getTipos(): CacheEntry<List<TipoProducto>>? {
        val entry = tiposCache.get("tipos")

        if (entry == null) {
            totalMisses++
            Log.d(TAG, "❌ [TIPOS] Cache MISS")
            return null
        }

        if (entry.isExpired(TIPOS_TTL)) {
            totalMisses++
            tiposCache.remove("tipos")
            val ageHours = entry.getAge() / (60 * 60 * 1000)
            Log.d(TAG, "⏰ [TIPOS] Cache EXPIRADO (edad: $ageHours h)")
            return null
        }

        totalHits++
        Log.d(TAG, "✅ [TIPOS] Cache HIT")
        return entry
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS - PRODUCTO INDIVIDUAL
    // =========================================================================

    fun putProducto(productoId: Int, producto: Producto) {
        try {
            val json = gson.toJson(producto)
            val sizeInBytes = json.toByteArray().size

            val entry = CacheEntry(
                data = producto,
                timestamp = System.currentTimeMillis(),
                sizeInBytes = sizeInBytes
            )

            productoIndividualCache.put(productoId, entry)
            Log.d(TAG, "✅ [PRODUCTO_INDIVIDUAL] Guardado en cache: producto $productoId (${sizeInBytes / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar producto en cache: ${e.message}")
        }
    }

    fun getProducto(productoId: Int): CacheEntry<Producto>? {
        val entry = productoIndividualCache.get(productoId)

        if (entry == null) {
            totalMisses++
            Log.d(TAG, "❌ [PRODUCTO_INDIVIDUAL] Cache MISS: producto $productoId")
            return null
        }

        if (entry.isExpired(PRODUCTOS_TTL)) {
            totalMisses++
            productoIndividualCache.remove(productoId)
            Log.d(TAG, "⏰ [PRODUCTO_INDIVIDUAL] Cache EXPIRADO: producto $productoId")
            return null
        }

        totalHits++
        Log.d(TAG, "✅ [PRODUCTO_INDIVIDUAL] Cache HIT: producto $productoId")
        return entry
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS - HISTORIAL DE COMPRAS
    // =========================================================================

    /**
     * Guarda el historial de compras en el cache
     * TTL: 24 horas (mismo que conversiones)
     */
    fun putCompras(compras: List<Compra>) {
        try {
            val json = gson.toJson(compras)
            val sizeInBytes = json.toByteArray().size

            val entry = CacheEntry(
                data = compras,
                timestamp = System.currentTimeMillis(),
                sizeInBytes = sizeInBytes
            )

            comprasCache.put("historial_compras", entry)
            Log.d(TAG, "✅ [COMPRAS] Guardadas en cache: ${compras.size} compras (${sizeInBytes / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar compras en cache: ${e.message}")
        }
    }

    /**
     * Obtiene el historial de compras del cache
     * @return CacheEntry con las compras o null si no existe o expiró
     */
    fun getCompras(): CacheEntry<List<Compra>>? {
        val entry = comprasCache.get("historial_compras")

        if (entry == null) {
            totalMisses++
            Log.d(TAG, "❌ [COMPRAS] Cache MISS")
            return null
        }

        // Usar TTL de 24 horas (igual que conversiones)
        if (entry.isExpired(CONVERSIONES_TTL)) {
            totalMisses++
            comprasCache.remove("historial_compras")
            val ageHours = entry.getAge() / (60 * 60 * 1000)
            Log.d(TAG, "⏰ [COMPRAS] Cache EXPIRADO (edad: $ageHours h)")
            return null
        }

        totalHits++
        Log.d(TAG, "✅ [COMPRAS] Cache HIT: ${entry.data.size} compras")
        return entry
    }

    // =========================================================================
    // MÉTODOS DE GESTIÓN Y LIMPIEZA
    // =========================================================================

    /**
     * Limpia todos los caches
     */
    fun clearAll() {
        productosCache.evictAll()
        conversionesCache.evictAll()
        tiposCache.evictAll()
        productoIndividualCache.evictAll()
        comprasCache.evictAll()
        Log.d(TAG, "🗑️ Todos los caches limpiados")
    }

    /**
     * Elimina entradas expiradas de todos los caches
     * Útil para limpieza proactiva
     */
    fun evictExpired() {
        var evictedCount = 0

        // Evict productos expirados
        val productoKeys = mutableListOf<String>()
        productosCache.snapshot().forEach { (key, entry) ->
            if (entry.isExpired(PRODUCTOS_TTL)) {
                productoKeys.add(key)
            }
        }
        productoKeys.forEach {
            productosCache.remove(it)
            evictedCount++
        }

        // Evict conversiones expiradas
        val conversionKeys = mutableListOf<Int>()
        conversionesCache.snapshot().forEach { (key, entry) ->
            if (entry.isExpired(CONVERSIONES_TTL)) {
                conversionKeys.add(key)
            }
        }
        conversionKeys.forEach {
            conversionesCache.remove(it)
            evictedCount++
        }

        // Evict tipos expirados
        tiposCache.snapshot().forEach { (key, entry) ->
            if (entry.isExpired(TIPOS_TTL)) {
                tiposCache.remove(key)
                evictedCount++
            }
        }

        // Evict productos individuales expirados
        val individualKeys = mutableListOf<Int>()
        productoIndividualCache.snapshot().forEach { (key, entry) ->
            if (entry.isExpired(PRODUCTOS_TTL)) {
                individualKeys.add(key)
            }
        }
        individualKeys.forEach {
            productoIndividualCache.remove(it)
            evictedCount++
        }

        // Evict compras expiradas
        val compraKeys = mutableListOf<String>()
        comprasCache.snapshot().forEach { (key, entry) ->
            if (entry.isExpired(CONVERSIONES_TTL)) {
                compraKeys.add(key)
            }
        }
        compraKeys.forEach {
            comprasCache.remove(it)
            evictedCount++
        }

        Log.d(TAG, "🗑️ Limpieza de expirados: $evictedCount entradas eliminadas")
    }

    /**
     * Genera un key único para listas de productos con filtros
     */
    fun generateProductosKey(idTipo: Int?, disponible: Boolean?): String {
        return "productos_tipo_${idTipo ?: "all"}_disponible_${disponible ?: "all"}"
    }

    /**
     * Reduce el uso de memoria del caché (para responder a presión de memoria)
     * @param percentageToFree Porcentaje de entradas a eliminar (0-100)
     */
    fun trimMemory(percentageToFree: Int) {
        val percent = percentageToFree.coerceIn(0, 100)
        Log.d(TAG, "⚠️ Liberando $percent% del caché por presión de memoria")

        // Calcular cuántas entradas eliminar de cada caché
        val productosToRemove = (productosCache.size() * percent / 100).coerceAtLeast(1)
        val conversionesToRemove = (conversionesCache.size() * percent / 100).coerceAtLeast(1)
        val individualsToRemove = (productoIndividualCache.size() * percent / 100).coerceAtLeast(1)

        // Eliminar las entradas más antiguas primero
        trimCacheByAge(productosCache, productosToRemove)
        trimCacheByAge(conversionesCache, conversionesToRemove)
        trimCacheByAge(productoIndividualCache, individualsToRemove)

        Log.d(TAG, "✅ Memoria liberada exitosamente")
    }

    /**
     * Elimina las N entradas más antiguas de un caché
     */
    private fun <K, V> trimCacheByAge(cache: LruCache<K, CacheEntry<V>>, count: Int) {
        val snapshot = cache.snapshot()
        val sortedByAge = snapshot.entries.sortedBy { it.value.timestamp }

        sortedByAge.take(count).forEach { entry ->
            cache.remove(entry.key)
        }
    }

    /**
     * Obtiene estadísticas de rendimiento del cache
     */
    fun getCacheStats(): CacheStats {
        val totalSize = productosCache.size() + conversionesCache.size() +
                tiposCache.size() + productoIndividualCache.size()

        val maxSize = productosCache.maxSize() + conversionesCache.maxSize() +
                tiposCache.maxSize() + productoIndividualCache.maxSize()

        return CacheStats(
            hits = totalHits,
            misses = totalMisses,
            evictionCount = totalEvictions,
            currentSize = totalSize,
            maxSize = maxSize
        )
    }

    /**
     * Imprime estadísticas del cache en el log
     */
    fun logStats() {
        val stats = getCacheStats()
        Log.d(TAG, stats.generateReport())
    }
}
