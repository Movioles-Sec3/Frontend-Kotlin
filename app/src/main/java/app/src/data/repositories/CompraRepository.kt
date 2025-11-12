package app.src.data.repositories

import android.content.Context
import android.util.Log
import app.src.data.api.ApiClient
import app.src.data.models.Compra
import app.src.data.models.CompraRequest
import app.src.data.models.ActualizarEstadoRequest
import app.src.data.models.EscanearQRRequest
import app.src.data.models.EscanearQRResponse
import app.src.data.models.EstadoCompra
import app.src.data.models.DetalleCompraResponse
import app.src.data.models.Producto
import app.src.data.models.TipoProducto
import app.src.utils.NetworkUtils
import app.src.utils.CartManager
import app.src.data.local.AppDatabase
import app.src.data.local.DataStoreManager
import app.src.data.local.entities.OrderEntity
import app.src.data.local.entities.OrderItemEntity
import app.src.data.local.entities.OrderOutboxEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class CompraRepository {

    private val api = ApiClient.compraService
    private val TAG = "CompraRepository"
    private val gson = Gson()

    /**
     * Obtiene el historial de compras del usuario
     * Estrategia Cache-First: Lee de Room primero, actualiza desde API en background
     */
    suspend fun obtenerHistorial(context: Context): Result<List<Compra>> {
        return try {
            val database = AppDatabase.getDatabase(context)
            val dataStore = DataStoreManager(context)
            val userId = dataStore.userId.first() ?: 0

            // 1. Leer desde Room primero (instantáneo)
            val cachedOrders = database.orderDao().getOrdersByUser(userId).first()

            if (cachedOrders.isNotEmpty()) {
                Log.d(TAG, "📦 Cargando ${cachedOrders.size} órdenes desde Room cache")

                // Convertir OrderEntity a Compra
                val comprasFromCache = cachedOrders.mapNotNull { orderEntity ->
                    try {
                        entityToCompra(orderEntity, database)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error convirtiendo orden ${orderEntity.id}: ${e.message}")
                        null
                    }
                }

                // Intentar actualizar desde API en background (sin bloquear)
                actualizarHistorialDesdeAPI(context, database, userId)

                return Result.Success(comprasFromCache, isFromCache = true, isCacheExpired = false)
            }

            // 2. Si no hay cache, intentar desde API
            val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

            if (hasInternet) {
                Log.d(TAG, "🌐 Sin caché, obteniendo historial de la API...")
                return obtenerYGuardarDesdeAPI(context, database, userId)
            } else {
                Log.d(TAG, "📵 Sin internet y sin caché local")
                return Result.Error("No hay conexión a internet y no hay datos guardados")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Actualiza el historial desde la API en background
     */
    private suspend fun actualizarHistorialDesdeAPI(context: Context, database: AppDatabase, userId: Int) {
        try {
            if (NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode) {
                val response = api.historialCompras("Bearer ${ApiClient.getToken()}")
                if (response.isSuccessful && response.body() != null) {
                    val compras = response.body()!!
                    guardarComprasEnRoom(compras, database, userId)
                    Log.d(TAG, "✅ Historial actualizado desde API en background")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ No se pudo actualizar desde API: ${e.message}")
        }
    }

    /**
     * Obtiene desde API y guarda en Room
     */
    private suspend fun obtenerYGuardarDesdeAPI(context: Context, database: AppDatabase, userId: Int): Result<List<Compra>> {
        return try {
            val response = api.historialCompras("Bearer ${ApiClient.getToken()}")
            if (response.isSuccessful && response.body() != null) {
                val compras = response.body()!!
                guardarComprasEnRoom(compras, database, userId)
                Log.d(TAG, "✅ ${compras.size} compras obtenidas de API y guardadas en Room")
                Result.Success(compras, isFromCache = false, isCacheExpired = false)
            } else {
                Result.Error("Error al obtener historial: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en API: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Guarda compras en Room
     */
    private suspend fun guardarComprasEnRoom(compras: List<Compra>, database: AppDatabase, userId: Int) {
        compras.forEach { compra ->
            try {
                val orderEntity = OrderEntity(
                    id = compra.id,
                    status = compra.estado.name,
                    total = compra.total,
                    createdAt = System.currentTimeMillis(),
                    readyAt = null,
                    deliveredAt = null,
                    userId = userId,
                    qrCode = compra.qr?.codigoQrHash, // ✅ Guardar QR en Room
                    // ✅ NUEVO: Guardar tiempos de entrega
                    tiempoHastaPreparacion = compra.tiempoHastaPreparacion,
                    tiempoPreparacion = compra.tiempoPreparacion,
                    tiempoEsperaEntrega = compra.tiempoEsperaEntrega,
                    tiempoTotal = compra.tiempoTotal
                )

                val orderItems = compra.detalles.map { detalle ->
                    OrderItemEntity(
                        orderId = compra.id,
                        productId = detalle.producto.id,
                        name = detalle.producto.nombre,
                        quantity = detalle.cantidad,
                        price = detalle.precioUnitarioCompra
                    )
                }

                database.orderDao().insertOrderWithItems(orderEntity, orderItems)
            } catch (e: Exception) {
                Log.w(TAG, "Error guardando orden ${compra.id}: ${e.message}")
            }
        }
    }

    /**
     * Convierte OrderEntity a Compra
     */
    private suspend fun entityToCompra(orderEntity: OrderEntity, database: AppDatabase): Compra {
        val items = database.orderDao().getOrderItems(orderEntity.id)

        // ✅ Generar QR offline si no existe
        val qrCode = orderEntity.qrCode ?: generarQROffline(orderEntity.id)

        // Crear una compra básica desde la entidad
        return Compra(
            id = orderEntity.id,
            fechaHora = orderEntity.createdAt.toString(),
            total = orderEntity.total,
            estado = when (orderEntity.status) {
                "LISTO" -> EstadoCompra.LISTO
                "ENTREGADO" -> EstadoCompra.ENTREGADO
                "EN_PREPARACION" -> EstadoCompra.EN_PREPARACION
                "PAGADO" -> EstadoCompra.PAGADO
                "PENDIENTE_SINCRONIZAR" -> EstadoCompra.PAGADO // Mostrar como PAGADO
                else -> EstadoCompra.CARRITO
            },
            detalles = items.map { item ->
                DetalleCompraResponse(
                    idProducto = item.productId,
                    cantidad = item.quantity,
                    precioUnitarioCompra = item.price,
                    producto = Producto(
                        id = item.productId,
                        nombre = item.name,
                        descripcion = "",
                        imagenUrl = null,
                        precio = item.price,
                        disponible = true,
                        idTipo = 1,
                        tipoProducto = TipoProducto(1, "General")
                    )
                )
            },
            qr = app.src.data.models.QR(
                codigoQrHash = qrCode,
                estado = app.src.data.models.EstadoQR.ACTIVO
            ), // ✅ Incluir QR (generado offline o desde la BD)
            fechaEnPreparacion = null,
            fechaListo = null,
            fechaEntregado = null,
            // ✅ NUEVO: Recuperar tiempos de entrega desde Room
            tiempoHastaPreparacion = orderEntity.tiempoHastaPreparacion,
            tiempoPreparacion = orderEntity.tiempoPreparacion,
            tiempoEsperaEntrega = orderEntity.tiempoEsperaEntrega,
            tiempoTotal = orderEntity.tiempoTotal
        )
    }

    /**
     * Genera un código QR offline para órdenes que no tienen uno
     * Formato: ORDER_{orderId}_{timestamp}
     */
    private fun generarQROffline(orderId: Int): String {
        val timestamp = System.currentTimeMillis()
        return "OFFLINE_ORDER_${orderId}_${timestamp}"
    }

    /**
     * Crea una nueva compra
     * Soporta modo offline guardando en outbox
     */
    suspend fun crearCompra(context: Context, compraRequest: CompraRequest): Result<Compra> {
        val database = AppDatabase.getDatabase(context)
        val dataStore = DataStoreManager(context)
        val userId = dataStore.userId.first() ?: 0
        val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

        // ✅ SI NO HAY INTERNET, GUARDAR EN OUTBOX DIRECTAMENTE (SIN INTENTAR CONECTAR)
        if (!hasInternet) {
            Log.d(TAG, "📵 Sin internet detectado, guardando en outbox...")

            return try {
                val currentTime = System.currentTimeMillis()

                // 1. Guardar en outbox para sincronizar después
                val outboxEntry = OrderOutboxEntity(
                    payloadJson = gson.toJson(compraRequest),
                    createdAt = currentTime,
                    retries = 0
                )
                val outboxId = database.orderOutboxDao().insert(outboxEntry)

                // 2. ✅ OBTENER EL SIGUIENTE ID DE ORDEN (continúa desde la última orden)
                val maxOrderId = database.orderDao().getMaxOrderId() ?: 0
                val nextOrderId = maxOrderId + 1

                Log.d(TAG, "📊 Último ID de orden: $maxOrderId, nuevo ID: $nextOrderId")

                // Calcular total estimado desde los productos del carrito
                val estimatedTotal = CartManager.getTotal()

                val tempOrderEntity = OrderEntity(
                    id = nextOrderId,
                    status = "PENDIENTE_SINCRONIZAR", // Estado especial para órdenes offline
                    total = estimatedTotal,
                    createdAt = currentTime,
                    readyAt = null,
                    deliveredAt = null,
                    userId = userId
                )

                // 3. Crear items de la orden
                val orderItems = compraRequest.productos.mapNotNull { detalle ->
                    // Buscar el producto en el carrito para obtener su información
                    val cartItem = CartManager.getItems().find { it.producto.id == detalle.idProducto }
                    cartItem?.let {
                        OrderItemEntity(
                            orderId = nextOrderId,
                            productId = detalle.idProducto,
                            name = it.producto.nombre,
                            quantity = detalle.cantidad,
                            price = it.producto.precio
                        )
                    }
                }

                // 4. Guardar orden temporal en Room
                database.orderDao().insertOrderWithItems(tempOrderEntity, orderItems)

                Log.d(TAG, "📤 Orden temporal ID:$nextOrderId guardada en historial (pendiente de sincronizar)")
                Log.d(TAG, "📤 Orden guardada en outbox ID:$outboxId para sincronizar después")

                Result.Error("Sin conexión. Tu orden se guardó y se procesará cuando tengas internet.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error guardando en outbox: ${e.message}")
                Result.Error("Error al guardar la orden: ${e.message}")
            }
        }

        // CON INTERNET: Intentar enviar a API
        return try {
            val response = api.crearCompra("Bearer ${ApiClient.getToken()}", compraRequest)
            if (response.isSuccessful && response.body() != null) {
                val compra = response.body()!!

                // Guardar en Room
                val orderEntity = OrderEntity(
                    id = compra.id,
                    status = compra.estado.name,
                    total = compra.total,
                    createdAt = System.currentTimeMillis(),
                    userId = userId
                )

                val orderItems = compraRequest.productos.map { detalle ->
                    OrderItemEntity(
                        orderId = compra.id,
                        productId = detalle.idProducto,
                        name = "Producto ${detalle.idProducto}",
                        quantity = detalle.cantidad,
                        price = 0.0 // Se actualizará con el total
                    )
                }

                database.orderDao().insertOrderWithItems(orderEntity, orderItems)

                // Guardar en DataStore
                dataStore.saveLastOrder(compra.id, compra.total)

                Log.d(TAG, "✅ Compra creada exitosamente: ID ${compra.id}")
                Result.Success(compra, isFromCache = false, isCacheExpired = false)
            } else {
                Result.Error("Error al crear compra: ${response.code()}")
            }
        } catch (e: Exception) {
            // ✅ DETECTAR ERRORES DE CONEXIÓN Y GUARDAR EN OUTBOX AUTOMÁTICAMENTE
            val isConnectionError = e.message?.contains("failed to connect", ignoreCase = true) == true ||
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e is java.net.ConnectException ||
                    e is java.net.SocketTimeoutException ||
                    e is java.net.UnknownHostException

            if (isConnectionError) {
                Log.w(TAG, "🌐 Servidor no accesible, guardando orden en outbox...")

                try {
                    val currentTime = System.currentTimeMillis()

                    // 1. Guardar en outbox para sincronizar después
                    val outboxEntry = OrderOutboxEntity(
                        payloadJson = gson.toJson(compraRequest),
                        createdAt = currentTime,
                        retries = 0
                    )
                    val outboxId = database.orderOutboxDao().insert(outboxEntry)

                    // 2. ✅ OBTENER EL SIGUIENTE ID DE ORDEN (continúa desde la última orden)
                    val maxOrderId = database.orderDao().getMaxOrderId() ?: 0
                    val nextOrderId = maxOrderId + 1

                    Log.d(TAG, "📊 Último ID de orden: $maxOrderId, nuevo ID: $nextOrderId")

                    val estimatedTotal = CartManager.getTotal()

                    val tempOrderEntity = OrderEntity(
                        id = nextOrderId,
                        status = "PENDIENTE_SINCRONIZAR",
                        total = estimatedTotal,
                        createdAt = currentTime,
                        readyAt = null,
                        deliveredAt = null,
                        userId = userId
                    )

                    // 3. Crear items de la orden
                    val orderItems = compraRequest.productos.mapNotNull { detalle ->
                        val cartItem = CartManager.getItems().find { it.producto.id == detalle.idProducto }
                        cartItem?.let {
                            OrderItemEntity(
                                orderId = nextOrderId,
                                productId = detalle.idProducto,
                                name = it.producto.nombre,
                                quantity = detalle.cantidad,
                                price = it.producto.precio
                            )
                        }
                    }

                    // 4. Guardar orden temporal en Room
                    database.orderDao().insertOrderWithItems(tempOrderEntity, orderItems)

                    Log.d(TAG, "📤 Orden temporal ID:$nextOrderId guardada en historial (servidor no disponible)")
                    Log.d(TAG, "📤 Orden guardada en outbox ID:$outboxId para sincronizar después")

                    return Result.Error("El servidor no está disponible. Tu orden se guardó y se procesará cuando haya conexión.")
                } catch (outboxError: Exception) {
                    Log.e(TAG, "❌ Error guardando en outbox: ${outboxError.message}")
                    return Result.Error("No se pudo guardar la orden: ${outboxError.message}")
                }
            }

            // SI NO ES ERROR DE CONEXIÓN, REPORTAR EL ERROR ORIGINAL
            Log.e(TAG, "❌ Error al crear compra: ${e.message}")
            Result.Error("Error al procesar la compra: ${e.message}")
        }
    }

    /**
     * Actualiza el estado de una compra
     * ✅ FUNCIONA OFFLINE: Guarda el cambio localmente y sincroniza después
     */
    suspend fun actualizarEstado(
        context: Context,
        compraId: Int,
        nuevoEstado: String
    ): Result<Compra> {
        val database = AppDatabase.getDatabase(context)
        val hasInternet = NetworkUtils.isNetworkAvailable(context) && !ApiClient.forceOfflineMode

        return try {
            if (hasInternet) {
                // CON INTERNET: Actualizar en el servidor
                val response = api.actualizarEstadoCompra(
                    compraId,
                    ActualizarEstadoRequest(nuevoEstado)
                )

                if (response.isSuccessful && response.body() != null) {
                    val compra = response.body()!!

                    // Actualizar también en Room
                    database.orderDao().updateOrderStatus(compraId, nuevoEstado, System.currentTimeMillis())

                    Log.d(TAG, "✅ Estado actualizado en servidor y localmente: $nuevoEstado")
                    Result.Success(compra, isFromCache = false, isCacheExpired = false)
                } else {
                    Result.Error("Error al actualizar estado: ${response.code()}")
                }
            } else {
                // ✅ SIN INTERNET: Guardar cambio localmente
                Log.d(TAG, "📵 Sin conexión, guardando cambio de estado localmente")

                // Actualizar estado en Room
                database.orderDao().updateOrderStatus(compraId, nuevoEstado, System.currentTimeMillis())

                // Obtener la orden actualizada para devolver
                val orderEntity = database.orderDao().getOrderById(compraId)

                if (orderEntity != null) {
                    val compraActualizada = entityToCompra(orderEntity, database)
                    Log.d(TAG, "✅ Estado actualizado localmente a: $nuevoEstado (se sincronizará después)")

                    // Retornar Success con un mensaje informativo
                    Result.Success(compraActualizada, isFromCache = true, isCacheExpired = false)
                } else {
                    Result.Error("Orden no encontrada")
                }
            }
        } catch (e: Exception) {
            // Si hay error de conexión, intentar guardar localmente
            val isConnectionError = e.message?.contains("failed to connect", ignoreCase = true) == true ||
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e is java.net.ConnectException ||
                    e is java.net.SocketTimeoutException ||
                    e is java.net.UnknownHostException

            if (isConnectionError) {
                try {
                    Log.d(TAG, "🌐 Error de conexión, guardando estado localmente")
                    database.orderDao().updateOrderStatus(compraId, nuevoEstado, System.currentTimeMillis())

                    val orderEntity = database.orderDao().getOrderById(compraId)
                    if (orderEntity != null) {
                        val compraActualizada = entityToCompra(orderEntity, database)
                        return Result.Success(compraActualizada, isFromCache = true, isCacheExpired = false)
                    }
                } catch (localError: Exception) {
                    Log.e(TAG, "❌ Error guardando localmente: ${localError.message}")
                }
            }

            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Escanea un código QR para validar y entregar un pedido
     * Solo funciona con internet (staff validation)
     */
    suspend fun escanearQR(codigoQR: String): Result<EscanearQRResponse> {
        return try {
            val response = api.escanearQR(EscanearQRRequest(codigoQR))

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ QR escaneado exitosamente")
                Result.Success(response.body()!!, isFromCache = false, isCacheExpired = false)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Código QR no válido o no encontrado"
                    400 -> "El código QR ya fue canjeado o no está listo"
                    else -> "Error al escanear QR: ${response.code()}"
                }
                Log.e(TAG, "❌ Error al escanear QR: ${response.code()}")
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al escanear QR: ${e.message}")
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
