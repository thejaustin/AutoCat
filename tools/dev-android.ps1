# AutoCat Android Development Helper
# Enhanced script with ADB and emulator management

param(
    [string]$Action = "status",
    [string]$Device = "",
    [switch]$Verbose
)

Write-Host "AutoCat Android Development Helper" -ForegroundColor Cyan

# Set Android environment
if (!$env:ANDROID_HOME) {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    Write-Host "Set ANDROID_HOME to: $env:ANDROID_HOME" -ForegroundColor Yellow
}

$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$emulator = "$env:ANDROID_HOME\emulator\emulator.exe"

function Show-DeviceStatus {
    Write-Host "`nChecking device status..." -ForegroundColor Green
    & $adb devices -l
    
    $devices = & $adb devices | Select-String "device$"
    if ($devices) {
        Write-Host "✓ Found $($devices.Count) connected device(s)" -ForegroundColor Green
    } else {
        Write-Host "✗ No devices connected" -ForegroundColor Red
        Write-Host "Available emulators:" -ForegroundColor Yellow
        & $emulator -list-avds
    }
}

function Install-AutoCat {
    Write-Host "`nBuilding and installing AutoCat..." -ForegroundColor Green
    
    # Build first
    ./gradlew assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Build failed!" -ForegroundColor Red
        return
    }
    
    # Find APK
    $apk = Get-ChildItem -Recurse -Filter "*debug*.apk" | Select-Object -First 1
    if (!$apk) {
        Write-Host "✗ APK not found!" -ForegroundColor Red
        return
    }
    
    Write-Host "Installing: $($apk.Name)" -ForegroundColor Cyan
    & $adb install -r $apk.FullName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Installation successful!" -ForegroundColor Green
        Write-Host "Starting AutoCat..." -ForegroundColor Cyan
        & $adb shell am start -n app.lawnchair.debug/com.android.launcher3.uioverrides.QuickstepLauncher
    } else {
        Write-Host "✗ Installation failed!" -ForegroundColor Red
    }
}

function Start-Emulator {
    Write-Host "`nAvailable emulators:" -ForegroundColor Yellow
    $avds = & $emulator -list-avds
    
    if ($avds) {
        $avds | ForEach-Object { Write-Host "  $_" -ForegroundColor White }
        
        if ($Device) {
            Write-Host "`nStarting emulator: $Device" -ForegroundColor Green
            Start-Process -FilePath $emulator -ArgumentList "-avd", $Device -NoNewWindow
        } else {
            Write-Host "`nTo start an emulator, use: .\dev-android.ps1 -Action emulator -Device <name>" -ForegroundColor Cyan
        }
    } else {
        Write-Host "No emulators configured. Create one in Android Studio." -ForegroundColor Red
    }
}

function Show-Logs {
    Write-Host "`nShowing AutoCat logs (Ctrl+C to stop)..." -ForegroundColor Green
    & $adb logcat | Select-String -Pattern "lawnchair|AutoCat|Launcher"
}

function Restart-ADB {
    Write-Host "`nRestarting ADB server..." -ForegroundColor Yellow
    & $adb kill-server
    Start-Sleep -Seconds 2
    & $adb start-server
    Show-DeviceStatus
}

# Main action handling
switch ($Action.ToLower()) {
    "status" { Show-DeviceStatus }
    "install" { Install-AutoCat }
    "emulator" { Start-Emulator }
    "logs" { Show-Logs }
    "restart" { Restart-ADB }
    default {
        Write-Host "Available actions:" -ForegroundColor Yellow
        Write-Host "  status   - Show device status" -ForegroundColor White
        Write-Host "  install  - Build and install AutoCat" -ForegroundColor White
        Write-Host "  emulator - Start an emulator" -ForegroundColor White
        Write-Host "  logs     - Show live logs" -ForegroundColor White
        Write-Host "  restart  - Restart ADB server" -ForegroundColor White
        Write-Host "`nUsage: .\dev-android.ps1 -Action <action> [-Device <name>]" -ForegroundColor Cyan
    }
}