# 🔌 Guía de Integración - TapAndToast API

## 📋 Resumen de la Integración

Se ha integrado completamente la aplicación Android con el backend de TapAndToast API. La integración incluye:

### ✅ Componentes Implementados

1. **Modelos de Datos** (`data/models/ApiModels.kt`)
   - Usuario, Producto, TipoProducto
   - Compra, DetalleCompra, QR
   - Enums: EstadoCompra, EstadoQR
   - Requests y Responses para todos los endpoints

2. **Servicios de API** (`data/api/ApiServices.kt`)
   - `UsuarioApiService`: Login, registro, perfil, recarga de saldo
   - `ProductoApiService`: Listar productos, categorías, crear/actualizar
   - `CompraApiService`: Crear compra, historial, escanear QR, actualizar estado
   - `GeneralApiService`: Health check, root

3. **Cliente de Red** (`data/api/ApiClient.kt`)
   - Configuración de Retrofit con Gson
   - Interceptor de logging (para debug)
   - Interceptor de autenticación JWT automático
   - Gestión del token Bearer

4. **Repositorios** (`data/repositories/`)
   - `UsuarioRepository`: Manejo de autenticación y perfil
   - `ProductoRepository`: Operaciones con productos y categorías
   - `CompraRepository`: Gestión de pedidos y pagos
   - Patrón `Result<T>` para manejo de errores

5. **ViewModels**
   - `LoginViewModel`: Autenticación (actualizado con API real)
   - `ProductoViewModel`: Carga de productos y categorías
   - `CompraViewModel`: Creación de compras e historial
   - `OrderPickupViewModel`: (existente) Generación de códigos QR

6. **Utilidades**
   - `SessionManager`: Gestión de token y datos de usuario en SharedPreferences

7. **Activities Actualizadas**
   - `LoginActivity`: Login con API, guarda sesión, verifica sesión activa
   - `HomeActivity`: Muestra info del usuario, logout

---

## 🚀 Configuración Inicial

### 1. Configurar la URL del Backend

Edita el archivo `data/api/ApiClient.kt` y cambia la `BASE_URL`:

```kotlin
// Para emulador Android Studio:
private const val BASE_URL = "http://10.0.2.2:8000/"

// Para dispositivo físico en la misma red:
private const val BASE_URL = "http://192.168.1.XXX:8000/"
// (Reemplaza XXX con la IP de tu PC)

// Para servidor en producción:
private const val BASE_URL = "https://tu-dominio.com/"
```

### 2. Permisos (Ya configurados)

El AndroidManifest.xml ya incluye:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `usesCleartextTraffic="true"` (para HTTP local)

### 3. Dependencias (Ya agregadas)

- Retrofit 2.9.0
- OkHttp 4.12.0
- Gson 2.10.1
- Coroutines

---

## 📱 Uso en las Activities

### Ejemplo 1: Listar Productos (ProductActivity)

```kotlin
class ProductActivity : AppCompatActivity() {
    private val viewModel: ProductoViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)
        
        // Cargar token de sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }
        
        // Observer
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProductoUiState.Loading -> {
                    // Mostrar loading
                }
                is ProductoUiState.Success -> {
                    // Mostrar productos en RecyclerView
                    val productos = state.productos
                    // adapter.submitList(productos)
                }
                is ProductoUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
        
        // Cargar productos
        viewModel.cargarProductos()
    }
}
```

### Ejemplo 2: Listar Categorías (CategoryActivity)

```kotlin
class CategoryActivity : AppCompatActivity() {
    private val viewModel: ProductoViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        
        viewModel.categorias.observe(this) { categorias ->
            // Mostrar categorías
            // adapter.submitList(categorias)
        }
        
        viewModel.cargarCategorias()
    }
}
```

### Ejemplo 3: Crear Compra (OrderSummaryActivity)

```kotlin
class OrderSummaryActivity : AppCompatActivity() {
    private val viewModel: CompraViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_summary)
        
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CompraUiState.Loading -> {
                    // Mostrar loading
                }
                is CompraUiState.Success -> {
                    val compra = state.compra
                    // Navegar a OrderPickupActivity con el código QR
                    val intent = Intent(this, OrderPickupActivity::class.java).apply {
                        putExtra("pickup_code", compra.qr?.codigoQrHash)
                        putExtra("title", "¡Tu orden está lista!")
                        putExtra("subtitle", "Orden #${compra.id}")
                    }
                    startActivity(intent)
                    finish()
                }
                is CompraUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
        
        findViewById<Button>(R.id.btn_checkout).setOnClickListener {
            // Crear lista de productos
            val productos = listOf(
                DetalleCompraRequest(idProducto = 1, cantidad = 2),
                DetalleCompraRequest(idProducto = 3, cantidad = 1)
            )
            viewModel.crearCompra(productos)
        }
    }
}
```

### Ejemplo 4: Recargar Saldo

```kotlin
private val usuarioRepo = UsuarioRepository()

fun recargarSaldo(monto: Double) {
    lifecycleScope.launch {
        when (val result = usuarioRepo.recargarSaldo(monto)) {
            is Result.Success -> {
                val usuario = result.data
                SessionManager.saveUserData(
                    this@MyActivity,
                    usuario.id,
                    usuario.nombre,
                    usuario.email,
                    usuario.saldo
                )
                Toast.makeText(this@MyActivity, 
                    "Saldo actualizado: $${usuario.saldo}", 
                    Toast.LENGTH_SHORT).show()
            }
            is Result.Error -> {
                Toast.makeText(this@MyActivity, 
                    result.message, 
                    Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
}
```

