@echo off
chcp 65001 >nul
setlocal

echo =======================================================
echo     Library Seat System - Dependency Installer
echo =======================================================

:: 1. Backend Install
echo.
echo [1/2] Installing Backend Dependencies (Maven)...
cd backend
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed. Please check if Maven is installed and configured.
    pause
    exit /b
)
cd ..
echo [SUCCESS] Backend dependencies installed.

:: 2. Frontend Install
echo.
echo [2/2] Installing Frontend Dependencies (NPM)...
cd frontend
call npm install
if %errorlevel% neq 0 (
    echo [ERROR] NPM install failed. Please check if Node.js is installed.
    pause
    exit /b
)
cd ..
echo [SUCCESS] Frontend dependencies installed.

echo.
echo =======================================================
echo    All dependencies installed successfully!
echo    You can now run 'run-system.bat' to start the system.
echo =======================================================
pause
