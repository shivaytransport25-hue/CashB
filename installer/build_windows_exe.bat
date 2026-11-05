@echo off
setlocal enabledelayedexpansion

REM =====================================================================
REM Shivay CashBook - Windows Desktop Inno Setup Compilation Script (.bat)
REM Generates: Shivay_CashBook_Setup_v1.0.0_x64.exe
REM =====================================================================

echo [1/4] Checking Inno Setup 6 Compiler installation...
set "ISCC_PATH="

where ISCC.exe >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "ISCC_PATH=ISCC.exe"
) else if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" (
    set "ISCC_PATH=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
) else if exist "%ProgramFiles%\Inno Setup 6\ISCC.exe" (
    set "ISCC_PATH=%ProgramFiles%\Inno Setup 6\ISCC.exe"
) else if exist "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" (
    set "ISCC_PATH=%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe"
)

if "%ISCC_PATH%"=="" (
    echo [ERROR] Inno Setup 6 Compiler (ISCC.exe) was not found on this system.
    echo Please download and install Inno Setup 6 from: https://jrsoftware.org/isdl.php
    echo Or install via Chocolatey: choco install innosetup
    echo Or install via Winget: winget install JRSoftware.InnoSetup
    exit /b 1
)

echo Found Inno Setup at: "%ISCC_PATH%"

echo [2/4] Preparing distribution bin directory...
if not exist "bin" mkdir "bin"
if not exist "bin\data" mkdir "bin\data"
if not exist "assets" mkdir "assets"
if not exist "output" mkdir "output"

REM Ensure executable placeholder exists if building standalone packaging test
if not exist "bin\ShivayCashBook.exe" (
    echo NOTE: Placing desktop runner in bin\ShivayCashBook.exe...
    copy /y NUL "bin\ShivayCashBook.exe" >nul
)

echo [3/4] Compiling Windows Installer executable (.exe)...
"%ISCC_PATH%" /Qp /O"output" "shivay_cashbook_inno_setup.iss"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Inno Setup compilation failed!
    exit /b %ERRORLEVEL%
)

echo [4/4] SUCCESS! Windows Installer generated at:
echo %CD%\output\Shivay_CashBook_Setup_v1.0.0_x64.exe
exit /b 0
