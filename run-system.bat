@echo off
chcp 65001 >nul
setlocal

echo =======================================================
echo        Library Seat Management System - Launcher       
echo =======================================================

echo [1/4] Starting Docker Services...
docker-compose -p library-seat up -d
if %errorlevel% neq 0 (
    echo [ERROR] Docker failed to start. Please check if Docker Desktop is running.
    pause
    exit /b
)
echo [SUCCESS] Docker services are up.

echo [2/4] Starting Backend (Spring Boot)...
start "Library Backend" cmd /k "color 0A && call start-backend.bat"

echo [3/4] Starting Frontend (Vue 3)...
start "Library Frontend" cmd /k "color 0B && call start-frontend.bat"

echo [4/4] Opening Browser...
timeout /t 20 >nul
start http://localhost:3000

echo.
echo =======================================================
echo    System is starting up!
echo    - Backend: http://localhost:8080/api
echo    - Frontend: http://localhost:3000
echo    - MySQL: localhost:3306 (root/root_password)
echo    - Redis: localhost:6379
echo =======================================================
echo.
pause
