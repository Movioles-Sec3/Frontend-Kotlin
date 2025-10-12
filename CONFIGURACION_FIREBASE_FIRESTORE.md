# 🔥 Configuración de Firebase Firestore para Dashboard de Analytics

## ✅ Cambios Realizados

### 1. Dependencias Actualizadas
Se agregó Firebase Firestore al archivo `build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-firestore-ktx")
```

### 2. AnalyticsLogger.kt Modificado
Ahora todos los eventos se envían a:
- ✅ **Firebase Analytics** (para métricas básicas)
- ✅ **Firebase Firestore** (para dashboard personalizado con todos los datos)
- ✅ **CSV Local** (backup offline)

### 3. Datos que se Envían a Firestore

Cada evento incluye:
- `event_type`: Tipo de evento (menu_ready, payment_completed, app_launch_to_menu)
- `timestamp`: Timestamp en milisegundos
- `timestamp_readable`: Fecha legible (2025-10-11 15:30:45)
- `duration_ms`: Duración del evento
- `network_type`: Tipo de red (Wi-Fi, 4G, 5G, Offline)
- `device_tier`: Nivel del dispositivo (low, mid, high)
- `os_api`: Nivel de API de Android
- `device_model`: Modelo del dispositivo (ej: Pixel 5)
- `device_brand`: Marca (ej: Samsung, Xiaomi)
- `os_version`: Versión de Android (ej: 13)
- `app_version`: Versión de la app
- Campos específicos por evento (screen, success, payment_method, etc.)

---

## 📋 Proceso de Configuración en Firebase Console

### Paso 1: Acceder a Firebase Console

1. Ve a [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Selecciona tu proyecto (o créalo si no existe)
3. Si ya tienes `google-services.json` configurado, Firebase ya está conectado

### Paso 2: Habilitar Cloud Firestore

1. En el menú lateral, haz clic en **"Build"** → **"Firestore Database"**
2. Haz clic en **"Create database"**
3. Selecciona el modo:
   - **Modo de producción** (recomendado para empezar)
   - **Modo de prueba** (permite lectura/escritura sin autenticación por 30 días)

4. Selecciona la ubicación del servidor (ej: `us-central1`, `southamerica-east1`)
   - ⚠️ **Importante**: No podrás cambiar esto después
   - Elige la región más cercana a tus usuarios

5. Haz clic en **"Enable"**

### Paso 3: Configurar Reglas de Seguridad

**Para desarrollo/pruebas (permite escritura sin autenticación):**

Ve a **"Firestore Database"** → **"Rules"** y usa:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir lectura y escritura en analytics_events por 30 días
    match /analytics_events/{document=**} {
      allow read, write: if request.time < timestamp.date(2025, 11, 11);
    }
  }
}
```

**Para producción (solo escritura desde la app, lectura autenticada):**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Solo permitir escritura desde la app, lectura solo para administradores
    match /analytics_events/{document=**} {
      allow write: if true; // La app puede escribir eventos
      allow read: if request.auth != null; // Solo usuarios autenticados pueden leer
    }
  }
}
```

Haz clic en **"Publish"** para guardar las reglas.

### Paso 4: Sincronizar el Proyecto

1. En Android Studio, haz clic en **"Sync Now"** para sincronizar las nuevas dependencias
2. Si hay errores, ve a **File** → **Sync Project with Gradle Files**

### Paso 5: Reconstruir la App

```bash
Build → Clean Project
Build → Rebuild Project
```

Luego instala la app en tu dispositivo.

---

## 🧪 Probar que Funciona

### 1. Ejecutar la App

1. Abre la app en tu celular
2. Navega por las pantallas (esto generará eventos `menu_ready`)
3. Realiza una compra (generará eventos `payment_completed`)

### 2. Verificar en Logcat

Busca en Logcat mensajes como:
```
D/AnalyticsLogger: 🔥 Evento menu_ready guardado en Firestore: ABC123XYZ
D/AnalyticsLogger: 🔥 Evento payment_completed guardado en Firestore: DEF456UVW
```

### 3. Verificar en Firebase Console

1. Ve a **Firestore Database** en Firebase Console
2. Deberías ver una colección llamada **`analytics_events`**
3. Haz clic en ella para ver los documentos
4. Cada documento contiene todos los datos del evento:

```json
{
  "event_type": "menu_ready",
  "timestamp": 1728667845123,
  "timestamp_readable": "2025-10-11 15:30:45",
  "duration_ms": 1234,
  "network_type": "Wi-Fi",
  "device_tier": "high",
  "os_api": 33,
  "device_model": "SM-G998B",
  "device_brand": "samsung",
  "os_version": "13",
  "screen": "HomeActivity",
  "app_version": "1.0"
}
```

---

## 📊 Crear un Dashboard

### Opción 1: Usar Firebase Console (Básico)

1. Ve a **Firestore Database** → Colección `analytics_events`
2. Puedes filtrar por `event_type`, `device_tier`, `network_type`, etc.
3. Exporta datos con el botón de exportación

### Opción 2: Conectar con Google Data Studio / Looker Studio (Recomendado)

