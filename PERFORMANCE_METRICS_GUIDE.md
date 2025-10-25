# 📊 Sistema de Medición de Rendimiento: Carga Paralela vs. Secuencial

## 🎯 Objetivo

Este sistema mide y compara el tiempo que tarda la aplicación desde que se abre hasta que el menú está completamente usable, comparando dos estrategias:

1. **Carga PARALELA**: El catálogo de productos y las imágenes se cargan simultáneamente
2. **Carga SECUENCIAL**: Primero se carga el catálogo, luego las imágenes

## 📈 Métricas Medidas

- **P50 (Percentil 50 / Mediana)**: El tiempo que el 50% de las cargas NO superan
- **P95 (Percentil 95)**: El tiempo que el 95% de las cargas NO superan
- **Min/Max/Avg**: Tiempos mínimo, máximo y promedio
- **Tiempo de catálogo**: Cuánto tarda en cargar los datos de productos
- **Tiempo de imágenes**: Cuánto tarda en precargar las imágenes
- **Tiempo total**: Tiempo completo de la operación
- **Tiempo menú listo**: Cuándo el menú es realmente usable para el usuario

## 🚀 Cómo Funciona

### 1. Sistema Automático de Alternancia

El sistema **alterna automáticamente** entre carga paralela y secuencial cada vez que se recarga el HomeActivity. Esto permite recolectar datos de ambos métodos para compararlos.

```kotlin
// Primera carga: PARALELA
// Segunda carga: SECUENCIAL
// Tercera carga: PARALELA
// Y así sucesivamente...
```

### 2. Recolección de Datos

Cada vez que se cargan los productos recomendados, el sistema:

1. ✅ Mide el tiempo de carga del catálogo
2. ✅ Mide el tiempo de carga de imágenes
3. ✅ Registra el tipo de red (Wi-Fi, 4G, 5G)
4. ✅ Detecta el tier del dispositivo (low/mid/high)
5. ✅ Guarda las métricas localmente
6. ✅ Envía las métricas a Firebase Analytics y Firestore

### 3. Almacenamiento

Las métricas se guardan en:
- **SharedPreferences local**: Hasta 100 muestras por método
- **Firebase Firestore**: Colección `performance_metrics`
- **Firebase Analytics**: Eventos con nombre `menu_load_performance`

## 📱 Cómo Usar

### Acceder al Dashboard de Métricas

1. Abre la aplicación
2. En la pantalla principal (HomeActivity)
3. **Mantén presionado** el texto del balance (saldo)
4. Aparecerá un diálogo con información de analytics
5. Presiona el botón **"📊 Performance"**
6. Se abrirá el dashboard de métricas

### Generar Datos para Comparar

Para obtener resultados precisos, necesitas recolectar varias muestras:

1. **Mata y reinicia la app** varias veces (mínimo 10-20 veces)
2. Cada reinicio alternará automáticamente entre carga paralela y secuencial
3. Prueba en diferentes condiciones:
   - Con Wi-Fi
   - Con datos móviles (4G/5G)
   - Con conexión lenta
   - Con conexión rápida

### Ver Resultados

En el dashboard verás algo como:

```
╔═══════════════════════════════════════════════════════╗
║     REPORTE DE RENDIMIENTO - CARGA PARALELA vs SECUENCIAL     ║
╚═══════════════════════════════════════════════════════╝

🚀 CARGA PARALELA (Catálogo + Imágenes simultáneas)
   Muestras: 15
   P50 (mediana): 450ms
   P95: 680ms
   Min: 320ms | Max: 750ms | Avg: 470ms
   ✅ Mejora: 35.7% más rápido

📦 CARGA SECUENCIAL (Catálogo → Imágenes)
   Muestras: 15
   P50 (mediana): 700ms
   P95: 920ms
   Min: 580ms | Max: 1100ms | Avg: 730ms

📊 COMPARACIÓN
   Tiempo ahorrado (P50): 250ms
   Mejora: 35.7%
   Tiempo ahorrado (P95): 240ms
```

## 🔧 Archivos Creados

### 1. PerformanceMetrics.kt
Sistema de medición y cálculo de percentiles.
- Registra mediciones
- Calcula P50, P95, min, max, avg
- Compara ambos métodos
- Genera reportes legibles

### 2. ImagePreloader.kt
Sistema de precarga de imágenes con caché LRU.
- Precarga paralela
- Precarga secuencial
- Caché en memoria inteligente
- Métricas de caché (hits/misses)

### 3. HomeViewModel.kt (modificado)
- Soporte para carga paralela/secuencial
- Medición automática de tiempos
- Alternancia automática de métodos
- Registro de métricas

### 4. PerformanceMetricsActivity.kt
Pantalla para visualizar el dashboard.
- Muestra comparación detallada
- Botón para actualizar métricas
- Botón para limpiar datos
- Información sobre percentiles

