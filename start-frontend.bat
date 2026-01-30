@echo off
cd frontend

echo ==========================================
echo      Starting Frontend (Vue 3)
echo ==========================================

:: Use npm instead of pnpm for stability
npm run dev || pause
