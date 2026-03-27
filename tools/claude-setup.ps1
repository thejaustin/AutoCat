# Claude Code Setup Script for AutoCat
# This script sets up the development environment for working with Claude Code on AutoCat

Write-Host "Setting up Claude Code environment for AutoCat..." -ForegroundColor Green

# Check if Claude Code is installed
if (!(Get-Command claude -ErrorAction SilentlyContinue)) {
    Write-Host "Installing Claude Code..." -ForegroundColor Yellow
    npm install -g @anthropic-ai/claude-code
}

# Set up project directory
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# Create necessary directories if they don't exist
$dirs = @("docs", "tools", "schemas")
foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir
        Write-Host "Created directory: $dir" -ForegroundColor Blue
    }
}

# Check for Android SDK
if (!$env:ANDROID_HOME) {
    Write-Warning "ANDROID_HOME is not set. Please ensure Android SDK is installed and configured."
}

# Check for Java
try {
    java -version 2>&1 | Out-Null
    Write-Host "Java is available" -ForegroundColor Green
} catch {
    Write-Warning "Java not found. Please install JDK 17 or later."
}

# Check for Gradle
if (Test-Path ".\gradlew.bat") {
    Write-Host "Gradle wrapper found" -ForegroundColor Green
} else {
    Write-Warning "Gradle wrapper not found. Please ensure you're in the correct project directory."
}

# Set up git hooks if .git exists
if (Test-Path ".git") {
    Write-Host "Setting up git hooks..." -ForegroundColor Yellow
    # Add pre-commit hook for linting
    $preCommitHook = @"
#!/bin/sh
echo "Running lint checks..."
./gradlew lint
if [ $? -ne 0 ]; then
    echo "Lint checks failed. Please fix issues before committing."
    exit 1
fi
"@
    
    $hookPath = ".git/hooks/pre-commit"
    $preCommitHook | Out-File -FilePath $hookPath -Encoding UTF8
    
    # Make executable on Unix-like systems
    if ($IsLinux -or $IsMacOS) {
        chmod +x $hookPath
    }
}

# Create development shortcuts
$shortcuts = @{
    "dev-test.ps1" = "./gradlew test"
    "dev-clean.ps1" = "./gradlew clean"
}

foreach ($script in $shortcuts.Keys) {
    $scriptPath = "tools/$script"
    if (!(Test-Path $scriptPath)) {
        $shortcuts[$script] | Out-File -FilePath $scriptPath -Encoding UTF8
        Write-Host "Created script: $scriptPath" -ForegroundColor Blue
    }
}

Write-Host "`nSetup complete! 🚀" -ForegroundColor Green
Write-Host "Available commands:" -ForegroundColor Cyan
Write-Host "  ./tools/dev-build.ps1     - Build debug APK" -ForegroundColor White
Write-Host "  ./tools/dev-install.ps1   - Install on device" -ForegroundColor White
Write-Host "  ./tools/dev-android.ps1   - Android development helper" -ForegroundColor White
Write-Host "  ./tools/dev-test.ps1      - Run tests" -ForegroundColor White  
Write-Host "  ./tools/dev-clean.ps1     - Clean build" -ForegroundColor White
Write-Host "  ./tools/claude-launch.bat - Start Claude Code" -ForegroundColor White
Write-Host "`nUse 'claude' command to start Claude Code in this directory." -ForegroundColor Yellow