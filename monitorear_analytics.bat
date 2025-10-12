@echo off
chcp 65001 >nul
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║         📊 MONITOR DE ANALYTICS - FIREBASE FIRESTORE              ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.
echo Iniciando monitoreo de eventos de Analytics...
echo.
echo 🔍 Filtrando logs de:
echo    - AnalyticsLogger (eventos enviados a Firebase)
echo    - CSVEventLogger (eventos guardados en CSV)
echo    - Firebase Firestore (confirmaciones de guardado)
echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

REM Verificar que ADB esté disponible
adb version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: ADB no está instalado o no está en el PATH
    echo.
    echo Por favor, asegúrate de que Android SDK esté instalado y configurado.
    echo Ubicación típica: C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools
    pause
    exit /b 1
)

REM Verificar que haya un dispositivo conectado
adb devices | findstr /r "device$" >nul
if errorlevel 1 (
    echo ⚠️  ADVERTENCIA: No hay dispositivos conectados
    echo.
    echo Por favor, conecta tu dispositivo Android o inicia el emulador.
    echo.
    pause
    exit /b 1
)

echo ✅ Dispositivo conectado detectado
echo.
echo 📱 Monitoreando logs en tiempo real...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

REM Limpiar el buffer de logcat y empezar a monitorear
adb logcat -c
adb logcat -v time -s AnalyticsLogger:D CSVEventLogger:D FirebaseFirestore:D FirebaseAnalytics:D

