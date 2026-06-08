@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title EchoInk - Stop Services
echo Stopping EchoInk frontend and backend services...
echo.

REM Kill processes using port 8080 (backend) and 5173 (frontend)
for /f "tokens=*" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    for %%p in (%%a) do set lasttoken=%%p
    if defined lasttoken (
        echo   Stopping backend PID !lasttoken!
        taskkill /PID !lasttoken! /F >nul 2>&1
        set lasttoken=
    )
)

for /f "tokens=*" %%a in ('netstat -ano ^| findstr ":5173.*LISTENING"') do (
    for %%p in (%%a) do set lasttoken=%%p
    if defined lasttoken (
        echo   Stopping frontend PID !lasttoken!
        taskkill /PID !lasttoken! /F >nul 2>&1
        set lasttoken=
    )
)

echo.
echo Stopped. (MySQL service remains running, not affected)
timeout /t 4 /nobreak >nul
