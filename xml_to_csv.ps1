# Script para convertir el XML de métricas a CSV
param(
    [string]$XmlFile = "temp_prefs.xml",
    [string]$OutputCsv
)

Write-Host "Procesando archivo XML..." -ForegroundColor Yellow

try {
    # Leer el archivo XML
    [xml]$xml = Get-Content $XmlFile -Encoding UTF8

    # Crear lista para almacenar todos los registros
    $allRecords = @()

    # Procesar PARALLEL_measurements
    $parallel = $xml.map.string | Where-Object { $_.name -eq 'PARALLEL_measurements' }
    if ($parallel -and $parallel.'#text') {
        Write-Host "  Procesando PARALLEL..." -ForegroundColor Cyan
        $measurements = $parallel.'#text' -split '\|'
        foreach ($m in $measurements) {
            if ($m.Trim() -ne '') {
                $allRecords += $m.Trim()
            }
        }
    }

    # Procesar SEQUENTIAL_measurements
    $sequential = $xml.map.string | Where-Object { $_.name -eq 'SEQUENTIAL_measurements' }
    if ($sequential -and $sequential.'#text') {
        Write-Host "  Procesando SEQUENTIAL..." -ForegroundColor Cyan
        $measurements = $sequential.'#text' -split '\|'
        foreach ($m in $measurements) {
            if ($m.Trim() -ne '') {
                $allRecords += $m.Trim()
            }
        }
    }

    # Escribir CSV
    Write-Host "  Escribiendo CSV..." -ForegroundColor Cyan

    # Header
    "timestamp,load_type,catalog_time_ms,images_time_ms,total_time_ms,menu_ready_time_ms,product_count,network_type,device_tier" | Out-File -FilePath $OutputCsv -Encoding UTF8

    # Datos
    foreach ($record in $allRecords) {
        $record | Out-File -FilePath $OutputCsv -Append -Encoding UTF8
    }

    Write-Host ""
    Write-Host "✅ Conversión completada - $($allRecords.Count) registros" -ForegroundColor Green

    # Mostrar vista previa
    Write-Host ""
    Write-Host "Vista previa:" -ForegroundColor Cyan
    Get-Content $OutputCsv | Select-Object -First 4
    if ($allRecords.Count -gt 3) {
        Write-Host "   ..." -ForegroundColor Gray
    }

    exit 0

} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

