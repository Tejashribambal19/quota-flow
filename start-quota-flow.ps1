$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Starting PostgreSQL and Redis..." -ForegroundColor Cyan
Set-Location $root
docker compose up -d

Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\backend'; .\mvnw.cmd spring-boot:run"

Write-Host "Waiting for backend on port 8080..." -ForegroundColor Yellow
$ready = $false
for ($attempt = 1; $attempt -le 40; $attempt++) {
    if (Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    throw "Backend did not become ready. Check the backend PowerShell window."
}

Write-Host "Starting Quota Flow desktop application..." -ForegroundColor Green
Set-Location "$root\desktop-client"
mvn javafx:run