1. Ve a [https://lookerstudio.google.com/](https://lookerstudio.google.com/)
2. Crea un nuevo informe
3. Conecta **Firestore** como fuente de datos
4. Arrastra y suelta campos para crear gráficos:
   - **Duración promedio de menu_ready** por `network_type`
   - **Distribución de dispositivos** por `device_tier`
   - **Tasa de éxito de pagos** (success = true/false)
   - **Eventos por hora** usando `timestamp_readable`

### Opción 3: Exportar a BigQuery (Avanzado)

1. En Firebase Console, ve a **Project Settings** → **Integrations**
2. Habilita **BigQuery**
3. Exporta Firestore a BigQuery automáticamente
4. Usa SQL para análisis avanzados

### Opción 4: Dashboard Web Personalizado

Puedes crear un dashboard web usando:
- **React/Vue.js** + Firebase SDK
- **Python** + `firebase-admin` + Streamlit/Dash
- **Node.js** + Express + Chart.js

Ejemplo de query en JavaScript:
```javascript
const db = firebase.firestore();
db.collection('analytics_events')
  .where('event_type', '==', 'menu_ready')
  .where('timestamp', '>', Date.now() - 86400000) // Últimas 24 horas
  .get()
  .then(snapshot => {
    const events = snapshot.docs.map(doc => doc.data());
    // Procesar y graficar...
  });
```

---

## 📈 Métricas Sugeridas para el Dashboard

### Rendimiento
- Duración promedio de `menu_ready` por tipo de red
- Percentiles (P50, P90, P95) de `app_launch_to_menu`
- Comparación de rendimiento entre dispositivos (low/mid/high tier)

### Uso
- Número de eventos por día/hora
- Distribución de tipos de red (Wi-Fi vs 4G vs 5G)
- Dispositivos más usados (brand, model)
- Versiones de Android más comunes

### Pagos
- Tasa de éxito de pagos (success = true/false)
- Métodos de pago más usados
- Duración promedio de proceso de pago

---

## 🔍 Consultas Útiles en Firestore

### Eventos de las últimas 24 horas
```javascript
db.collection('analytics_events')
  .where('timestamp', '>', Date.now() - 86400000)
  .orderBy('timestamp', 'desc')
  .limit(100)
```

### Pagos fallidos
```javascript
db.collection('analytics_events')
  .where('event_type', '==', 'payment_completed')
  .where('success', '==', false)
  .orderBy('timestamp', 'desc')
```

### Dispositivos de gama baja con problemas de rendimiento
```javascript
db.collection('analytics_events')
  .where('device_tier', '==', 'low')
  .where('duration_ms', '>', 3000) // Más de 3 segundos
  .orderBy('duration_ms', 'desc')
```

---

## ⚠️ Consideraciones Importantes

### 1. Costos de Firestore
- **Lectura**: $0.06 por cada 100,000 documentos leídos
- **Escritura**: $0.18 por cada 100,000 documentos escritos
- **Almacenamiento**: $0.18 por GB al mes

**Estimación para 1000 usuarios activos diarios:**
- ~10,000 eventos/día = ~300,000 eventos/mes
- Costo: ~$0.54/mes de escrituras + almacenamiento mínimo
- ✅ Muy económico para dashboards de analytics

### 2. Límites de Firestore
- Máximo 1 millón de documentos en el plan gratuito Spark
- Máximo 10 GB de almacenamiento gratis
- Máximo 50,000 lecturas/día gratis
- Máximo 20,000 escrituras/día gratis

### 3. Offline Support
Firebase Firestore tiene **caché offline automática**:
- Si el usuario no tiene conexión, los eventos se guardan localmente
- Cuando recupera conexión, se sincronizan automáticamente
- ✅ No necesitas configurar nada extra

### 4. Índices
Si creas consultas complejas con múltiples filtros, Firebase te pedirá crear índices compuestos. Simplemente haz clic en el link del error en Logcat y se creará automáticamente.

---

## 🚀 Próximos Pasos

1. ✅ Sincronizar las dependencias (`Sync Now`)
2. ✅ Habilitar Firestore en Firebase Console
3. ✅ Configurar reglas de seguridad
4. ✅ Reconstruir la app (`Clean Project` → `Rebuild Project`)
5. ✅ Probar la app y verificar eventos en Firebase Console
6. 📊 Crear dashboard en Looker Studio o dashboard personalizado
7. 📈 Analizar métricas de rendimiento y uso

---

## 🆘 Solución de Problemas

### Error: "Firebase Firestore has not been initialized"
- Verifica que `google-services.json` esté en `app/`
- Verifica que el plugin de Google Services esté aplicado en `build.gradle.kts`

### Error: "PERMISSION_DENIED: Missing or insufficient permissions"
- Revisa las reglas de seguridad en Firestore
- Asegúrate de permitir `write: if true;` para la colección `analytics_events`

### Los eventos no aparecen en Firestore
- Verifica que el dispositivo tenga conexión a Internet
- Revisa Logcat buscando errores `AnalyticsLogger`
- Verifica que Firestore esté habilitado en Firebase Console

### Eventos duplicados
- Cada llamada a `logMenuReady()` crea un nuevo documento
- Esto es normal, cada evento es único
- Si quieres evitar duplicados, implementa un ID único basado en timestamp + tipo

---

## 📚 Recursos Adicionales

- [Documentación de Firestore](https://firebase.google.com/docs/firestore)
- [Reglas de seguridad de Firestore](https://firebase.google.com/docs/firestore/security/get-started)
- [Looker Studio](https://lookerstudio.google.com/)
- [Precios de Firebase](https://firebase.google.com/pricing)

---

**¡Listo! Ahora tienes un sistema completo de analytics que envía datos a Firebase Firestore para crear dashboards personalizados.** 🎉

