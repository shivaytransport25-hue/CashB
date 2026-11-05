=====================================================================
SHIVAY CASHBOOK - PC SOFTWARE (WINDOWS DESKTOP INSTALLER .EXE)
Inno Setup 6 Build Package
=====================================================================

To create the Windows Desktop Setup Executable (Shivay_CashBook_Setup_v1.0.0_x64.exe):

METHOD 1: ONE-CLICK BATCH FILE (Recommended for Windows)
---------------------------------------------------------
1. Install Inno Setup 6 on your Windows PC from:
   https://jrsoftware.org/isdl.php
   (Or run: winget install JRSoftware.InnoSetup)
2. Double-click "build_windows_exe.bat".
3. The setup executable will be compiled and placed into:
   dist\windows\output\Shivay_CashBook_Setup_v1.0.0_x64.exe

METHOD 2: INNO SETUP GUI COMPILER
----------------------------------
1. Open Inno Setup Compiler.
2. Open file "shivay_cashbook_inno_setup.iss".
3. Click "Build" -> "Compile" (or press Ctrl + F9).
4. The generated installer will appear in the output folder.

WHAT THE INSTALLER DOES ON WINDOWS PC:
---------------------------------------
- Installs 64-bit Shivay CashBook Desktop Accounting software.
- Creates Desktop shortcut & Start Menu group.
- Registers .scb (Shivay Cash Book) backup file associations.
- Includes full offline SQLite engine and real-time cloud sync.
- Supports PDF statement export, A4 receipt printing, and Excel CSV.
=====================================================================
