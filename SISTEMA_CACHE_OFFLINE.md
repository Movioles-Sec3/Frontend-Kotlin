# 🔄 SISTEMA DE CACHÉ OFFLINE - FIREBASE FIRESTORE

## ✅ Sistema Implementado

Se ha implementado un **sistema completo de caché offline** que garantiza que **NINGÚN evento se pierda**, incluso sin conexión a Internet.

---

## 🎯 Cómo Funciona

### Flujo Completo

```
Usuario usa la app
      ↓
Evento generado (menu_ready, payment_completed, etc.)
      ↓
¿Hay Internet? ───┐
      ↓           │
     SÍ          NO
      ↓           ↓
Enviar a ──→  Guardar en
Firestore      Room Database
      ↓           ↓
   ✅ OK      💾 Cache Local
      │           ↓
      │      Esperar Internet
      │           ↓
      │      WorkManager detecta
      │      conexión
      │           ↓
      └──────→ Sincronizar
                  ↓
             ✅ Enviado a Firestore
```

---

## 📦 Componentes del Sistema

### 1. **OfflineEvent.kt** - Entidad de Base de Datos
Define la estructura de los eventos guardados localmente.

**Campos:**
- `id`: ID único del evento
- `eventType`: Tipo de evento (menu_ready, payment_completed, etc.)
- `timestamp`: Timestamp en milisegundos
- `timestampReadable`: Fecha legible
- `durationMs`: Duración del evento
- `networkType`: Wi-Fi, 4G, 5G, Offline
- `deviceTier`: low, mid, high
- `osApi`: Nivel de API de Android
- `deviceModel`: Modelo del dispositivo
- `deviceBrand`: Marca del dispositivo
- `osVersion`: Versión de Android
- `isSynced`: true/false - ¿Ya se envió a Firebase?
- `retryCount`: Número de intentos de sincronización
- `lastError`: Último error si falló

### 2. **OfflineEventDao.kt** - Acceso a Datos
Métodos para interactuar con la base de datos:
- `insert()`: Guardar nuevo evento
- `getPendingEvents()`: Obtener eventos NO sincronizados
- `markAsSynced()`: Marcar evento como enviado a Firebase
- `updateRetryInfo()`: Actualizar contador de reintentos
- `deleteSyncedOlderThan()`: Limpiar eventos antiguos
- `getPendingCount()`: Contar eventos pendientes

### 3. **AnalyticsDatabase.kt** - Base de Datos Room
Singleton que gestiona la base de datos SQLite local.

### 4. **SyncOfflineEventsWorker.kt** - Sincronización Automática
Worker de Android WorkManager que:
- Se ejecuta **automáticamente cuando hay Internet**
- Envía todos los eventos pendientes a Firestore
- Reintenta si algo falla
- Limpia eventos antiguos ya sincronizados

### 5. **AnalyticsLogger.kt** - Logger Principal (Modificado)
Ahora incluye:
- Detección de conexión a Internet
- Lógica de caché offline
- Programación de sincronización automática

---

## 🚀 Escenarios de Uso

### Escenario 1: Con Internet ✅

```kotlin
// Usuario navega por la app
AnalyticsLogger.logMenuReady(context, 1234)
```

**Resultado:**
1. ✅ Evento enviado a Firebase Analytics
2. 🔥 Evento enviado a Firestore (en la nube)
3. 📄 Evento guardado en CSV local (backup)

**Logs:**
```
✅ Evento menu_ready enviado a Firebase Analytics
🔥 Evento menu_ready guardado en Firestore: ABC123XYZ
📄 Evento menu_ready guardado en CSV
```

---

### Escenario 2: Sin Internet ❌→✅

```kotlin
// Usuario navega sin conexión
AnalyticsLogger.logMenuReady(context, 1234)
```

**Resultado (SIN Internet):**
1. ⚠️ Sin conexión detectada
2. 💾 Evento guardado en Room Database (caché local)
3. 📄 Evento guardado en CSV local (backup)

**Logs:**
```
⚠️ Sin conexión a Internet - Guardando evento menu_ready en caché offline
💾 Evento menu_ready guardado en caché offline (ID: 1)
📦 Total eventos pendientes: 1
📄 Evento menu_ready guardado en CSV
```

**Resultado (CUANDO REGRESA Internet):**
1. 🔄 WorkManager detecta conexión automáticamente
2. 📤 Envía todos los eventos pendientes a Firestore
3. ✅ Marca eventos como sincronizados

**Logs:**
```
🔄 Iniciando sincronización automática de eventos pendientes
📦 Sincronizando 5 eventos pendientes
🔥 Evento menu_ready (ID: 1) sincronizado: XYZ789
🔥 Evento payment_completed (ID: 2) sincronizado: ABC456
...
🎉 Sincronización completada: 5 exitosos, 0 fallidos
```

---

## 🧪 Probar el Sistema Offline

### Test 1: Generar Eventos Sin Internet

1. **Desactiva Wi-Fi y datos móviles** en tu celular
2. Abre la app
3. Navega por el menú, haz pagos, etc.
4. Ejecuta el monitor:
   ```bash
   MONITOR_ANALYTICS.bat
   ```
5. Selecciona opción **[1]** (Monitorear logs)

**Deberías ver:**
```
⚠️ Sin conexión a Internet - Guardando evento menu_ready en caché offline
💾 Evento menu_ready guardado en caché offline (ID: 1)
📦 Total eventos pendientes: 1

💾 Evento payment_completed guardado en caché offline (ID: 2)
📦 Total eventos pendientes: 2
```

---

### Test 2: Sincronización Automática

1. **Reconecta Wi-Fi o datos móviles**
2. El sistema sincroniza automáticamente (en 15-30 segundos)
3. En el monitor verás:

