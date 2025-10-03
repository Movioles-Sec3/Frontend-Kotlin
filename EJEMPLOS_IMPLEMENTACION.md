# 🚀 Ejemplos Prácticos de Implementación

## Ejemplo Completo: ProductActivity con API Real

```kotlin
package app.src

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.src.data.api.ApiClient
import app.src.data.models.Producto
import app.src.utils.SessionManager

class ProductActivity : AppCompatActivity() {
    
    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)
        
        // Verificar sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }
        
        // Inicializar vistas
        recyclerView = findViewById(R.id.rv_productos)
        progressBar = findViewById(R.id.progress_bar)
        errorText = findViewById(R.id.tv_error)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Observer
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProductoUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                }
                is ProductoUiState.Success -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    
                    // Crear adapter simple con los productos
                    val adapter = ProductoAdapter(state.productos) { producto ->
                        // Click en producto
                        Toast.makeText(this, 
                            "Producto: ${producto.nombre} - $${producto.precio}", 
                            Toast.LENGTH_SHORT).show()
                    }
                    recyclerView.adapter = adapter
                }
                is ProductoUiState.Error -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                    recyclerView.visibility = View.GONE
                }
                else -> {}
            }
        }
        
        // Cargar productos
        viewModel.cargarProductos()
        
        // Botón volver
        findViewById<Button>(R.id.btn_back_to_home)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}

// Adapter simple para RecyclerView
class ProductoAdapter(
    private val productos: List<Producto>,
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.tv_producto_nombre)
        val precio: TextView = view.findViewById(R.id.tv_producto_precio)
        val descripcion: TextView = view.findViewById(R.id.tv_producto_descripcion)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = productos[position]
        holder.nombre.text = producto.nombre
        holder.precio.text = "$${producto.precio}"
        holder.descripcion.text = producto.descripcion ?: ""
        holder.itemView.setOnClickListener { onClick(producto) }
    }
    
    override fun getItemCount() = productos.size
}
```

## Ejemplo: CategoryActivity con Filtrado

```kotlin
package app.src

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import app.src.data.api.ApiClient
import app.src.utils.SessionManager

class CategoryActivity : AppCompatActivity() {
    
    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var spinner: Spinner
    private lateinit var tvCount: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        
        // Verificar sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }
        
        spinner = findViewById(R.id.spinner_categorias)
        tvCount = findViewById(R.id.tv_categoria_count)
        
        // Observer de categorías
        viewModel.categorias.observe(this) { categorias ->
            val nombres = categorias.map { it.nombre }
            val adapter = ArrayAdapter(this, 
                android.R.layout.simple_spinner_item, nombres)
            adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val categoriaId = categorias[pos].id
                    viewModel.cargarProductos(categoriaId)
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        
        // Observer de productos
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ProductoUiState.Success -> {
                    tvCount.text = "${state.productos.size} productos"
                }
                is ProductoUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
        
        // Cargar categorías
        viewModel.cargarCategorias()
        
        findViewById<Button>(R.id.btn_back_to_home)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}
```

## Ejemplo: OrderSummaryActivity Actualizado con API

```kotlin
package app.src

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.src.data.api.ApiClient
import app.src.data.models.DetalleCompraRequest
import app.src.utils.SessionManager
import kotlinx.coroutines.launch

class OrderSummaryActivity : AppCompatActivity() {

    private val compraViewModel: CompraViewModel by viewModels()
    
    // Lista temporal de items del carrito (puedes obtenerla de un singleton o Intent)
    private val cartItems = mutableListOf<CartItem>()
    
    data class CartItem(
        val id: Int,
        val title: String,
        val qty: Int,
        val price: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_summary)
        
        // Verificar sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        } else {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mock items (normalmente vendrían de un carrito global)
        cartItems.addAll(listOf(
            CartItem(1, "Cerveza IPA", 2, 8500.0),
            CartItem(2, "Hamburguesa", 1, 15000.0)
        ))
        
        mostrarItems()
        
        // Observer del ViewModel
        compraViewModel.uiState.observe(this) { state ->
            when (state) {
                is CompraUiState.Loading -> {
                    findViewById<ProgressBar>(R.id.progress_bar)?.visibility = 
                        android.view.View.VISIBLE
                    findViewById<Button>(R.id.btn_checkout)?.isEnabled = false
                }
                is CompraUiState.Success -> {
                    findViewById<ProgressBar>(R.id.progress_bar)?.visibility = 
                        android.view.View.GONE
                    
                    val compra = state.compra
                    Toast.makeText(this, 
                        "¡Compra exitosa! Total: $${compra.total}", 
                        Toast.LENGTH_LONG).show()
                    
                    // Actualizar saldo del usuario
                    actualizarSaldoLocal(compra.total)
                    
                    // Navegar a OrderPickupActivity con el código QR
                    val intent = Intent(this, OrderPickupActivity::class.java).apply {
                        putExtra("pickup_code", compra.qr?.codigoQrHash)
                        putExtra("title", "¡Tu orden está lista!")
                        putExtra("subtitle", "Orden #${compra.id} - Total: $${compra.total}")
                    }
                    startActivity(intent)
                    finish()
                }
                is CompraUiState.Error -> {
                    findViewById<ProgressBar>(R.id.progress_bar)?.visibility = 
                        android.view.View.GONE
                    findViewById<Button>(R.id.btn_checkout)?.isEnabled = true
                    
                    Toast.makeText(this, 
                        "Error: ${state.message}", 
                        Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        findViewById<Button>(R.id.btn_checkout)?.setOnClickListener {
            realizarCompra()
        }

        findViewById<Button>(R.id.btn_back_to_home)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
    
    private fun mostrarItems() {
        val container = findViewById<LinearLayout>(R.id.ll_items_container)
        val inflater = LayoutInflater.from(this)
        
        var subtotal = 0.0
        container.removeAllViews()
        
        for (item in cartItems) {
            val row = inflater.inflate(R.layout.item_order_summary, container, false)
            row.findViewById<TextView>(R.id.tv_item_title).text = item.title
            row.findViewById<TextView>(R.id.tv_item_qty_price).text = 
                "${item.qty} × $${item.price}"
            val lineTotal = item.qty * item.price
            row.findViewById<TextView>(R.id.tv_item_total).text = "$${lineTotal}"
            subtotal += lineTotal
            container.addView(row)
        }
        
        val tax = subtotal * 0.08
        val total = subtotal + tax
        
        findViewById<TextView>(R.id.tv_subtotal_value).text = "$${subtotal}"
        findViewById<TextView>(R.id.tv_tax_value).text = "$${tax}"
        findViewById<TextView>(R.id.tv_total_value).text = "$${total}"
    }
    
    private fun realizarCompra() {
        // Convertir CartItem a DetalleCompraRequest
        val detalles = cartItems.map { item ->
            DetalleCompraRequest(
                idProducto = item.id,
                cantidad = item.qty
            )
        }
        
        // Crear compra usando el ViewModel
        compraViewModel.crearCompra(detalles)
    }
    
    private fun actualizarSaldoLocal(totalGastado: Double) {
        val saldoActual = SessionManager.getUserSaldo(this)
        val nuevoSaldo = saldoActual - totalGastado
        
        val nombre = SessionManager.getUserName(this) ?: ""
        val email = SessionManager.getUserEmail(this) ?: ""
        val userId = 0 // Obtener del SessionManager si lo guardaste
        
        SessionManager.saveUserData(this, userId, nombre, email, nuevoSaldo)
    }
}
```