---

## 🔐 Flujo de Autenticación

### Login
1. Usuario ingresa email y contraseña
2. `LoginViewModel` llama a `AuthRepository.login()`
3. `AuthRepository` usa `UsuarioRepository.login()`
4. Se obtiene el token JWT
5. Token se guarda en `ApiClient` y `SessionManager`
6. Se obtiene el perfil del usuario
7. Datos del usuario se guardan en `SessionManager`
8. Navega a `HomeActivity`

### Verificación de Sesión
En cada Activity principal:
```kotlin
val token = SessionManager.getToken(this)
if (token != null) {
    ApiClient.setToken(token)
} else {
    // Redirigir a LoginActivity
    startActivity(Intent(this, LoginActivity::class.java))
    finish()
}
```

### Logout
```kotlin
SessionManager.clearSession(this)
ApiClient.setToken(null)
val intent = Intent(this, LoginActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
finish()
```

---

## 🛠️ Uso Directo de Repositorios

Si no quieres usar ViewModels, puedes llamar directamente a los repositorios:

```kotlin
private val productoRepo = ProductoRepository()
private val compraRepo = CompraRepository()
private val usuarioRepo = UsuarioRepository()

fun ejemplo() {
    lifecycleScope.launch {
        // Listar productos
        when (val result = productoRepo.listarProductos()) {
            is Result.Success -> {
                val productos = result.data
                // Usar productos
            }
            is Result.Error -> {
                // Manejar error
            }
            else -> {}
        }
        
        // Crear compra
        val items = listOf(
            DetalleCompraRequest(1, 2),
            DetalleCompraRequest(2, 1)
        )
        when (val result = compraRepo.crearCompra(items)) {
            is Result.Success -> {
                val compra = result.data
                // Mostrar QR: compra.qr?.codigoQrHash
            }
            is Result.Error -> {
                // Manejar error
            }
            else -> {}
        }
    }
}
```

---

## 🐛 Testing y Debug

### Ver Logs de Red

Los logs de Retrofit están activados en modo DEBUG. Verás en Logcat:
- URL de cada petición
- Headers (incluyendo Authorization)
- Body de request y response
- Códigos de estado HTTP

### Probar desde el Emulador

1. Inicia el backend: `uvicorn main:app --reload`
2. Usa la URL: `http://10.0.2.2:8000/`
3. El emulador redirige `10.0.2.2` a `localhost` de tu PC

### Probar desde Dispositivo Físico

1. Conecta tu PC y dispositivo a la misma red WiFi
2. Encuentra la IP de tu PC: `ipconfig` (Windows) o `ifconfig` (Mac/Linux)
3. Cambia la `BASE_URL` a `http://TU_IP:8000/`
4. Asegúrate de que el firewall permite conexiones al puerto 8000

---

## 📦 Estructura de Archivos Creados

```
app/src/main/java/app/src/
├── data/
│   ├── api/
│   │   ├── ApiClient.kt          // Cliente Retrofit configurado
│   │   └── ApiServices.kt        // Interfaces de servicios
│   ├── models/
│   │   └── ApiModels.kt          // Modelos de datos
│   └── repositories/
│       ├── UsuarioRepository.kt  // Repo de usuarios
│       ├── ProductoRepository.kt // Repo de productos
│       └── CompraRepository.kt   // Repo de compras
├── utils/
│   └── SessionManager.kt         // Gestión de sesión
├── AuthRepository.kt             // (Actualizado)
├── LoginViewModel.kt             // (Actualizado)
├── LoginActivity.kt              // (Actualizado)
├── HomeActivity.kt               // (Actualizado)
├── ProductoViewModel.kt          // Nuevo ViewModel
└── CompraViewModel.kt            // Nuevo ViewModel
```

---

## 🔄 Próximos Pasos Sugeridos

1. **Actualizar ProductActivity** para mostrar productos reales desde la API
2. **Actualizar CategoryActivity** para mostrar categorías desde la API
3. **Actualizar OrderSummaryActivity** para crear compras reales
4. **Agregar RecyclerView** para listas de productos/categorías
5. **Agregar Glide/Coil** para cargar imágenes de productos
6. **Implementar caché** con Room Database para modo offline
7. **Agregar pull-to-refresh** en listas
8. **Mejorar manejo de errores** con reintentos
9. **Agregar pantalla de perfil** para ver/editar datos del usuario
10. **Implementar carrito de compras** antes de finalizar pedido

---

## ❓ Solución de Problemas

### Error: "Unable to resolve host"
- Verifica que el backend esté corriendo
- Verifica la URL en `ApiClient.BASE_URL`
- Verifica permisos de Internet en AndroidManifest

### Error: "Unauthorized" (401)
- El token expiró o es inválido
- Hacer logout y login nuevamente
- Verificar que el token se está enviando correctamente

### Error: "Network Security Policy"
- Asegúrate de tener `usesCleartextTraffic="true"` en AndroidManifest
- Para producción, usa HTTPS

### Los datos no se muestran
- Verifica los logs en Logcat
- Asegúrate de estar observando los LiveData
- Verifica que la UI se actualice en el hilo principal

---

## 📞 API Endpoints Disponibles

Consulta la documentación completa del backend en:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

---

¡La integración está completa y lista para usar! 🎉

