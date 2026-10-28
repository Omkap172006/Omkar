# AI-Powered Virtual CA System - 1-Click Startup Script (Pure Java Spring Boot)
$ErrorActionPreference = "Stop"

$jarPath = Join-Path $PSScriptRoot "backend\target\virtual-ca-backend-1.0.0.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "[Virtual CA] Packaging backend JAR with Maven..." -ForegroundColor Cyan
    Set-Location (Join-Path $PSScriptRoot "backend")
    & "c:\tools\apache-maven-3.9.9\bin\mvn.cmd" package -DskipTests
    Set-Location $PSScriptRoot
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  AI-Powered Virtual CA System (Pure Java Spring Boot)    " -ForegroundColor Green
Write-Host "  Running on: http://localhost:8000                       " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the server at any time.`n" -ForegroundColor Yellow

$javaExe = "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot\bin\java.exe"
if (-not (Test-Path $javaExe)) {
    $javaExe = "java"
}

& $javaExe -jar $jarPath

