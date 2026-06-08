@echo off
chcp 65001 >nul
title EchoInk Starter
cd /d D:\YUMO

echo ============================================
echo    EchoInk Start
echo ============================================
echo.

echo [1/4] Checking MySQL...
sc query MySQL80 | find "RUNNING" >nul
if errorlevel 1 (
    echo     MySQL not running, starting...
    net start MySQL80
    if errorlevel 1 (
        echo     [ERROR] Cannot start MySQL. Please start MySQL80 service manually.
        pause
        exit /b 1
    )
) else (
    echo     MySQL is running
)
echo.

echo [2/4] Starting backend...
cd /d D:\YUMO\backend
start "Backend" cmd /c ""C:\Program Files\Git\bin\bash.exe" run-backend.sh & pause"
cd /d D:\YUMO
echo.

echo [3/4] Starting frontend...
cd /d D:\YUMO\frontend
start "Frontend" cmd /c "npm run dev & pause"
cd /d D:\YUMO
echo.

echo [4/4] Waiting for services to be ready (40 seconds)...
timeout /t 40 /nobreak >nul

start "" http://localhost:5173

echo.
echo ============================================
echo    Started! Browser should open at http://localhost:5173
echo    Account: demo / secret123
echo.
echo    Backend and frontend are running in separate windows.
echo    Close those windows to stop services.
echo ============================================
echo.
echo This window can be closed.
timeout /t 8 /nobreak >nul
