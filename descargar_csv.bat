@echo off
echo.
echo ============================================
echo   DESCARGA DEL CSV
echo ============================================
echo.

echo Intentando metodo 1: run-as...
adb shell "run-as app.src cat files/analytics_events.csv" > analytics_events_temp.csv 2>nul

if exist analytics_events_temp.csv (
    for %%A in (analytics_events_temp.csv) do set size=%%~zA
    if !size! GTR 0 (
        move /Y analytics_events_temp.csv analytics_events.csv >nul
        echo [OK] CSV descargado exitosamente: analytics_events.csv
        echo.
        type analytics_events.csv
        echo.
        goto :success
    )
    del analytics_events_temp.csv
)

echo Metodo 1 fallo. Intentando metodo 2: almacenamiento externo...
adb pull /storage/emulated/0/Android/data/app.src/files/analytics_events.csv analytics_events.csv 2>nul

if exist analytics_events.csv (
    echo [OK] CSV descargado exitosamente: analytics_events.csv
    echo.
    type analytics_events.csv
    goto :success
)

echo.
echo ============================================
echo   NO SE PUDO DESCARGAR EL CSV
echo ============================================
echo.
echo POSIBLES CAUSAS:
echo   1. El archivo no existe aun en el dispositivo
echo   2. La app no se ejecuto con el codigo nuevo
echo   3. No se ha generado ningun evento
echo.
echo SOLUCION:
echo   1. Ejecuta: reinstalar_y_monitorear.bat
echo   2. Abre la app
echo   3. Espera a ver en los logs: "Archivo CSV creado"
echo   4. Vuelve a ejecutar este script
echo.
pause
exit /b 1

:success
echo.
echo ============================================
echo   EXITO
echo ============================================
echo.
echo El archivo CSV se descargo correctamente.
echo Puedes abrirlo con Excel o cualquier editor.
echo.
pause

