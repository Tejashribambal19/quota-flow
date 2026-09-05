$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$client = Join-Path $root "desktop-client"

Set-Location $client
Write-Host "Building JavaFX client..." -ForegroundColor Cyan
mvn clean package dependency:copy-dependencies -DincludeScope=runtime

$input = Join-Path $client "target\jpackage-input"
$output = Join-Path $client "target\installer"
New-Item -ItemType Directory -Force -Path $input | Out-Null
New-Item -ItemType Directory -Force -Path $output | Out-Null
Copy-Item "target\desktop-client-1.0.0.jar" $input -Force
Copy-Item "target\dependency\*.jar" $input -Force

Write-Host "Creating Windows application image..." -ForegroundColor Cyan
jpackage `
  --type app-image `
  --name "QuotaFlow" `
  --app-version "1.0.0" `
  --vendor "Tejashri Bambal" `
  --input $input `
  --dest $output `
  --main-jar "desktop-client-1.0.0.jar" `
  --main-class "com.tejashri.quota.client.QuotaDesktopApplication"

Write-Host "Package created at: $output\QuotaFlow" -ForegroundColor Green
