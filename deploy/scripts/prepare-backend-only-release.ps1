param(
    [string] $JarPath = "D:\Code_Project\app-backend-main\yudao-server\target\yudao-server.jar",
    [string] $OutputRoot = "D:\Code_Project\app-backend-main\.codex-tmp\backend-only-release"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$verifyScript = Join-Path $PSScriptRoot "verify-offline-model-jar.ps1"

if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "JAR not found: $JarPath"
}

& $verifyScript -JarPath $JarPath | Out-Host
if (-not $?) {
    exit 1
}

$releaseBackend = Join-Path $OutputRoot "release\backend"
New-Item -ItemType Directory -Path $releaseBackend -Force | Out-Null

$targetJar = Join-Path $releaseBackend "yudao-server.jar"
Copy-Item -LiteralPath $JarPath -Destination $targetJar -Force

$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $targetJar
$manifest = [pscustomobject]@{
    createdAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    repoRoot = $repoRoot.Path
    outputRoot = (Resolve-Path $OutputRoot).Path
    jarPath = $targetJar
    jarBytes = (Get-Item -LiteralPath $targetJar).Length
    jarSha256 = $hash.Hash.ToLowerInvariant()
    deployTarget = "/opt/kunqiong-translation/release/backend/yudao-server.jar"
    deployCommand = "cd /opt/kunqiong-translation && SKIP_FRONTEND_DEPLOY=true FRONTDOOR_MODE=docker-host-nginx PUBLIC_BASE_URL=https://translate.kunqiongai.com ./scripts/deploy.sh"
    postDeployStrictCatalogCommand = "powershell -ExecutionPolicy Bypass -File .\deploy\scripts\verify-offline-model-production.ps1 -SkipSms -StrictCatalog"
}

$manifestPath = Join-Path $OutputRoot "manifest.json"
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
$manifest | ConvertTo-Json -Depth 8
