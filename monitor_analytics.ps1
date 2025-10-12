# ===========================================================================
# MONITOR VISUAL DE ANALYTICS - FIREBASE FIRESTORE
# ===========================================================================

# Configuracion de colores
$Host.UI.RawUI.BackgroundColor = "Black"
$Host.UI.RawUI.ForegroundColor = "White"
Clear-Host

function Show-Header {
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host "         MONITOR DE ANALYTICS - FIREBASE FIRESTORE                         " -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Show-Menu {
    Show-Header
    Write-Host "Selecciona una opcion:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[1] " -ForegroundColor Green -NoNewline
    Write-Host "Monitorear logs en tiempo real (Live)"
    Write-Host "[2] " -ForegroundColor Green -NoNewline
    Write-Host "Descargar y mostrar archivo CSV"
    Write-Host "[3] " -ForegroundColor Green -NoNewline
    Write-Host "Mostrar estadisticas detalladas"
    Write-Host "[4] " -ForegroundColor Green -NoNewline
    Write-Host "Ver eventos de Firebase Firestore"
    Write-Host "[5] " -ForegroundColor Green -NoNewline
    Write-Host "Analisis grafico de rendimiento"
    Write-Host "[6] " -ForegroundColor Green -NoNewline
    Write-Host "Modo completo (todo lo anterior)"
    Write-Host "[0] " -ForegroundColor Red -NoNewline
    Write-Host "Salir"
    Write-Host ""

    $choice = Read-Host "Selecciona una opcion [0-6]"
    return $choice
}

function Test-ADB {
    try {
        $adbVersion = adb version 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERROR: ADB no esta instalado o no esta en el PATH" -ForegroundColor Red
            Write-Host ""
            Write-Host "Por favor, asegurate de que Android SDK este instalado." -ForegroundColor Yellow
            return $false
        }

        $devices = adb devices 2>&1 | Select-String "device$"
        if ($devices.Count -eq 0) {
            Write-Host "ADVERTENCIA: No hay dispositivos conectados" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Por favor, conecta tu dispositivo Android o inicia el emulador." -ForegroundColor Yellow
            return $false
        }

        Write-Host "Dispositivo conectado detectado" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "Error al verificar ADB: $_" -ForegroundColor Red
        return $false
    }
}

function Monitor-Logs {
    Clear-Host
    Show-Header
    Write-Host "MONITOREANDO LOGS EN TIEMPO REAL" -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    if (-not (Test-ADB)) {
        Read-Host "Presiona Enter para continuar"
        return
    }

    Write-Host "Filtrando eventos de Analytics..." -ForegroundColor Yellow
    Write-Host "   Presiona Ctrl+C para detener" -ForegroundColor Gray
    Write-Host ""
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    # Limpiar logcat y monitorear
    adb logcat -c

    # Monitorear con colores
    adb logcat -v time -s AnalyticsLogger:D CSVEventLogger:D FirebaseFirestore:D | ForEach-Object {
        $line = $_

        if ($line -match "menu_ready") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "payment_completed") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "guardado en Firestore") {
            Write-Host $line -ForegroundColor Magenta
        }
        elseif ($line -match "Error") {
            Write-Host $line -ForegroundColor Red
        }
        else {
            Write-Host $line -ForegroundColor White
        }
    }

    Read-Host "`nPresiona Enter para continuar"
}

