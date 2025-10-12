package app.src.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // ⚠️ CAMBIAR ESTO: Usa la IP de tu máquina en la red local para pruebas desde dispositivo físico
    // Para emulador Android: usa "10.0.2.2"
    // Para dispositivo físico: usa la IP de tu PC (ej: "192.168.10.16")
    private const val BASE_URL = "http://192.168.0.5:8080/"

    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun getToken(): String? = token

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val newRequest = if (token != null && !request.url.encodedPath.contains("token")
            && !request.url.encodedPath.contains("usuarios/")) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val usuarioService: UsuarioApiService = retrofit.create(UsuarioApiService::class.java)
    val productoService: ProductoApiService = retrofit.create(ProductoApiService::class.java)
    val compraService: CompraApiService = retrofit.create(CompraApiService::class.java)
    val generalService: GeneralApiService = retrofit.create(GeneralApiService::class.java)
}
