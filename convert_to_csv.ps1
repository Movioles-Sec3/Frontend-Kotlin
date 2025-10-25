# Script PowerShell para convertir las métricas XML a CSV
# Este script es llamado por el .bat

param(
    [string]$InputXml = "temp_prefs.xml",
    [string]$OutputCsv = "performance_metrics\metrics.csv"
)

try {
    Write-Host "Leyendo archivo XML..." -ForegroundColor Yellow
    [xml]$xml = Get-Content $InputXml -Encoding UTF8

    Write-Host "Creando archivo CSV..." -ForegroundColor Yellow

    # Crear header del CSV
    $header = "timestamp,load_type,catalog_time_ms,images_time_ms,total_time_ms,menu_ready_time_ms,product_count,network_type,device_tier"
    $header | Out-File -FilePath $OutputCsv -Encoding UTF8

    $totalRecords = 0

    # Procesar mediciones PARALELAS
    $parallel = $xml.map.string | Where-Object { $_.name -eq 'PARALLEL_measurements' }
    if ($parallel -and $parallel.'#text') {
        Write-Host "  📊 Procesando mediciones PARALELAS..." -ForegroundColor Cyan
        $measurements = $parallel.'#text'.Split('|')
        foreach ($measurement in $measurements) {
            if ($measurement.Trim() -ne '') {
                $measurement | Out-File -FilePath $OutputCsv -Append -Encoding UTF8
                $totalRecords++
            }
        }
    }

    # Procesar mediciones SECUENCIALES
    $sequential = $xml.map.string | Where-Object { $_.name -eq 'SEQUENTIAL_measurements' }
    if ($sequential -and $sequential.'#text') {
        Write-Host "  📦 Procesando mediciones SECUENCIALES..." -ForegroundColor Cyan
        $measurements = $sequential.'#text'.Split('|')
        foreach ($measurement in $measurements) {
            if ($measurement.Trim() -ne '') {
                $measurement | Out-File -FilePath $OutputCsv -Append -Encoding UTF8
                $totalRecords++
            }
        }
    }

    Write-Host ""
    Write-Host "✅ Conversión completada - Total registros: $totalRecords" -ForegroundColor Green

    # Mostrar vista previa
    Write-Host ""
    Write-Host "📄 Vista previa del CSV:" -ForegroundColor Cyan
    Get-Content $OutputCsv | Select-Object -First 4
    if ($totalRecords -gt 3) {
        Write-Host "   ..." -ForegroundColor Gray
    }

    exit 0

} catch {
    Write-Host "❌ Error: $_" -ForegroundColor Red
    exit 1
}