function Download-CSV {
    Clear-Host
    Show-Header
    Write-Host "DESCARGANDO ARCHIVO CSV DE EVENTOS" -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    if (-not (Test-ADB)) {
        Read-Host "Presiona Enter para continuar"
        return
    }

    Write-Host "Buscando archivo CSV en el dispositivo..." -ForegroundColor Yellow

    # Intentar descargar el CSV
    $csvPath = "/sdcard/Android/data/app.src/files/analytics_events.csv"

    adb pull $csvPath "analytics_events.csv" 2>&1 | Out-Null

    if (Test-Path "analytics_events.csv") {
        Write-Host "Archivo descargado exitosamente" -ForegroundColor Green
        Write-Host ""
        Write-Host "Contenido del archivo:" -ForegroundColor Cyan
        Write-Host "===========================================================================" -ForegroundColor Cyan
        Write-Host ""

        $csv = Import-Csv "analytics_events.csv"
        $csv | Format-Table -AutoSize

        Write-Host ""
        Write-Host "===========================================================================" -ForegroundColor Cyan
        Write-Host "Archivo guardado en: $(Get-Location)\analytics_events.csv" -ForegroundColor Green
    }
    else {
        Write-Host "No se pudo descargar el archivo CSV" -ForegroundColor Red
        Write-Host ""
        Write-Host "Posibles razones:" -ForegroundColor Yellow
        Write-Host "  - La app no ha generado eventos todavia" -ForegroundColor Gray
        Write-Host "  - El archivo esta en una ubicacion diferente" -ForegroundColor Gray
    }

    Write-Host ""
    Read-Host "Presiona Enter para continuar"
}

function Show-Statistics {
    Clear-Host
    Show-Header
    Write-Host "ESTADISTICAS DETALLADAS DE EVENTOS" -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    if (-not (Test-Path "analytics_events.csv")) {
        Write-Host "Primero descarga el archivo CSV (opcion 2)" -ForegroundColor Yellow
        Read-Host "`nPresiona Enter para continuar"
        return
    }

    $csv = Import-Csv "analytics_events.csv"
    $totalEvents = $csv.Count

    Write-Host "RESUMEN GENERAL:" -ForegroundColor Yellow
    Write-Host "  Total de eventos: $totalEvents" -ForegroundColor White
    Write-Host ""

    # Eventos por tipo
    Write-Host "EVENTOS POR TIPO:" -ForegroundColor Yellow
    $eventTypes = $csv | Group-Object event_name | Sort-Object Count -Descending
    foreach ($type in $eventTypes) {
        $percentage = [math]::Round(($type.Count / $totalEvents) * 100, 2)
        $percentText = "$percentage%"
        Write-Host "  $($type.Name): $($type.Count) eventos ($percentText)" -ForegroundColor Cyan
    }
    Write-Host ""

    # Tipos de red
    Write-Host "DISTRIBUCION POR TIPO DE RED:" -ForegroundColor Yellow
    $networkTypes = $csv | Group-Object network_type | Sort-Object Count -Descending
    foreach ($network in $networkTypes) {
        $percentage = [math]::Round(($network.Count / $totalEvents) * 100, 2)
        $percentText = "$percentage%"
        Write-Host "  $($network.Name): $($network.Count) eventos ($percentText)" -ForegroundColor Cyan
    }
    Write-Host ""

    # Dispositivos por tier
    Write-Host "DISTRIBUCION POR NIVEL DE DISPOSITIVO:" -ForegroundColor Yellow
    $deviceTiers = $csv | Group-Object device_tier | Sort-Object Count -Descending
    foreach ($tier in $deviceTiers) {
        $percentage = [math]::Round(($tier.Count / $totalEvents) * 100, 2)
        $percentText = "$percentage%"
        Write-Host "  $($tier.Name): $($tier.Count) eventos ($percentText)" -ForegroundColor Cyan
    }
    Write-Host ""

    # Estadisticas de duracion para menu_ready
    $menuReadyEvents = $csv | Where-Object { $_.event_name -eq "menu_ready" }
    if ($menuReadyEvents.Count -gt 0) {
        Write-Host "RENDIMIENTO (menu_ready):" -ForegroundColor Yellow
        $durations = $menuReadyEvents | ForEach-Object { [int]$_.duration_ms }
        $avgDuration = ($durations | Measure-Object -Average).Average
        $minDuration = ($durations | Measure-Object -Minimum).Minimum
        $maxDuration = ($durations | Measure-Object -Maximum).Maximum

        Write-Host "  Duracion promedio: $([math]::Round($avgDuration, 2)) ms" -ForegroundColor Green
        Write-Host "  Duracion minima:   $minDuration ms" -ForegroundColor Green
        Write-Host "  Duracion maxima:   $maxDuration ms" -ForegroundColor Yellow
        Write-Host ""
    }

    # Ultimos 5 eventos
    Write-Host "ULTIMOS 5 EVENTOS:" -ForegroundColor Yellow
    $csv | Select-Object -Last 5 | Format-Table timestamp, event_name, duration_ms, network_type, device_tier -AutoSize

    Write-Host ""
    Read-Host "Presiona Enter para continuar"
}

