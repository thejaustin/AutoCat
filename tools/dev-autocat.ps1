param(
    [string[]]$GradleArgs = @("assembleLawnWithQuickstepGithubDebug"),
    [switch]$UseConfigurationCache,
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$jbr = "C:\Program Files\Android\Android Studio\jbr"
$sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"

$env:JAVA_HOME = $jbr
$env:PATH = "$jbr\bin;$env:PATH"
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:GRADLE_USER_HOME = Join-Path $repoRoot ".gradle-user-home"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT"
Write-Host "GRADLE_USER_HOME=$env:GRADLE_USER_HOME"

if ($NoBuild) {
    return
}

$wrapperArgs = @()
if (-not $UseConfigurationCache) {
    $wrapperArgs += "--no-configuration-cache"
}
$wrapperArgs += $GradleArgs

Push-Location $repoRoot
try {
    & .\gradlew.bat @wrapperArgs
} finally {
    Pop-Location
}
