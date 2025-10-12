# 📊 GUÍA DE USO - MONITORES DE ANALYTICS

## 🎯 Archivos Creados

Se han creado **4 herramientas** para monitorear los eventos de Analytics que se envían a Firebase Firestore:

### 1️⃣ `MONITOR_ANALYTICS.bat` ⭐ **RECOMENDADO**
**Herramienta visual completa con menú interactivo**

#### Características:
- ✅ Interfaz visual con colores
- ✅ Menú interactivo con 6 opciones
- ✅ Estadísticas detalladas con gráficos ASCII
- ✅ Análisis de rendimiento
- ✅ Descarga automática de archivos CSV

#### Cómo usarlo:
```bash
# Opción 1: Doble clic en el archivo
MONITOR_ANALYTICS.bat

# Opción 2: Desde la terminal
.\MONITOR_ANALYTICS.bat
```

#### Opciones del menú:
1. **📡 Monitorear logs en tiempo real** - Ver eventos mientras ocurren
2. **📄 Descargar y mostrar CSV** - Descargar el archivo de eventos del dispositivo
3. **📊 Estadísticas detalladas** - Ver análisis completo con gráficos
4. **🔥 Ver eventos de Firebase** - Solo eventos enviados a Firestore
5. **📈 Análisis gráfico** - Distribución de duraciones con gráfico ASCII
6. **🔄 Modo completo** - Ejecutar todo automáticamente

---

### 2️⃣ `monitorear_analytics.bat`
**Monitor simple en tiempo real**

#### Características:
- ✅ Monitoreo directo de Logcat
- ✅ Filtrado automático de eventos de Analytics
- ✅ Ejecución rápida

#### Cómo usarlo:
```bash
monitorear_analytics.bat
```

#### Qué verás:
```
10-11 15:30:45.123 D/AnalyticsLogger: ✅ Evento menu_ready enviado a Firebase Analytics
10-11 15:30:45.456 D/AnalyticsLogger: 🔥 Evento menu_ready guardado en Firestore: ABC123XYZ
10-11 15:30:45.789 D/CSVEventLogger: 📄 Evento menu_ready guardado en CSV
```

---

### 3️⃣ `ver_analytics_completo.bat`
**Herramienta avanzada con múltiples opciones**

#### Características:
- ✅ Menú con 5 opciones
- ✅ Descarga de CSV
- ✅ Estadísticas básicas
- ✅ Modo completo

#### Cómo usarlo:
```bash
ver_analytics_completo.bat
```

#### Opciones:
1. Monitorear logs en tiempo real
2. Descargar y mostrar CSV
3. Mostrar estadísticas
4. Ver eventos de Firebase
5. Modo completo

---

### 4️⃣ `monitor_analytics.ps1`
**Script PowerShell con visualización avanzada**

