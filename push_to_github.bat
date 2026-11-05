@echo off
set "PATH=C:\Users\WIN\MinGit\cmd;%PATH%"
cd /d "G:\My Drive\project\Cash Book"
echo ========================================================
echo Pushing CashBook Project to GitHub (shivaytransport25-hue/CashB)...
echo ========================================================
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Code pushed successfully to GitHub!
) else (
    echo [NOTE] If prompted, sign in with GitHub in the browser window or enter your Personal Access Token (PAT).
)
pause
