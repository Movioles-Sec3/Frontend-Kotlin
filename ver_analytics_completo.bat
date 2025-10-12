@echo off
chcp 65001 >nul
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║      📊 VISUALIZADOR COMPLETO DE ANALYTICS - FIREBASE              ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.
echo Selecciona una opción:
echo.
echo [1] 📡 Monitorear logs en tiempo real (Logcat)
echo [2] 📄 Descargar y mostrar archivo CSV de eventos
echo [3] 📊 Mostrar estadísticas de eventos
echo [4] 🔥 Ver eventos enviados a Firebase Firestore
echo [5] 🔄 Todo lo anterior (modo completo)
echo [0] ❌ Salir
echo.
set /p opcion="Selecciona una opción [0-5]: "

if "%opcion%"=="1" goto monitor_logs
if "%opcion%"=="2" goto download_csv
if "%opcion%"=="3" goto show_stats
if "%opcion%"=="4" goto firebase_events
if "%opcion%"=="5" goto complete_mode
if "%opcion%"=="0" exit /b 0

echo Opción inválida
pause
goto :eof

:monitor_logs
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║              📡 MONITOREANDO LOGS EN TIEMPO REAL                   ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.
echo Presiona Ctrl+C para detener el monitoreo
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

REM Verificar ADB
adb version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: ADB no está disponible
    pause
    exit /b 1
)

REM Limpiar logcat y monitorear
adb logcat -c
adb logcat -v time -s AnalyticsLogger:D CSVEventLogger:D FirebaseFirestore:D
pause
exit /b 0

:download_csv
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║           📄 DESCARGANDO ARCHIVO CSV DE EVENTOS                    ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.

REM Verificar ADB
adb version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: ADB no está disponible
    pause
    exit /b 1
)

echo 🔍 Buscando archivo CSV en el dispositivo...
echo.

REM Buscar el archivo analytics_events.csv en el dispositivo
adb shell "find /sdcard/Android/data/app.src/files -name 'analytics_events.csv' 2>/dev/null" > temp_path.txt

REM Leer la ruta del archivo
set /p csv_path=<temp_path.txt
del temp_path.txt

if "%csv_path%"=="" (
    echo ⚠️  No se encontró el archivo CSV en el dispositivo.
    echo.
    echo Posibles razones:
    echo - La app no ha generado eventos todavía
    echo - El archivo está en una ubicación diferente
    echo.
    echo Intentando rutas alternativas...

    REM Intentar ruta alternativa
    adb pull /sdcard/Android/data/app.src/files/analytics_events.csv analytics_events.csv 2>nul

    if not exist analytics_events.csv (
        echo ❌ No se pudo descargar el archivo CSV
        echo.
        echo Intenta ejecutar la app primero para generar eventos.
        pause
        exit /b 1
    )
) else (
    echo ✅ Archivo encontrado en: %csv_path%
    echo.
    echo 📥 Descargando archivo...

    adb pull %csv_path% analytics_events.csv

    if errorlevel 1 (
        echo ❌ Error al descargar el archivo
        pause
        exit /b 1
    )
)

echo.
echo ✅ Archivo descargado exitosamente: analytics_events.csv
echo.
echo 📊 Contenido del archivo:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

REM Mostrar el contenido del CSV
type analytics_events.csv

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo 💾 Archivo guardado en: %cd%\analytics_events.csv
echo.
pause
exit /b 0

:show_stats
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║              📊 ESTADÍSTICAS DE EVENTOS                            ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.

REM Verificar si existe el archivo CSV
if not exist analytics_events.csv (
    echo ⚠️  Primero descarga el archivo CSV (opción 2)
    pause
    exit /b 1
)

echo Analizando eventos...
echo.

REM Contar eventos por tipo
echo 📈 EVENTOS POR TIPO:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
findstr /C:"menu_ready" analytics_events.csv | find /c /v "" > temp_count.txt
set /p menu_count=<temp_count.txt
echo   menu_ready:           %menu_count% eventos

findstr /C:"payment_completed" analytics_events.csv | find /c /v "" > temp_count.txt
set /p payment_count=<temp_count.txt
echo   payment_completed:    %payment_count% eventos

findstr /C:"app_launch_to_menu" analytics_events.csv | find /c /v "" > temp_count.txt
set /p launch_count=<temp_count.txt
echo   app_launch_to_menu:   %launch_count% eventos

del temp_count.txt

echo.
echo 📱 TIPOS DE RED:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
findstr /C:"Wi-Fi" analytics_events.csv | find /c /v "" > temp_count.txt
set /p wifi_count=<temp_count.txt
echo   Wi-Fi:     %wifi_count% eventos

findstr /C:"4G" analytics_events.csv | find /c /v "" > temp_count.txt
set /p 4g_count=<temp_count.txt
echo   4G:        %4g_count% eventos

findstr /C:"5G" analytics_events.csv | find /c /v "" > temp_count.txt
set /p 5g_count=<temp_count.txt
echo   5G:        %5g_count% eventos

del temp_count.txt

echo.
echo 🎯 ÚLTIMOS 5 EVENTOS:
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
powershell -Command "Get-Content analytics_events.csv | Select-Object -Last 5"

echo.
pause
exit /b 0

:firebase_events
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║         🔥 EVENTOS ENVIADOS A FIREBASE FIRESTORE                   ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.
echo Monitoreando confirmaciones de Firebase...
echo Presiona Ctrl+C para detener
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

adb logcat -v time | findstr /C:"guardado en Firestore" /C:"Error al guardar en Firestore" /C:"Firebase Analytics"
pause
exit /b 0

:complete_mode
cls
echo ╔════════════════════════════════════════════════════════════════════╗
echo ║              🔄 MODO COMPLETO - ANÁLISIS TOTAL                     ║
echo ╚════════════════════════════════════════════════════════════════════╝
echo.

REM 1. Descargar CSV
echo [1/3] 📥 Descargando archivo CSV...
adb pull /sdcard/Android/data/app.src/files/analytics_events.csv analytics_events.csv 2>nul

if exist analytics_events.csv (
    echo ✅ CSV descargado exitosamente
) else (
    echo ⚠️  No se pudo descargar el CSV
)

echo.

REM 2. Mostrar estadísticas
if exist analytics_events.csv (
    echo [2/3] 📊 Estadísticas rápidas:
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    for /f %%A in ('type analytics_events.csv ^| find /c /v ""') do set total_lines=%%A
    set /a total_events=total_lines-1
    echo   Total de eventos: %total_events%

    echo.
    echo   Últimos 3 eventos:
    powershell -Command "Get-Content analytics_events.csv | Select-Object -Last 3"
    echo.
)

REM 3. Iniciar monitoreo en tiempo real
echo [3/3] 📡 Iniciando monitoreo en tiempo real...
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo Presiona Ctrl+C para detener el monitoreo
echo.

adb logcat -c
adb logcat -v time -s AnalyticsLogger:D CSVEventLogger:D FirebaseFirestore:D

pause
exit /b 0

