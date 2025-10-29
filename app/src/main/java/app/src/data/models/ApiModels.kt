package app.src.data.models

import com.google.gson.annotations.SerializedName

// ==================== MODELOS DE USUARIO ====================

data class Usuario(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("saldo") val saldo: Double = 0.0
)

data class UsuarioCreate(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class RecargarSaldoRequest(
    @SerializedName("monto") val monto: Double
)

// ==================== MODELOS DE PRODUCTOS ====================

data class TipoProducto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String
)

data class Producto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("imagen_url") val imagenUrl: String?,
    @SerializedName("precio") val precio: Double,
    @SerializedName("disponible") val disponible: Boolean,
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("tipo_producto") val tipoProducto: TipoProducto
)

data class ProductoCreate(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("imagen_url") val imagenUrl: String?,
    @SerializedName("precio") val precio: Double,
    @SerializedName("disponible") val disponible: Boolean,
    @SerializedName("id_tipo") val idTipo: Int
)

// ==================== MODELOS DE COMPRAS ====================

data class DetalleCompraRequest(
    @SerializedName("id_producto") val idProducto: Int,
    @SerializedName("cantidad") val cantidad: Int
)

data class CompraRequest(
    @SerializedName("productos") val productos: List<DetalleCompraRequest>
)

data class DetalleCompraResponse(
    @SerializedName("id_producto") val idProducto: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio_unitario_compra") val precioUnitarioCompra: Double,
    @SerializedName("producto") val producto: Producto
)

data class QR(
    @SerializedName("codigo_qr_hash") val codigoQrHash: String,
    @SerializedName("estado") val estado: EstadoQR
)

enum class EstadoQR {
    @SerializedName("ACTIVO") ACTIVO,
    @SerializedName("CANJEADO") CANJEADO,
    @SerializedName("EXPIRADO") EXPIRADO
}

enum class EstadoCompra {
    @SerializedName("CARRITO") CARRITO,
    @SerializedName("PAGADO") PAGADO,
    @SerializedName("EN_PREPARACION") EN_PREPARACION,
    @SerializedName("LISTO") LISTO,
    @SerializedName("ENTREGADO") ENTREGADO
}

data class Compra(
    @SerializedName("id") val id: Int,
    @SerializedName("fecha_hora") val fechaHora: String,
    @SerializedName("total") val total: Double,
    @SerializedName("estado") val estado: EstadoCompra,
    @SerializedName("detalles") val detalles: List<DetalleCompraResponse>,
    @SerializedName("qr") val qr: QR?,
    // Nuevos campos de fechas
    @SerializedName("fecha_en_preparacion") val fechaEnPreparacion: String?,
    @SerializedName("fecha_listo") val fechaListo: String?,
    @SerializedName("fecha_entregado") val fechaEntregado: String?,
    // Nuevos campos de tiempos (solo se muestran cuando la compra está finalizada)
    @SerializedName("tiempo_hasta_preparacion") val tiempoHastaPreparacion: Double?,
    @SerializedName("tiempo_preparacion") val tiempoPreparacion: Double?,
    @SerializedName("tiempo_espera_entrega") val tiempoEsperaEntrega: Double?,
    @SerializedName("tiempo_total") val tiempoTotal: Double?
)

data class ActualizarEstadoRequest(
    @SerializedName("estado") val estado: String
)

data class EscanearQRRequest(
    @SerializedName("codigo_qr_hash") val codigoQrHash: String
)

data class EscanearQRResponse(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("compra_id") val compraId: Int,
    @SerializedName("cliente") val cliente: String,
    @SerializedName("total") val total: Double
)

// ==================== RESPUESTAS GENERALES ====================

data class ApiResponse(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("version") val version: String? = null,
    @SerializedName("documentacion") val documentacion: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("mensaje") val mensaje: String
)

data class ErrorResponse(
    @SerializedName("detail") val detail: String
)
