# AutoCat Development Install Script
Write-Host "Installing AutoCat Debug APK..." -ForegroundColor Green

# Check if device/emulator is connected
$devices = adb devices
if ($devices -match "device$") {
    ./gradlew installDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ App installed successfully!" -ForegroundColor Green
        Write-Host "Check your device/emulator for the AutoCat launcher" -ForegroundColor Cyan
    } else {
        Write-Host "✗ Installation failed!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} else {
    Write-Host "✗ No Android device or emulator detected!" -ForegroundColor Red
    Write-Host "Please connect a device or start an emulator and try again." -ForegroundColor Yellow
    exit 1
}