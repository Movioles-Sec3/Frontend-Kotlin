# 📊 User Insights Dashboard - Implementación Completa

## 🎯 Resumen

Se ha implementado una nueva vista **User Insights Dashboard** que muestra estadísticas y patrones de compra del usuario, junto con la implementación de una **Business Question (BQ)** completa.

---

## 🔍 Business Question Implementada

**"¿Cuál es el producto más frecuente en las órdenes del usuario y cuál es su patrón de compra semanal?"**

### Componentes de la BQ:

1. **Frecuencia de Productos**: Identifica los productos más comprados
2. **Patrón Semanal**: Analiza qué días de la semana el usuario hace más pedidos
3. **Gasto por Categoría**: Distribución del gasto del usuario

---

## 📁 Archivos Creados

### 1. **Data Layer**

#### `InsightsDao.kt`
- DAO con queries SQL optimizadas para análisis de datos
- Queries principales:
  - `getMostFrequentProducts()`: Top productos por frecuencia
  - `getWeeklySpendingPattern()`: Patrón de compra semanal
  - `getSpendingByCategory()`: Gasto por categoría
  - Estadísticas generales (total gastado, promedio, etc.)

#### `InsightsModels.kt`
- **ProductFrequency**: Modelo para productos más comprados
- **WeeklySpending**: Modelo para patrón semanal (con función `getDayName()`)
- **CategorySpending**: Modelo para gasto por categoría
- **UserInsights**: Modelo completo de insights del usuario

#### `InsightsRepository.kt`
- Repository que implementa patrón Repository
- Método principal: `getUserInsights(userId)` que retorna `Result<UserInsights>`
- Ejecuta queries en `Dispatchers.IO` para no bloquear UI

### 2. **ViewModel Layer**

#### `UserInsightsViewModel.kt`
- ViewModel con LiveData para observar cambios
- Usa `viewModelScope.launch` para coroutines
- Estados: `Loading`, `Success`, `Error`, `NoData`
- Método `loadUserInsights()` ejecuta la carga en background

### 3. **UI Layer**

#### `UserInsightsActivity.kt`
- Activity con RecyclerViews para listas dinámicas
- Adapters: `TopProductsAdapter` y `WeeklyPatternAdapter`
- Estados de UI: loading, content, empty state, error state
- Usa `viewModels()` delegate para ViewModel

#### `activity_user_insights.xml`
- Layout con CoordinatorLayout y NestedScrollView
- Cards con estadísticas generales:
  - 🛍️ Órdenes Totales
  - 💰 Gasto Total
  - 📈 Promedio por Orden
  - 🎯 Productos Únicos
  - 🏆 Orden Más Grande
- Secciones para BQ:
  - ⭐ Producto Favorito
  - 📅 Día Más Activo
  - 🔝 Top 5 Productos
  - 📊 Patrón Semanal

#### `item_top_product.xml`
- Card para mostrar producto con ranking, nombre, órdenes y gasto

#### `item_weekly_pattern.xml`
- Card para mostrar día de la semana con órdenes y gasto

### 4. **Adapters**

#### `TopProductsAdapter.kt`
- RecyclerView.Adapter con ListAdapter y DiffUtil
- Muestra ranking, nombre del producto, cantidad de órdenes y gasto total

#### `WeeklyPatternAdapter.kt`
- RecyclerView.Adapter con ListAdapter y DiffUtil
- Muestra día de la semana, cantidad de órdenes, gasto total y promedio

### 5. **Database Update**

#### `AppDatabase.kt`
- Agregado `abstract fun insightsDao(): InsightsDao`
- Comentario actualizado indicando 6 capas de almacenamiento

### 6. **UI Integration**

