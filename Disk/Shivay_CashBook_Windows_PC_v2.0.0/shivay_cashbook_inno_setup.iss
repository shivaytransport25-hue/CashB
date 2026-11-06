; =====================================================================
; Shivay CashBook - Inno Setup 6 Script for Windows Desktop (x64)
; Produces: Shivay_CashBook_Setup_v2.0.0_x64.exe
; Build Includes:
;   - Minimalist Progressive Disclosure Ledger UI
;   - Voucher Entry Edit & Delete with Back-Date Protection
;   - Operator Username, PIN/Password App Lock & Profile Photo
;   - 1-Tap Petty Cash (Chhote-Mote Kharche) & Calendar/Clock Picker
;   - Dynamic Color + Light / Dark / System Display Mode
;   - Real-Time Firebase Cloud Sync & PDF/CSV/JSON Export
; =====================================================================

#define MyAppName "Shivay CashBook"
#define MyAppVersion "2.0.0"
#define MyAppPublisher "Shivay Technologies Pvt. Ltd."
#define MyAppURL "https://shivaycashbook.com"
#define MyAppExeName "ShivayCashBook.exe"
#define MyAppAssocName MyAppName + " Financial Backup"
#define MyAppAssocExt ".scb"
#define MyAppAssocKey StringChange(MyAppAssocName, " ", "") + MyAppAssocExt

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
AppId={{9F82A4D1-5C73-4E88-A210-4B4EE6C63B72}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/support
AppUpdatesURL={#MyAppURL}/updates
DefaultDirName={autopf}\{#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
ChangesAssociations=yes
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
; Windows 10/11 & Server modern styling
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputDir=output
OutputBaseFilename=Shivay_CashBook_Setup_v2.0.0_x64
Compression=lzma2/ultra64
SolidCompression=yes
DisableDirPage=no
DisableProgramGroupPage=no
VersionInfoVersion={#MyAppVersion}
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription={#MyAppName} Desktop Financial Management System (v{#MyAppVersion})
VersionInfoCopyright=Copyright (C) 2026 {#MyAppPublisher}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "Create a Quick Launch shortcut"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked; OnlyBelowVersion: 6.1; Check: not IsAdminInstallMode
Name: "startupicon"; Description: "Launch {#MyAppName} on Windows startup"; GroupDescription: "Startup:"; Flags: unchecked

[Files]
; Main Executable and core application bundles
Source: "bin\ShivayCashBook.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "bin\*.dll"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "bin\data\*"; DestDir: "{app}\data"; Flags: ignoreversion recursesubdirs createallsubdirs
; Templates, fonts, and assets
Source: "assets\*"; DestDir: "{app}\assets"; Flags: ignoreversion recursesubdirs createallsubdirs
; Sample documentation and README
Source: "README_PC_INSTALLER.txt"; DestDir: "{app}"; DestName: "User_Manual.txt"; Flags: ignoreversion

[Registry]
; Register .scb file extension for Shivay CashBook Backup files
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocExt}\OpenWithProgids"; ValueType: string; ValueName: "{#MyAppAssocKey}"; ValueData: ""; Flags: uninsdeletevalue
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}"; ValueType: string; ValueName: ""; ValueData: "{#MyAppAssocName}"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\{#MyAppExeName},0"
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""
; Auto-start registry key (optional task)
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "{#MyAppName}"; ValueData: """{app}\{#MyAppExeName}"" --autostart"; Flags: uninsdeletevalue; Tasks: startupicon

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\User Manual"; Filename: "{app}\User_Manual.txt"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Filename: "{app}\{#MyAppExeName}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\*.log"
Type: filesandordirs; Name: "{localappdata}\ShivayCashBook\cache"
