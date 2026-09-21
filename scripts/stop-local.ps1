$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $projectRoot '.runtime/app.pid'
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Output 'No background moamoa PID recorded.'; return }
$appProcessId = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
$appProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $appProcessId"
if (-not $appProcess) { Write-Output 'The recorded process has already stopped.'; return }
$appVersion = (Get-Content -LiteralPath (Join-Path $projectRoot 'VERSION') -Raw).Trim()
$expectedJar = Join-Path $projectRoot "build/libs/moamoa-$appVersion.jar"
if ($appProcess.Name -ne 'java.exe' -or -not $appProcess.CommandLine.Contains($expectedJar)) {
    throw 'The PID does not match this project. Refusing to stop another application.'
}
Stop-Process -Id $appProcessId
Write-Output "Stopped moamoa PID $appProcessId."