```
🔄 Iniciando sincronización automática de eventos pendientes
📦 Sincronizando 2 eventos pendientes
🔥 Evento menu_ready (ID: 1) sincronizado: ABC123
🔥 Evento payment_completed (ID: 2) sincronizado: DEF456
🎉 Sincronización completada: 2 exitosos, 0 fallidos
```

4. Verifica en **Firebase Console** → **Firestore Database** → `analytics_events`
   - Deberías ver los 2 eventos que se generaron offline

---

## 📊 Ver Eventos Pendientes

### Desde Logcat

```bash
# Ejecutar el monitor
MONITOR_ANALYTICS.bat

# Opción [1] - Ver logs en tiempo real
# Busca líneas con:
📦 Total eventos pendientes: X
```

### Desde la Base de Datos Directamente

Crea un script `ver_eventos_pendientes.bat`:

```batch
@echo off
echo ========================================
echo EVENTOS PENDIENTES DE SINCRONIZACION
echo ========================================
echo.

REM Conectar al dispositivo y consultar la base de datos
adb shell "su -c 'sqlite3 /data/data/app.src/databases/analytics_database \"SELECT id, eventType, timestamp, isSynced FROM offline_events WHERE isSynced = 0;\"'"

echo.
echo ========================================
pause
```

---

## 🔍 Verificar Estado del Sistema

### Opción 1: Logs en Tiempo Real

```bash
MONITOR_ANALYTICS.bat
# Opción [1]
```

Verás:
- 💾 Eventos guardados en caché
- 📦 Total de eventos pendientes
- 🔄 Sincronizaciones automáticas
- 🔥 Eventos enviados a Firestore

### Opción 2: Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. **Firestore Database** → `analytics_events`
3. Verás todos los eventos (incluso los que se enviaron después de estar offline)

---

## ⚙️ Configuración Avanzada

### Cambiar Frecuencia de Sincronización

En `AnalyticsLogger.kt`, función `scheduleSyncWork()`:

```kotlin
val syncRequest = OneTimeWorkRequestBuilder<SyncOfflineEventsWorker>()
    .setConstraints(constraints)
    .setInitialDelay(5, TimeUnit.SECONDS) // ← Agregar delay inicial
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        15, TimeUnit.SECONDS // ← Cambiar intervalo de reintentos
    )
    .build()
```

### Cambiar Tiempo de Retención

En `SyncOfflineEventsWorker.kt`:

```kotlin
// Limpiar eventos sincronizados de más de X días
val daysToKeep = 7 // ← Cambiar aquí
val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000)
offlineEventDao.deleteSyncedOlderThan(cutoffTime)
```

---

## 📈 Métricas del Sistema

### Ver Estadísticas de Caché

En `OfflineEventDao.kt` puedes agregar:

```kotlin
@Query("SELECT COUNT(*) FROM offline_events WHERE isSynced = 1")
suspend fun getSyncedCount(): Int

@Query("SELECT AVG(retryCount) FROM offline_events WHERE isSynced = 1")
suspend fun getAverageRetries(): Double
```

---

## 🛡️ Ventajas del Sistema

### ✅ Garantías

1. **NINGÚN evento se pierde** - Aunque no haya Internet por días
2. **Sincronización automática** - No requiere intervención manual
3. **Triple backup**:
   - Room Database (caché local persistente)
   - CSV local (backup adicional)
   - Firestore (nube)

4. **Reintentos inteligentes** - Política exponencial de backoff
5. **Limpieza automática** - No acumula datos innecesarios
6. **Eficiente** - Solo sincroniza con Internet disponible

### ⚡ Rendimiento

- **Sin bloqueo de UI** - Todas las operaciones en background
- **Bajo consumo de batería** - WorkManager optimiza la ejecución
- **Bajo uso de datos** - Solo sincroniza cuando hay Wi-Fi (configurable)

---

## 🔧 Solución de Problemas

### Problema: Eventos no se sincronizan

**Verificar:**
1. Que el dispositivo tenga Internet
2. Que Firestore esté habilitado en Firebase Console
3. Logs de WorkManager:
   ```
   adb logcat | findstr "SyncOfflineEventsWorker"
   ```

### Problema: Base de datos no se crea

**Verificar:**
1. Que las dependencias estén sincronizadas
2. Que KSP esté procesando anotaciones de Room
3. Rebuild el proyecto:
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

### Problema: Demasiados eventos pendientes

**Solución manual:**

```kotlin
// En HomeActivity o donde quieras forzar sincronización
lifecycleScope.launch {
    val database = AnalyticsDatabase.getDatabase(this@HomeActivity)
    val pendingCount = database.offlineEventDao().getPendingCount()
    Log.d("DEBUG", "Eventos pendientes: $pendingCount")
    
    // Forzar sincronización
    WorkManager.getInstance(this@HomeActivity)
        .enqueueUniqueWork(
            "sync_offline_events",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncOfflineEventsWorker>().build()
        )
}
```

---

## 📚 Próximos Pasos

1. ✅ **Sincroniza el proyecto** en Android Studio
2. ✅ **Rebuild** la app (Clean + Rebuild)
3. ✅ **Instala** en tu dispositivo
4. ✅ **Prueba sin Internet** - Desconecta Wi-Fi/datos
5. ✅ **Genera eventos** - Navega por la app
6. ✅ **Reconecta Internet** - Espera 30 segundos
7. ✅ **Verifica en Firebase** - Los eventos deberían estar ahí

---

**¡El sistema de caché offline está completamente implementado y listo para usar!** 🎉

Todos los eventos se guardan localmente si no hay Internet y se sincronizan automáticamente cuando regresa la conexión.