function Show-FirebaseEvents {
    Clear-Host
    Show-Header
    Write-Host "EVENTOS ENVIADOS A FIREBASE FIRESTORE" -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    if (-not (Test-ADB)) {
        Read-Host "Presiona Enter para continuar"
        return
    }

    Write-Host "Monitoreando confirmaciones de Firebase..." -ForegroundColor Yellow
    Write-Host "   Presiona Ctrl+C para detener" -ForegroundColor Gray
    Write-Host ""
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    # Mostrar solo logs de Firebase
    adb logcat -v time | Select-String "guardado en Firestore|Error al guardar en Firestore|FirebaseFirestore" | ForEach-Object {
        $line = $_

        if ($line -match "guardado en Firestore") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "Error") {
            Write-Host $line -ForegroundColor Red
        }
        else {
            Write-Host $line -ForegroundColor Cyan
        }
    }

    Read-Host "`nPresiona Enter para continuar"
}

function Show-PerformanceAnalysis {
    Clear-Host
    Show-Header
    Write-Host "ANALISIS GRAFICO DE RENDIMIENTO" -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    if (-not (Test-Path "analytics_events.csv")) {
        Write-Host "Primero descarga el archivo CSV (opcion 2)" -ForegroundColor Yellow
        Read-Host "`nPresiona Enter para continuar"
        return
    }

    $csv = Import-Csv "analytics_events.csv"
    $menuReadyEvents = $csv | Where-Object { $_.event_name -eq "menu_ready" } | ForEach-Object { [int]$_.duration_ms }

    if ($menuReadyEvents.Count -eq 0) {
        Write-Host "No hay eventos menu_ready para analizar" -ForegroundColor Yellow
        Read-Host "`nPresiona Enter para continuar"
        return
    }

    # Grafico ASCII de distribucion de duraciones
    Write-Host "DISTRIBUCION DE DURACIONES (menu_ready):" -ForegroundColor Yellow
    Write-Host ""

    $min = ($menuReadyEvents | Measure-Object -Minimum).Minimum
    $max = ($menuReadyEvents | Measure-Object -Maximum).Maximum
    $avg = ($menuReadyEvents | Measure-Object -Average).Average

    # Crear rangos
    $range = $max - $min
    $bucketSize = [math]::Max(100, [math]::Ceiling($range / 10))

    $buckets = @{}
    for ($i = 0; $i -lt 10; $i++) {
        $buckets[$i] = 0
    }

    foreach ($duration in $menuReadyEvents) {
        $bucketIndex = [math]::Floor(($duration - $min) / $bucketSize)
        if ($bucketIndex -ge 10) { $bucketIndex = 9 }
        $buckets[$bucketIndex]++
    }

    $maxCount = ($buckets.Values | Measure-Object -Maximum).Maximum

    for ($i = 0; $i -lt 10; $i++) {
        $rangeStart = $min + ($i * $bucketSize)
        $rangeEnd = $rangeStart + $bucketSize
        $count = $buckets[$i]

        $barLength = if ($maxCount -gt 0) { [math]::Floor(($count / $maxCount) * 40) } else { 0 }
        $bar = "#" * $barLength

        $rangeText = "$("{0,5}" -f $rangeStart)-$("{0,5}" -f $rangeEnd) ms: "
        Write-Host $rangeText -ForegroundColor White -NoNewline
        Write-Host $bar -ForegroundColor Green -NoNewline
        Write-Host " ($count)" -ForegroundColor Cyan
    }

    Write-Host ""
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host "  Promedio: $([math]::Round($avg, 2)) ms" -ForegroundColor Green
    Write-Host "  Minimo:   $min ms" -ForegroundColor Green
    Write-Host "  Maximo:   $max ms" -ForegroundColor Yellow

    Write-Host ""
    Read-Host "Presiona Enter para continuar"
}

