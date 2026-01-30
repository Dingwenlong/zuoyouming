@echo off
chcp 65001 >nul
setlocal

echo =======================================================
echo     Library Seat System - Frontend Dependency Fixer
echo =======================================================

cd frontend

echo [1/3] Cleaning old dependencies...
if exist "node_modules" (
    rmdir /s /q "node_modules"
    echo - Removed node_modules
)
if exist "pnpm-lock.yaml" (
    del "pnpm-lock.yaml"
    echo - Removed pnpm-lock.yaml
)

echo.
echo [2/3] Installing dependencies with local pnpm...
echo This may take a few minutes. Please wait...
call pnpm install
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Installation failed.
    echo Please check your network connection or pnpm version.
    pause
    exit /b
)

echo.
echo [3/3] Verifying setup...
call pnpm run prepare
if %errorlevel% neq 0 (
    echo [WARN] 'prepare' script failed, but dependencies are installed.
)

echo.
echo =======================================================
echo    Fix Complete! 
echo    The "out of sync" warning should be gone.
echo    You can now close this window and run 'run-system.bat'.
echo =======================================================
pause
