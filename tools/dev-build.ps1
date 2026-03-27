# AutoCat Development Build Script
param(
    [string]$Flavor = "LawnWithQuickstepGithub"
)

Write-Host "Building AutoCat Debug APK ($Flavor)..." -ForegroundColor Green

$buildTask = switch ($Flavor) {
    "github" { "assembleLawnWithQuickstepGithubDebug" }
    "nightly" { "assembleLawnWithQuickstepNightlyDebug" }
    "play" { "assembleLawnWithQuickstepPlayDebug" }
    default { "assembleLawnWithQuickstepGithubDebug" }
}

Write-Host "Running: ./gradlew $buildTask" -ForegroundColor Yellow
./gradlew $buildTask

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Build successful!" -ForegroundColor Green
    $apkPath = Get-ChildItem -Recurse -Filter "*debug*.apk" | Select-Object -First 1
    if ($apkPath) {
        Write-Host "APK location: $($apkPath.FullName)" -ForegroundColor Cyan
        Write-Host "Size: $([math]::Round($apkPath.Length / 1MB, 2)) MB" -ForegroundColor Blue
    }
} else {
    Write-Host "✗ Build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}