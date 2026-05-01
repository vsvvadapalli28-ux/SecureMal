@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: setup_vm.bat  —  SecureMal Dynamic Analysis VM Setup
:: Creates a VirtualBox VM, configures it, and takes a clean
:: snapshot so SecureMal can use it for dynamic malware analysis.
::
:: USAGE:  setup_vm.bat [VM_NAME] [SNAPSHOT_NAME] [ISO_PATH]
::
::   VM_NAME       Name for the new VM  (default: SecureMal-Clean)
::   SNAPSHOT_NAME Name for the snapshot (default: Clean)
::   ISO_PATH      Path to a Windows ISO (required for a real install)
::
:: Example:
::   setup_vm.bat SecureMal-Clean Clean "C:\ISOs\win10.iso"
:: ============================================================

:: ── Parameters with defaults ─────────────────────────────────
set VM_NAME=%~1
if "!VM_NAME!"=="" set VM_NAME=SecureMal-Clean

set SNAPSHOT_NAME=%~2
if "!SNAPSHOT_NAME!"=="" set SNAPSHOT_NAME=Clean

set ISO_PATH=%~3

:: ── Locate VBoxManage ─────────────────────────────────────────
set VBOX=
if exist "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" (
    set VBOX=C:\Program Files\Oracle\VirtualBox\VBoxManage.exe
)
if defined VBOXMANAGE (
    set VBOX=!VBOXMANAGE!
)
if "!VBOX!"=="" (
    echo [ERROR] VBoxManage not found.
    echo         Install VirtualBox or set the VBOXMANAGE environment variable.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  SecureMal VM Setup
echo  VM Name      : !VM_NAME!
echo  Snapshot     : !SNAPSHOT_NAME!
echo  VBoxManage   : !VBOX!
echo ============================================================
echo.

:: ── Check if VM already exists ───────────────────────────────
"!VBOX!" showvminfo "!VM_NAME!" >nul 2>&1
if !errorlevel! == 0 (
    echo [INFO] VM '!VM_NAME!' already exists. Skipping creation.
    goto :snapshot
)

:: ── Step 1: Create the VM ────────────────────────────────────
echo [1/6] Creating VM '!VM_NAME!'...
"!VBOX!" createvm --name "!VM_NAME!" --ostype Windows10_64 --register
if !errorlevel! neq 0 (
    echo [ERROR] Failed to create VM.
    pause
    exit /b 1
)

:: ── Step 2: Configure hardware ───────────────────────────────
echo [2/6] Configuring hardware (2 GB RAM, 2 CPUs, 64 MB VRAM)...
"!VBOX!" modifyvm "!VM_NAME!" ^
    --memory 2048 ^
    --cpus 2 ^
    --vram 64 ^
    --graphicscontroller vmsvga ^
    --audio none ^
    --usb off ^
    --nic1 nat

:: ── Step 3: Create and attach a 60 GB disk ───────────────────
echo [3/6] Creating 60 GB virtual disk...
set VDI_PATH=%USERPROFILE%\VirtualBox VMs\!VM_NAME!\!VM_NAME!.vdi
"!VBOX!" createhd --filename "!VDI_PATH!" --size 61440 --format VDI
if !errorlevel! neq 0 (
    echo [WARN] Disk creation failed — it may already exist. Continuing...
)

echo [3/6] Attaching disk...
"!VBOX!" storagectl "!VM_NAME!" --name "SATA" --add sata --controller IntelAhci
"!VBOX!" storageattach "!VM_NAME!" ^
    --storagectl "SATA" --port 0 --device 0 ^
    --type hdd --medium "!VDI_PATH!"

:: ── Step 4: Attach ISO (if provided) ─────────────────────────
if "!ISO_PATH!"=="" (
    echo [4/6] No ISO provided. Attaching empty DVD drive...
    echo        You can attach a Windows ISO later via the VirtualBox GUI.
    "!VBOX!" storageattach "!VM_NAME!" ^
        --storagectl "SATA" --port 1 --device 0 ^
        --type dvddrive --medium emptydrive
) else (
    echo [4/6] Attaching ISO: !ISO_PATH!
    "!VBOX!" storageattach "!VM_NAME!" ^
        --storagectl "SATA" --port 1 --device 0 ^
        --type dvddrive --medium "!ISO_PATH!"
)

:: ── Step 5: Start the VM ─────────────────────────────────────
echo.
echo [5/6] Starting VM in GUI mode for OS installation...
echo        Complete the Windows installation, then come back here.
echo.
"!VBOX!" startvm "!VM_NAME!" --type gui

echo.
echo ============================================================
echo  ACTION REQUIRED
echo  1. Install Windows inside the VM
echo  2. Install VirtualBox Guest Additions (for guestcontrol)
echo  3. Create a local user and note the username/password
echo  4. Disable Windows Defender / automatic updates
echo  5. When ready, press any key here to take the clean snapshot
echo ============================================================
pause

:: ── Step 6: Take the clean snapshot ─────────────────────────
:snapshot
echo [6/6] Taking snapshot '!SNAPSHOT_NAME!'...
"!VBOX!" snapshot "!VM_NAME!" take "!SNAPSHOT_NAME!" ^
    --description "SecureMal clean baseline — taken by setup_vm.bat"
if !errorlevel! neq 0 (
    echo [ERROR] Failed to take snapshot. Make sure the VM is running or powered off.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  SUCCESS!  VM '!VM_NAME!' is ready for dynamic analysis.
echo.
echo  Next steps:
echo    1. Open SecureMal
echo    2. Click 'Dynamic Analysis' on any uploaded file
echo    3. The VM Configuration dialog will auto-detect your VM
echo    4. Select '!VM_NAME!' and snapshot '!SNAPSHOT_NAME!', then Save
echo ============================================================
echo.
pause
endlocal
