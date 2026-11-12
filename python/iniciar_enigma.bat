@echo off
setlocal
cd /d "%~dp0"
if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" main.py
    if errorlevel 1 pause
    exit /b
)
where py >nul 2>nul
if errorlevel 1 (
    python main.py
) else (
    py -3 main.py
)
if errorlevel 1 pause
endlocal
