package app.src.data.api

import app.src.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface UsuarioApiService {

    @POST("usuarios/")
    suspend fun registrarUsuario(@Body usuario: UsuarioCreate): Response<Usuario>

    @POST("usuarios/token")
    suspend fun login(@Body loginRequest: LoginRequest): Response<TokenResponse>

    @GET("usuarios/me")
    suspend fun obtenerPerfil(@Header("Authorization") token: String): Response<Usuario>

    @POST("usuarios/me/recargar")
    suspend fun recargarSaldo(
        @Header("Authorization") token: String,
        @Body recarga: RecargarSaldoRequest
    ): Response<Usuario>
}

interface ProductoApiService {

    @GET("productos/")
    suspend fun listarProductos(
        @Query("id_tipo") idTipo: Int? = null,
        @Query("disponible") disponible: Boolean? = true
    ): Response<List<Producto>>

    @GET("productos/{producto_id}")
    suspend fun obtenerProducto(@Path("producto_id") productoId: Int): Response<Producto>

    @GET("productos/tipos/")
    suspend fun listarTipos(): Response<List<TipoProducto>>

    @GET("productos/recomendados")
    suspend fun obtenerProductosRecomendados(): Response<List<Producto>>

    @GET("productos/{producto_id}/conversiones")
    suspend fun obtenerConversionesProducto(@Path("producto_id") productoId: Int): Response<ProductoConConversiones>

    @POST("productos/tipos/")
    suspend fun crearTipo(@Body tipo: TipoProducto): Response<TipoProducto>

    @POST("productos/")
    suspend fun crearProducto(@Body producto: ProductoCreate): Response<Producto>

    @PUT("productos/{producto_id}")
    suspend fun actualizarProducto(
        @Path("producto_id") productoId: Int,
        @Body producto: ProductoCreate
    ): Response<Producto>
}

interface CompraApiService {

    @POST("compras/")
    suspend fun crearCompra(
        @Header("Authorization") token: String,
        @Body compraRequest: CompraRequest
    ): Response<Compra>

    @GET("compras/me")
    suspend fun historialCompras(@Header("Authorization") token: String): Response<List<Compra>>

    @GET("compras/pendientes")
    suspend fun comprasPendientes(): Response<List<Compra>>

    @PUT("compras/{compra_id}/estado")
    suspend fun actualizarEstadoCompra(
        @Path("compra_id") compraId: Int,
        @Body estado: ActualizarEstadoRequest
    ): Response<Compra>

    @POST("compras/qr/escanear")
    suspend fun escanearQR(@Body escanear: EscanearQRRequest): Response<EscanearQRResponse>
}

interface GeneralApiService {

    @GET("/")
    suspend fun root(): Response<ApiResponse>

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>
}
