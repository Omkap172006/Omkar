@echo off
setlocal
cd /d "%~dp0"

echo ==========================================================
echo   AI-Powered Virtual CA System (Pure Java Spring Boot)
echo   Running on: http://localhost:8000
echo ==========================================================
echo Press Ctrl+C to stop the server at any time.
echo.

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)

"%JAVA_CMD%" -jar "%~dp0backend\target\virtual-ca-backend-1.0.0.jar"
pause

