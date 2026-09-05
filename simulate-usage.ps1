param(
    [int]$Events = 25,
    [int]$DelayMilliseconds = 700
)

$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"
$loginBody = @{ email = "admin@abclogistics.com"; password = "Tenant@12345" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.token)" }
$resources = @("API_REQUEST", "STORAGE_MB", "COMPUTE_SECOND", "BACKGROUND_JOB")

Write-Host "Generating $Events realistic usage events for ABC Logistics..." -ForegroundColor Cyan
for ($i = 1; $i -le $Events; $i++) {
    $resource = $resources | Get-Random
    $quantity = switch ($resource) {
        "API_REQUEST" { Get-Random -Minimum 1 -Maximum 20 }
        "STORAGE_MB" { Get-Random -Minimum 1 -Maximum 15 }
        "COMPUTE_SECOND" { Get-Random -Minimum 5 -Maximum 60 }
        default { Get-Random -Minimum 1 -Maximum 5 }
    }
    $body = @{
        resourceType = $resource
        quantity = $quantity
        requestId = "sim-$([guid]::NewGuid())"
    } | ConvertTo-Json
    try {
        $result = Invoke-RestMethod -Uri "$baseUrl/usage/$($login.tenantId)/consume" `
            -Method Post -Headers $headers -ContentType "application/json" -Body $body
        Write-Host "[$i/$Events] $resource +$quantity -> $($result.level) ($($result.percentage)%)"
    } catch {
        Write-Host "[$i/$Events] $resource blocked by quota" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds $DelayMilliseconds
}
Write-Host "Simulation complete. Refresh the tenant dashboard." -ForegroundColor Green
