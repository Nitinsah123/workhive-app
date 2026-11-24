# WorkHive - Local Development Runner
# Sets JAVA_HOME, launches backend in dev profile (in-memory H2, embedded MailHog), and launches frontend with Vite.

param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

$ErrorActionPreference = "Stop"

$workspaceRoot = $PSScriptRoot
$javaHome = "C:\Program Files\Java\jdk-23"
$javaExe = "$javaHome\bin\java.exe"
$backendJar = Join-Path $workspaceRoot "backend\target\workhive-backend-1.0.0.jar"
$frontendDir = Join-Path $workspaceRoot "frontend"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Starting WorkHive Application        " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Start Backend
if (-not $FrontendOnly) {
    if (Test-Path $backendJar) {
        Write-Host "`n[1/2] Starting Spring Boot Backend (Port 8080)..." -ForegroundColor Green
        $backendProcess = Start-Process -FilePath $javaExe `
            -ArgumentList "-Dspring.profiles.active=dev", "-Dmailhog.enabled=true", "-jar", "`"$backendJar`"" `
            -WorkingDirectory (Join-Path $workspaceRoot "backend") `
            -PassThru

        Write-Host "  -> Backend Process PID: $($backendProcess.Id)" -ForegroundColor DarkGray
        Write-Host "  -> Backend API: http://localhost:8080" -ForegroundColor Cyan
        Write-Host "  -> Actuator Health: http://localhost:8080/actuator/health" -ForegroundColor Cyan
        Write-Host "  -> Embedded MailHog UI: http://localhost:8025" -ForegroundColor Cyan
    } else {
        Write-Host "Backend JAR not found at $backendJar. Please build first using tools/apache-maven-3.9.6." -ForegroundColor Yellow
    }
}

# 2. Start Frontend
if (-not $BackendOnly) {
    Write-Host "`n[2/2] Starting React Frontend (Port 3000)..." -ForegroundColor Green
    $frontendProcess = Start-Process -FilePath "pnpm.cmd" `
        -ArgumentList "dev" `
        -WorkingDirectory $frontendDir `
        -PassThru

    Write-Host "  -> Frontend Process PID: $($frontendProcess.Id)" -ForegroundColor DarkGray
    Write-Host "  -> Frontend App: http://localhost:3000" -ForegroundColor Cyan
}

Write-Host "`nApplication is running!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Yellow
Write-Host "Backend:  http://localhost:8080" -ForegroundColor Yellow
Write-Host "MailHog:  http://localhost:8025" -ForegroundColor Yellow