function Complete-Mode {
    Clear-Host
    Show-Header
    Write-Host "MODO COMPLETO - ANALISIS TOTAL" -ForegroundColor Cyan
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    # Paso 1: Descargar CSV
    Write-Host "[1/3] Descargando archivo CSV..." -ForegroundColor Yellow
    adb pull "/sdcard/Android/data/app.src/files/analytics_events.csv" "analytics_events.csv" 2>&1 | Out-Null

    if (Test-Path "analytics_events.csv") {
        Write-Host "      CSV descargado exitosamente" -ForegroundColor Green
    } else {
        Write-Host "      No se pudo descargar el CSV" -ForegroundColor Yellow
    }
    Write-Host ""

    # Paso 2: Mostrar estadisticas
    if (Test-Path "analytics_events.csv") {
        Write-Host "[2/3] Estadisticas rapidas:" -ForegroundColor Yellow

        $csv = Import-Csv "analytics_events.csv"
        $totalEvents = $csv.Count

        Write-Host "      Total de eventos: $totalEvents" -ForegroundColor Cyan

        $eventTypes = $csv | Group-Object event_name
        Write-Host ""
        Write-Host "      Eventos por tipo:" -ForegroundColor White
        foreach ($type in $eventTypes) {
            Write-Host "        - $($type.Name): $($type.Count)" -ForegroundColor Gray
        }

        Write-Host ""
        Write-Host "      Ultimos 3 eventos:" -ForegroundColor White
        $csv | Select-Object -Last 3 | Format-Table timestamp, event_name, duration_ms, network_type -AutoSize
    }

    # Paso 3: Monitoreo en tiempo real
    Write-Host "[3/3] Iniciando monitoreo en tiempo real..." -ForegroundColor Yellow
    Write-Host "      Presiona Ctrl+C para detener" -ForegroundColor Gray
    Write-Host ""
    Write-Host "===========================================================================" -ForegroundColor Cyan
    Write-Host ""

    adb logcat -c
    adb logcat -v time -s AnalyticsLogger:D CSVEventLogger:D FirebaseFirestore:D | ForEach-Object {
        $line = $_

        if ($line -match "guardado en Firestore") {
            Write-Host $line -ForegroundColor Magenta
        }
        elseif ($line -match "menu_ready") {
            Write-Host $line -ForegroundColor Green
        }
        elseif ($line -match "payment_completed") {
            Write-Host $line -ForegroundColor Cyan
        }
        elseif ($line -match "Error") {
            Write-Host $line -ForegroundColor Red
        }
        else {
            Write-Host $line -ForegroundColor White
        }
    }

    Read-Host "`nPresiona Enter para continuar"
}

# Menu principal
while ($true) {
    $choice = Show-Menu

    switch ($choice) {
        "1" { Monitor-Logs }
        "2" { Download-CSV }
        "3" { Show-Statistics }
        "4" { Show-FirebaseEvents }
        "5" { Show-PerformanceAnalysis }
        "6" { Complete-Mode }
        "0" {
            Clear-Host
            Write-Host "Hasta luego!" -ForegroundColor Green
            exit
        }
        default {
            Write-Host "Opcion invalida" -ForegroundColor Red
            Start-Sleep -Seconds 1
        }
    }
}

