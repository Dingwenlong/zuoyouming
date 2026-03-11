@echo off
cd backend

if "%SERVER_PORT%"=="" set "SERVER_PORT=8082"
if "%DB_HOST%"=="" set "DB_HOST=127.0.0.1"
if "%DB_PORT%"=="" set "DB_PORT=3306"
if "%DB_NAME%"=="" set "DB_NAME=library_seat"
if "%REDIS_HOST%"=="" set "REDIS_HOST=localhost"
if "%REDIS_PORT%"=="" set "REDIS_PORT=6379"

echo ==========================================
echo      Starting Backend (Spring Boot)
echo ==========================================
echo Backend Port: %SERVER_PORT%
echo MySQL: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo Redis: %REDIS_HOST%:%REDIS_PORT%
echo ==========================================

mvn spring-boot:run
if %errorlevel% neq 0 pause
