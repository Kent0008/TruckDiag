@echo off
setlocal enabledelayedexpansion
title TruckDiag Diagnostic API Server

echo ======================================================
echo          TruckDiag Platform - API & Simulator         
echo ======================================================
echo.

cd /d "%~dp0\..\backend"

:: 1. Check Python
where python >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Python not found in system PATH!
    echo Please install Python 3.12+ from https://www.python.org/
    pause
    exit /b 1
)

:: 2. Create virtual environment if missing
if not exist "venv\Scripts\python.exe" (
    echo [*] Creating virtual environment in backend\venv...
    python -m venv venv
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Failed to create virtual environment.
        pause
        exit /b 1
    )
    echo [*] Upgrading pip...
    venv\Scripts\python.exe -m pip install --upgrade pip
)

:: 3. Install/verify requirements
echo [*] Checking dependencies...
venv\Scripts\pip.exe install -r requirements.txt
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to install requirements.
    pause
    exit /b 1
)

:: 4. Start FastAPI server
echo.
echo ======================================================
echo  Server is starting on http://0.0.0.0:8000
echo  - Swagger UI:   http://localhost:8000/docs
echo  - Health Check: http://localhost:8000/api/v1/health
echo  - Android Emu:  http://10.0.2.2:8000
echo ======================================================
echo.

set PYTHONPATH=%cd%
venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
pause
