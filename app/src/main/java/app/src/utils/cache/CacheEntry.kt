package app.src.utils.cache

/**
 * CacheEntry - Wrapper para datos almacenados en cache
 *
 * Esta clase envuelve los datos cacheados junto con metadata necesaria
 * para la gestión del cache LRU.
 *
 * @param T Tipo de dato a cachear
 * @property data Los datos reales a almacenar
 * @property timestamp Momento en que se guardó en cache (milisegundos desde epoch)
 * @property sizeInBytes Tamaño aproximado en bytes del objeto serializado
 *
 * DECISIÓN DE DISEÑO:
 * -------------------
 * Se usa un wrapper en lugar de modificar las clases de modelo para:
 * 1. Mantener los modelos limpios (separación de concerns)
 * 2. Agregar metadata sin contaminar la lógica de negocio
 * 3. Permitir cálculos de TTL sin modificar cada modelo
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
    val sizeInBytes: Int
) {
    /**
     * Verifica si la entrada ha expirado según el TTL proporcionado
     *
     * @param ttl Time To Live en milisegundos
     * @return true si ha pasado más tiempo que el TTL, false en caso contrario
     */
    fun isExpired(ttl: Long): Boolean {
        val age = System.currentTimeMillis() - timestamp
        return age > ttl
    }

    /**
     * Retorna la edad de la entrada en milisegundos
     */
    fun getAge(): Long {
        return System.currentTimeMillis() - timestamp
    }
}

