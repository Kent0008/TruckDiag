@echo off
setlocal enabledelayedexpansion
title Stop TruckDiag Server

echo ======================================================
echo          Stopping TruckDiag API Server (Port 8000)    
echo ======================================================
echo.

set FOUND=0
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8000" ^| findstr "LISTENING"') do (
    set PID=%%a
    echo [*] Found process on port 8000 with PID: !PID!
    taskkill /F /PID !PID! >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        echo [*] Successfully stopped PID !PID!
        set FOUND=1
    )
)

if "!FOUND!"=="0" (
    echo [*] No process found listening on port 8000.
) else (
    echo [OK] TruckDiag API server has been stopped.
)

echo.
timeout /t 3 >nul
