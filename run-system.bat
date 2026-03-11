@echo off
chcp 65001 >nul
setlocal

if "%MYSQL_PORT%"=="" set "MYSQL_PORT=13306"
if "%REDIS_PORT%"=="" set "REDIS_PORT=6380"
if "%BACKEND_PORT%"=="" set "BACKEND_PORT=8082"
if "%FRONTEND_PORT%"=="" set "FRONTEND_PORT=3000"

echo =======================================================
echo        Library Seat Management System - Launcher       
echo =======================================================

echo [1/4] Starting Docker Services...
docker compose -p library-seat up -d
if %errorlevel% neq 0 (
    echo [ERROR] Docker failed to start. Please check if Docker Desktop is running.
    pause
    exit /b
)
echo [SUCCESS] Docker services are up.

echo [2/4] Starting Backend (Spring Boot)...
start "Library Backend" cmd /k "color 0A && set DB_PORT=%MYSQL_PORT% && set REDIS_PORT=%REDIS_PORT% && set SERVER_PORT=%BACKEND_PORT% && call start-backend.bat"

echo [3/4] Starting Frontend (Vue 3)...
start "Library Frontend" cmd /k "color 0B && call start-frontend.bat"

echo [4/4] Opening Browser...
timeout /t 20 >nul
start http://localhost:%FRONTEND_PORT%

echo.
echo =======================================================
echo    System is starting up!
echo    - Backend: http://localhost:%BACKEND_PORT%
echo    - Frontend: http://localhost:%FRONTEND_PORT%
echo    - MySQL: localhost:%MYSQL_PORT% (root/root_password)
echo    - Redis: localhost:%REDIS_PORT%
echo =======================================================
echo.
pause
