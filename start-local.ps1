$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = Join-Path $projectRoot "backend"
$webClientPath = Join-Path $projectRoot "web-client"

function Require-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Install it and try again."
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       QUOTA FLOW - LOCAL STARTUP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Require-Command "docker"
Require-Command "mvn"
Require-Command "npm"

if (-not (Test-Path $backendPath)) {
    throw "Backend directory not found: $backendPath"
}

if (-not (Test-Path $webClientPath)) {
    throw "Web client directory not found: $webClientPath"
}

Write-Host "[1/5] Checking Docker..." -ForegroundColor Yellow

docker info *> $null

if ($LASTEXITCODE -ne 0) {
    $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"

    if (-not (Test-Path $dockerDesktop)) {
        throw "Docker Desktop is not running and could not be found."
    }

    Write-Host "Starting Docker Desktop..."
    Start-Process $dockerDesktop

    $dockerReady = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        Start-Sleep -Seconds 2
        docker info *> $null

        if ($LASTEXITCODE -eq 0) {
            $dockerReady = $true
            break
        }
    }

    if (-not $dockerReady) {
        throw "Docker Desktop did not become ready in time."
    }
}

Write-Host "[2/5] Starting PostgreSQL and Redis..." -ForegroundColor Yellow
Push-Location $projectRoot
try {
    docker compose up -d
} finally {
    Pop-Location
}

Write-Host "[3/5] Generating a local JWT secret..." -ForegroundColor Yellow
$secretBytes = New-Object byte[] 64
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$random.GetBytes($secretBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)

Write-Host "[4/5] Starting the Spring Boot backend..." -ForegroundColor Yellow
$backendCommand = "Set-Location '$backendPath'; mvn spring-boot:run"
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $backendCommand

Write-Host "Waiting for http://localhost:8080/api/health ..."
$backendReady = $false

for ($attempt = 1; $attempt -le 90; $attempt++) {
    try {
        $health = Invoke-RestMethod `
            -Uri "http://localhost:8080/api/health" `
            -TimeoutSec 2

        if ($health.status -eq "UP") {
            $backendReady = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $backendReady) {
    throw "The backend did not become healthy in time. Check its PowerShell window."
}

Write-Host "[5/5] Starting the React frontend..." -ForegroundColor Yellow
$webCommand = "Set-Location '$webClientPath'; npm install; npm run dev"
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $webCommand

Start-Sleep -Seconds 5
Start-Process "http://localhost:5173"

Write-Host ""
Write-Host "Quota Flow started successfully." -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173"
Write-Host "Backend:  http://localhost:8080/api"
Write-Host "Health:   http://localhost:8080/api/health"
Write-Host ""
Write-Host "Close the backend and frontend PowerShell windows to stop the applications."