#### `activity_home.xml`
- Agregado nuevo botón "📊 Mis Estadísticas" con color naranja (#FF9800)
- Card Material con efecto de elevación

#### `HomeActivity.kt`
- Agregado listener para `btn_user_insights`
- Navega a `UserInsightsActivity`

#### `AndroidManifest.xml`
- Registrada `UserInsightsActivity`

---

## 🚀 Características Técnicas

### ✅ Uso de Kotlin Coroutines
```kotlin
viewModelScope.launch {
    // Background work with Dispatchers.IO in repository
    when (val result = repository.getUserInsights(userId)) {
        is Result.Success -> { /* update UI */ }
        is Result.Error -> { /* show error */ }
    }
}
```

### ✅ Flow y LiveData
- `LiveData<InsightsState>` para observar cambios de estado
- UI reactiva que se actualiza automáticamente

### ✅ Room Database Queries
- Queries SQL complejas con JOINs
- Agrupación y agregación de datos
- Cálculos de día de la semana desde epoch

### ✅ Repository Pattern
- Separación de capas (Data, Domain, UI)
- Manejo de errores con sealed class `Result`

### ✅ RecyclerView con DiffUtil
- Eficiencia en actualización de listas
- Animaciones suaves

### ✅ Material Design 3
- Cards con elevación y esquinas redondeadas
- CoordinatorLayout con AppBar
- Estados de UI (loading, empty, error)

---

## 📊 Business Question: Análisis Detallado

### Query 1: Productos Más Frecuentes
```sql
SELECT 
    oi.productId,
    oi.name as productName,
    COUNT(DISTINCT oi.orderId) as orderCount,
    SUM(oi.quantity) as totalQuantity,
    SUM(oi.quantity * oi.price) as totalSpent
FROM order_items oi
INNER JOIN orders o ON oi.orderId = o.id
WHERE o.userId = :userId AND o.status != 'CARRITO'
GROUP BY oi.productId, oi.name
ORDER BY orderCount DESC, totalSpent DESC
```

**Insights obtenidos:**
- Productos más populares del usuario
- Frecuencia de compra por producto
- Gasto total por producto

### Query 2: Patrón Semanal
```sql
SELECT 
    CAST(strftime('%w', datetime(createdAt/1000, 'unixepoch')) AS INTEGER) as dayOfWeek,
    COUNT(*) as orderCount,
    SUM(total) as totalSpent,
    AVG(total) as avgOrderValue
FROM orders
WHERE userId = :userId AND status != 'CARRITO'
GROUP BY dayOfWeek
ORDER BY orderCount DESC
```

**Insights obtenidos:**
- Día de la semana más activo
- Patrón de consumo semanal
- Gasto promedio por día

### Query 3: Estadísticas Generales
- Total de órdenes
- Gasto total acumulado
- Valor promedio por orden
- Productos únicos comprados
- Orden más grande

---

## 🎨 Diseño de UI

### Paleta de Colores
- **Naranja** (#FF9800): Botón de acceso en Home
- **Azul** (color_primary): Header principal
- **Blanco**: Texto en cards
- **Material Cards**: Fondo blanco con elevación

### Estados de UI
1. **Loading**: ProgressBar circular centrado
2. **Content**: ScrollView con todos los datos
3. **Empty State**: Emoji 📊 + mensaje amigable
4. **Error State**: Emoji ❌ + botón "Reintentar"

---

## 🔄 Flujo de Datos

```
UserInsightsActivity (UI)
    ↓ observes
UserInsightsViewModel (ViewModel)
    ↓ calls
InsightsRepository (Repository)
    ↓ uses
InsightsDao (Room DAO)
    ↓ queries
AppDatabase (SQLite)
    ↓ reads from
OrderEntity + OrderItemEntity (Local Data)
```

---

## 📱 Cómo Acceder

1. Inicia sesión en la app
2. En la pantalla Home, desplázate hacia abajo
3. Presiona el botón **"📊 Mis Estadísticas"** (color naranja)
4. Verás el dashboard con todas tus estadísticas

---

## 🧪 Testing Recomendado

1. **Sin órdenes**: Verifica que muestre el estado vacío
2. **Con pocas órdenes**: Verifica que las estadísticas sean correctas
3. **Con muchas órdenes**: Verifica el rendimiento de las queries
4. **Offline**: Todo funciona offline (usa datos locales de Room)

---

## 🎯 Beneficios para el Usuario

✅ **Conocimiento de hábitos**: Ve cuánto gasta y con qué frecuencia  
✅ **Productos favoritos**: Identifica sus preferencias  
✅ **Patrones temporales**: Sabe qué días compra más  
✅ **Datos históricos**: Todo basado en su historial real  
✅ **Funciona offline**: No requiere conexión a internet  

---

## 🚀 Mejoras Futuras Sugeridas

1. **Gráficos visuales**: Agregar charts con MPAndroidChart
2. **Comparativa temporal**: Mes actual vs mes anterior
3. **Recomendaciones**: Sugerir productos basados en patrones
4. **Exportar datos**: Permitir exportar estadísticas a PDF
5. **Filtros temporales**: Ver estadísticas por rango de fechas
6. **Achievements**: Gamificación con logros por compras

---

## 📝 Notas Técnicas

- **Version de Base de Datos**: Mantiene versión 6 (no requiere migración)
- **Compatibilidad**: Android API 24+ (Android 7.0+)
- **Performance**: Queries optimizadas con índices en Room
- **Memoria**: Uso eficiente con DiffUtil en RecyclerViews

---

## ✅ Checklist de Implementación

- [x] Crear modelos de datos para estadísticas
- [x] Implementar DAO con queries de Business Questions
- [x] Crear Repository con patrón Result
- [x] Implementar ViewModel con LiveData
- [x] Diseñar layouts XML responsivos
- [x] Crear adapters para RecyclerViews
- [x] Integrar en HomeActivity
- [x] Registrar en AndroidManifest
- [x] Validar compilación sin errores
- [x] Documentar implementación completa

---

**Implementado por**: GitHub Copilot  
**Fecha**: 2025-01-04  
**Arquitectura**: MVVM con Repository Pattern  
**Tecnologías**: Kotlin, Room, Coroutines, LiveData, Material Design 3