## Ejemplo: Pantalla de Perfil de Usuario

```kotlin
package app.src

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.src.data.api.ApiClient
import app.src.data.repositories.Result
import app.src.data.repositories.UsuarioRepository
import app.src.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    
    private val usuarioRepo = UsuarioRepository()
    private lateinit var tvNombre: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var etMonto: EditText
    private lateinit var btnRecargar: Button
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Verificar sesión
        val token = SessionManager.getToken(this)
        if (token != null) {
            ApiClient.setToken(token)
        }
        
        tvNombre = findViewById(R.id.tv_nombre)
        tvEmail = findViewById(R.id.tv_email)
        tvSaldo = findViewById(R.id.tv_saldo)
        etMonto = findViewById(R.id.et_monto)
        btnRecargar = findViewById(R.id.btn_recargar)
        progressBar = findViewById(R.id.progress_bar)
        
        cargarDatos()
        
        btnRecargar.setOnClickListener {
            val montoStr = etMonto.text.toString()
            if (montoStr.isNotEmpty()) {
                val monto = montoStr.toDoubleOrNull()
                if (monto != null && monto > 0) {
                    recargarSaldo(monto)
                } else {
                    Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun cargarDatos() {
        // Primero mostrar datos guardados localmente
        tvNombre.text = SessionManager.getUserName(this)
        tvEmail.text = SessionManager.getUserEmail(this)
        tvSaldo.text = "$${SessionManager.getUserSaldo(this)}"
        
        // Luego actualizar desde la API
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            when (val result = usuarioRepo.obtenerPerfil()) {
                is Result.Success -> {
                    val usuario = result.data
                    SessionManager.saveUserData(
                        this@ProfileActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )
                    tvNombre.text = usuario.nombre
                    tvEmail.text = usuario.email
                    tvSaldo.text = "$${usuario.saldo}"
                }
                is Result.Error -> {
                    Toast.makeText(this@ProfileActivity, 
                        result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            progressBar.visibility = View.GONE
        }
    }
    
    private fun recargarSaldo(monto: Double) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            btnRecargar.isEnabled = false
            
            when (val result = usuarioRepo.recargarSaldo(monto)) {
                is Result.Success -> {
                    val usuario = result.data
                    SessionManager.saveUserData(
                        this@ProfileActivity,
                        usuario.id,
                        usuario.nombre,
                        usuario.email,
                        usuario.saldo
                    )
                    tvSaldo.text = "$${usuario.saldo}"
                    etMonto.text.clear()
                    Toast.makeText(this@ProfileActivity, 
                        "Saldo recargado exitosamente", 
                        Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(this@ProfileActivity, 
                        "Error: ${result.message}", 
                        Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            
            progressBar.visibility = View.GONE
            btnRecargar.isEnabled = true
        }
    }
}
```

## Notas Importantes

### 1. RecyclerView Adapter
Los ejemplos asumen que tienes layouts `item_producto.xml` con IDs como:
- `tv_producto_nombre`
- `tv_producto_precio`
- `tv_producto_descripcion`

### 2. Layouts Necesarios
Asegúrate de tener en tus layouts:
- `progress_bar` (ProgressBar)
- `tv_error` (TextView para errores)
- `rv_productos` (RecyclerView)

### 3. Imágenes de Productos
Para cargar imágenes, agrega Glide o Coil:

```kotlin
// En build.gradle.kts
implementation("com.github.bumptech.glide:glide:4.16.0")

// En el adapter
Glide.with(context)
    .load(producto.imagenUrl)
    .placeholder(R.drawable.placeholder)
    .into(holder.imageView)
```

### 4. Carrito Global
Considera crear un Singleton para el carrito:

```kotlin
object CartManager {
    private val items = mutableListOf<CartItem>()
    
    fun addItem(item: CartItem) {
        items.add(item)
    }
    
    fun getItems() = items.toList()
    
    fun clear() {
        items.clear()
    }
}
```

¡Listo para implementar! 🚀