Este es el script que ejecuta `MONITOR_ANALYTICS.bat`. Puedes ejecutarlo directamente si prefieres PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File monitor_analytics.ps1
```

---

## 🚀 Guía de Uso Rápido

### Escenario 1: "Quiero ver eventos en tiempo real"

1. Conecta tu dispositivo Android o inicia el emulador
2. Ejecuta `MONITOR_ANALYTICS.bat`
3. Selecciona opción **[1]** (Monitorear logs en tiempo real)
4. Usa la app en tu dispositivo
5. Verás los eventos aparecer en tiempo real con colores:
   - 🟢 **Verde** = eventos menu_ready
   - 🔵 **Cyan** = eventos payment_completed
   - 🟣 **Magenta** = confirmaciones de Firebase Firestore
   - 🔴 **Rojo** = errores

### Escenario 2: "Quiero ver estadísticas de rendimiento"

1. Ejecuta `MONITOR_ANALYTICS.bat`
2. Selecciona opción **[2]** (Descargar CSV)
3. Luego selecciona opción **[3]** (Estadísticas)
4. Verás:
   - Total de eventos
   - Distribución por tipo de red (Wi-Fi, 4G, 5G)
   - Distribución por nivel de dispositivo (low/mid/high)
   - Duración promedio, mínima y máxima
   - Últimos 5 eventos

### Escenario 3: "Quiero análisis completo automático"

1. Ejecuta `MONITOR_ANALYTICS.bat`
2. Selecciona opción **[6]** (Modo completo)
3. El script automáticamente:
   - Descarga el CSV
   - Muestra estadísticas rápidas
   - Inicia monitoreo en tiempo real

### Escenario 4: "Solo quiero verificar que lleguen a Firebase"

1. Ejecuta `MONITOR_ANALYTICS.bat`
2. Selecciona opción **[4]** (Ver eventos de Firebase)
3. Verás solo las confirmaciones de Firestore:
   ```
   🔥 Evento menu_ready guardado en Firestore: ABC123
   🔥 Evento payment_completed guardado en Firestore: DEF456
   ```

---

## 📊 Interpretación de los Datos

### Eventos que verás:

#### 1. **menu_ready**
Evento cuando el menú principal está listo para usar.

**Campos importantes:**
- `duration_ms`: Tiempo que tardó en cargar (meta: < 2000ms)
- `network_type`: Wi-Fi, 4G, 5G
- `device_tier`: low, mid, high
- `screen`: HomeActivity

**Ejemplo:**
```
duration_ms: 1234
network_type: Wi-Fi
device_tier: high
screen: HomeActivity
```

#### 2. **payment_completed**
Evento cuando se completa un proceso de pago.

**Campos importantes:**
- `duration_ms`: Tiempo del proceso de pago
- `success`: true/false
- `payment_method`: Método de pago usado

**Ejemplo:**
```
duration_ms: 2345
success: true
payment_method: balance
```

#### 3. **app_launch_to_menu**
Tiempo total desde que se abre la app hasta que el menú está listo.

**Campos importantes:**
- `duration_ms`: Tiempo total de arranque (meta: < 3000ms)

---

## 📈 Métricas Clave a Monitorear

### ✅ Bueno
- `menu_ready` < 2000ms en Wi-Fi
- `menu_ready` < 3000ms en 4G/5G
- `payment_completed` con `success: true` > 95%

### ⚠️ Aceptable
- `menu_ready` entre 2000-3000ms en Wi-Fi
- `menu_ready` entre 3000-5000ms en 4G/5G
- `payment_completed` con `success: true` entre 90-95%

### 🔴 Problemas
- `menu_ready` > 3000ms en Wi-Fi
- `menu_ready` > 5000ms en 4G/5G
- `payment_completed` con `success: true` < 90%

---

## 🔍 Solución de Problemas

### Problema: "❌ ERROR: ADB no está disponible"

**Solución:**
1. Asegúrate de tener Android SDK instalado
2. Agrega ADB al PATH:
   ```
   C:\Users\TU_USUARIO\AppData\Local\Android\Sdk\platform-tools
   ```
3. Reinicia la terminal

### Problema: "⚠️ No hay dispositivos conectados"

**Solución:**
1. Conecta tu dispositivo Android por USB
2. Habilita "Depuración USB" en el dispositivo
3. O inicia el emulador de Android Studio

### Problema: "No se pudo descargar el CSV"

**Solución:**
1. Asegúrate de que la app esté instalada y se haya ejecutado
2. Navega por la app para generar eventos
3. Verifica que el package name sea correcto: `app.src`
4. Intenta manualmente:
   ```bash
   adb shell ls /sdcard/Android/data/app.src/files/
   ```

### Problema: "Los eventos no aparecen en Firebase"

**Solución:**
1. Verifica que Firestore esté habilitado en Firebase Console
2. Revisa las reglas de seguridad (deben permitir `write: if true`)
3. Asegúrate de que el dispositivo tenga conexión a Internet
4. Revisa el Logcat buscando errores:
   ```
   ❌ Error al guardar en Firestore
   ```

---

## 💡 Consejos de Uso

### 1. Mantén el monitor abierto mientras usas la app
Ejecuta el monitor en una ventana y la app en el dispositivo. Verás los eventos en tiempo real.

### 2. Descarga el CSV periódicamente
El CSV es un backup local. Descárgalo regularmente para análisis offline.

### 3. Usa el modo completo para debugging
La opción [6] es perfecta para sesiones de debugging porque muestra todo.

### 4. Filtra por tipo de evento
En el CSV puedes buscar patrones:
```bash
findstr "payment_completed" analytics_events.csv
```

### 5. Compara rendimiento
Compara duraciones entre dispositivos, redes y versiones de la app.

---

## 📁 Archivos Generados

### `analytics_events.csv`
Archivo descargado del dispositivo con todos los eventos.

**Ubicación:** `C:\Users\USUARIO\Desktop\Android\Frontend-Kotlin\analytics_events.csv`

**Formato:**
```csv
timestamp,event_name,duration_ms,network_type,device_tier,os_api,success,payment_method,screen,app_version,device_model,android_version
2025-10-11 15:30:45.123,menu_ready,1234,Wi-Fi,high,33,,,HomeActivity,1.0,Samsung SM-G998B,Android 13 (API 33)
```

---

## 🔗 Integración con Firebase Console

### Ver eventos en Firebase Console:

1. Ve a [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database**
4. Busca la colección **`analytics_events`**
5. Verás todos los eventos con todos los campos

### Crear consultas personalizadas:

En la consola de Firebase puedes filtrar por:
- `event_type` = "menu_ready"
- `network_type` = "Wi-Fi"
- `device_tier` = "low"
- `success` = false
- etc.

---

## 📚 Próximos Pasos

1. ✅ **Ejecuta uno de los monitores** para verificar que funciona
2. ✅ **Usa la app** para generar eventos
3. ✅ **Revisa las estadísticas** para identificar problemas de rendimiento
4. ✅ **Configura alertas** en Firebase Console para eventos específicos
5. ✅ **Crea un dashboard** en Looker Studio para visualización avanzada

---

## 🆘 Soporte

Si tienes problemas:

1. Revisa la sección "Solución de Problemas" arriba
2. Verifica el Logcat completo con:
   ```bash
   adb logcat > logcat_completo.txt
   ```
3. Revisa los logs de Firebase en la consola

---

**¡Listo! Ahora tienes herramientas completas para monitorear todos los eventos de Analytics que se envían a Firebase Firestore.** 🎉

