@echo off
setlocal
title Run TruckDiag Backend Pytest Suite

echo ======================================================
echo          Running TruckDiag Backend Test Suite         
echo ======================================================
echo.

cd /d "%~dp0\..\backend"

if not exist "venv\Scripts\pytest.exe" (
    echo [ERROR] Virtual environment with pytest not found!
    echo Run scripts\start-backend.bat first to set up the environment.
    pause
    exit /b 1
)

set PYTHONPATH=%cd%
venv\Scripts\pytest.exe -v tests

echo.
echo ======================================================
echo Tests completed.
echo ======================================================
pause