### 5. activity_performance_metrics.xml
Layout del dashboard de métricas.

## 📊 Interpretando los Resultados

### ¿Qué método es mejor?

- **Si P50 paralelo < P50 secuencial**: La carga paralela es más rápida
- **Porcentaje de mejora**: Cuánto más rápido es un método vs. el otro
- **P95**: Importante para ver el peor caso (experiencias lentas)

### Ejemplo de Interpretación

```
P50 Paralelo: 450ms
P50 Secuencial: 700ms
Mejora: 35.7%
```

Esto significa que:
- ✅ El 50% de los usuarios verán el menú listo en menos de 450ms (paralelo)
- ⚠️ Con el método secuencial, ese mismo 50% esperaría 700ms
- 🚀 La carga paralela ahorra 250ms (35.7% más rápida)

### Variables que Afectan el Rendimiento

1. **Tipo de red**: Wi-Fi es más rápido que 4G
2. **Tier del dispositivo**: Dispositivos high-end procesan más rápido
3. **Número de imágenes**: Más imágenes = más tiempo
4. **Tamaño de imágenes**: Imágenes grandes tardan más en cargarse
5. **Latencia de red**: Conexiones con alta latencia afectan más a la carga secuencial

## 🔬 Casos de Uso

### Escenario 1: Conexión Rápida (Wi-Fi)
**Hipótesis**: La carga paralela debería ser significativamente más rápida.
**Por qué**: Puede descargar múltiples imágenes simultáneamente.

### Escenario 2: Conexión Lenta (3G/4G débil)
**Hipótesis**: La diferencia podría ser menor.
**Por qué**: El ancho de banda limitado puede saturarse en ambos casos.

### Escenario 3: Dispositivo Low-End
**Hipótesis**: La carga paralela podría tener overhead adicional.
**Por qué**: Múltiples hilos compiten por recursos limitados.

## 🎛️ Configuración Avanzada

### Cambiar el Número de Productos Recomendados

En `HomeViewModel.kt`:
```kotlin
val productosLimitados = result.data.take(5) // Cambiar 5 por el número deseado
```

### Forzar un Método Específico

En `HomeViewModel.kt`:
```kotlin
companion object {
    var useParallelLoading = true // false para secuencial siempre
}

// Y comentar esta línea en cargarProductosRecomendados():
// useParallelLoading = !useParallelLoading
```

### Cambiar el Tamaño del Caché de Imágenes

En `ImagePreloader.kt`:
```kotlin
val cacheSize = maxMemory / 8 // Cambiar 8 por otro divisor (4, 16, etc.)
```

## 📱 Firebase Integration

### Ver Datos en Firebase

1. **Firebase Analytics**:
   - Abre Firebase Console
   - Ve a Analytics → Events
   - Busca el evento `menu_load_performance`
   - Verás todas las métricas por carga

2. **Firebase Firestore**:
   - Abre Firebase Console
   - Ve a Firestore Database
   - Busca la colección `performance_metrics`
   - Cada documento tiene una medición completa

### Queries Útiles en Firestore

Ejemplo de query para obtener métricas de carga paralela:
```javascript
db.collection('performance_metrics')
  .where('load_type', '==', 'PARALLEL')
  .orderBy('timestamp', 'desc')
  .limit(100)
```

## 🐛 Troubleshooting

### No veo datos en el dashboard
1. Asegúrate de haber reiniciado la app varias veces
2. Verifica que los logs muestren "🚀 Iniciando carga PARALELA" o "📦 Iniciando carga SECUENCIAL"
3. Revisa los logs con el tag "HomeViewModel" y "PerformanceMetrics"

### Los tiempos parecen iguales
1. Puede ser que las imágenes ya estén en caché
2. Limpia el caché de la app: Settings → Apps → TapAndToast → Clear Cache
3. Prueba con diferentes conexiones de red

### El dashboard muestra "Se necesitan más muestras"
1. Necesitas al menos 1 muestra de cada método
2. Reinicia la app más veces para recolectar datos

## 📊 Exportar Datos

Los datos también se guardan en SharedPreferences con el formato:
```
Key: PARALLEL_measurements o SEQUENTIAL_measurements
Format: timestamp,loadType,catalogTime,imagesTime,totalTime,menuReadyTime,productCount,networkType,deviceTier
```

Puedes extraerlos con:
```bash
adb shell run-as app.src cat /data/data/app.src/shared_prefs/performance_metrics.xml
```

## 🎯 Conclusión

Este sistema te permite:
1. ✅ Medir objetivamente cuál método de carga es más rápido
2. ✅ Entender cómo diferentes condiciones afectan el rendimiento
3. ✅ Tomar decisiones basadas en datos reales
4. ✅ Optimizar la experiencia del usuario

**Recomendación**: Usa el método que tenga el **P50 y P95 más bajos** en las condiciones más comunes de tus usuarios (ej: Wi-Fi, dispositivos mid-tier).

