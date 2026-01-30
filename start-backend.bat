@echo off
cd backend

echo ==========================================
echo      Starting Backend (Spring Boot)
echo ==========================================

mvn spring-boot:run
if %errorlevel% neq 0 pause
