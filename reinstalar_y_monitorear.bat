@echo off
echo.
echo ============================================
echo   REINSTALACION CON CODIGO CSV
echo ============================================
echo.

echo [Paso 1] Desinstalando version anterior...
adb uninstall app.src
timeout /t 2 /nobreak >nul

echo.
echo [Paso 2] Instalando version con CSV...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if errorlevel 1 (
    echo.
    echo ERROR: No se pudo instalar la app.
    echo Verifica que ADB este funcionando.
    pause
    exit /b 1
)

echo.
echo [Paso 3] Limpiando logs anteriores...
adb logcat -c

echo.
echo ============================================
echo   INSTALACION COMPLETADA
echo ============================================
echo.
echo AHORA DEBES:
echo   1. Abrir la app en tu dispositivo
echo   2. Iniciar sesion
echo   3. Esperar 3 segundos en la pantalla principal
echo.
echo Mientras usas la app, este script mostrara los logs
echo en tiempo real para que veas cuando se crea el CSV.
echo.
echo Presiona cualquier tecla cuando hayas abierto la app...
pause >nul

echo.
echo ============================================
echo   MONITOREANDO EVENTOS (Ctrl+C para salir)
echo ============================================
echo.

adb logcat -s HomeActivity OrderSummaryActivity AnalyticsLogger CSVEventLogger

pause

