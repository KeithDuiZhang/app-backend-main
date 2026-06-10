param(
    [string] $JarPath = "D:\Code_Project\app-backend-main\yudao-server\target\yudao-server.jar",
    [int] $ExpectedComponentCount = 72,
    [int] $ExpectedOpusComponentCount = 66,
    [int] $ExpectedDownloadableOpusComponentCount = 54,
    [int] $ExpectedPlannedOpusComponentCount = 12,
    [int] $ExpectedBusinessPackCount = 8,
    [string] $ExpectedRecommendedPackId = "opus_zh_core_v1",
    [int] $ExpectedRecommendedPackComponentCount = 6,
    [long] $ExpectedRecommendedPackBytes = 460419006,
    [int] $ExpectedImagePackComponentCount = 0,
    [long] $ExpectedImagePackBytes = 0,
    [int] $ExpectedConversationPackComponentCount = 3,
    [long] $ExpectedConversationPackBytes = 902832184,
    [string] $ExpectedSmall100BusinessPackId = "small100_multi_v1",
    [string] $ExpectedSmall100ComponentId = "text-small100-multi",
    [int] $ExpectedSmall100PackComponentCount = 1,
    [long] $ExpectedSmall100PackBytes = 1866441897,
    [int] $ExpectedSmall100RequiredFileCount = 8,
    [string] $ExpectedSmall100ReleaseStatus = ""
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression.FileSystem

if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    throw "JAR not found: $JarPath"
}

function Read-JarJson([System.IO.Compression.ZipArchive] $Zip, [string] $EntryName) {
    $entry = $Zip.Entries | Where-Object { $_.FullName -eq $EntryName } | Select-Object -First 1
    if ($null -eq $entry) {
        throw "JAR entry not found: $EntryName"
    }
    $reader = New-Object System.IO.StreamReader($entry.Open(), [System.Text.Encoding]::UTF8)
    try {
        return $reader.ReadToEnd() | ConvertFrom-Json
    } finally {
        $reader.Dispose()
    }
}

function As-Array([object] $Value) {
    $items = @()
    foreach ($item in $Value) {
        $items += $item
    }
    return $items
}

function Test-PlannedComponent([object] $Component) {
    return ([string]$Component.releaseStatus -match "planned") -or
        [string]::IsNullOrWhiteSpace([string]$Component.url) -or
        [string]::IsNullOrWhiteSpace([string]$Component.sha256)
}

$blockedIds = @(
    "text-opus-marian-en-ko"
)

$zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
try {
    $components = As-Array (Read-JarJson $zip "BOOT-INF/classes/offline-models/component-packs.json")
    $businessPacks = As-Array (Read-JarJson $zip "BOOT-INF/classes/offline-models/business-packs.json")
} finally {
    $zip.Dispose()
}

$componentIds = @($components | ForEach-Object { $_.packId })
$opusComponents = @($components | Where-Object { $_.packId -like "text-opus-marian-*" })
$downloadableOpusComponents = @($opusComponents | Where-Object { -not (Test-PlannedComponent $_) })
$plannedOpusComponents = @($opusComponents | Where-Object { Test-PlannedComponent $_ })
$blockedPresent = @($componentIds | Where-Object { $blockedIds -contains $_ })
$componentById = @{}
foreach ($component in $components) {
    $componentById[$component.packId] = $component
}
$missingOpusMetadata = @($downloadableOpusComponents | Where-Object {
    [string]::IsNullOrWhiteSpace($_.url) -or
    [string]::IsNullOrWhiteSpace($_.sha256) -or
    [string]::IsNullOrWhiteSpace($_.manifestUrl) -or
    @($_.requiredFiles).Count -eq 0
})

$textPack = @($businessPacks | Where-Object { $_.packId -eq "offline-text-translation-full" })[0]
$imagePack = @($businessPacks | Where-Object { $_.packId -eq "ocr_photo_recognition_v1" })[0]
$conversationPack = @($businessPacks | Where-Object { $_.packId -eq "offline_dialog_voice_v1" })[0]
$recommendedPack = @($businessPacks | Where-Object { $_.packId -eq $ExpectedRecommendedPackId })[0]
$small100Pack = @($businessPacks | Where-Object { $_.packId -eq $ExpectedSmall100BusinessPackId })[0]
$small100Component = @($components | Where-Object { $_.packId -eq $ExpectedSmall100ComponentId })[0]
$expectedImageComponents = @()
$expectedConversationComponents = @("asr-sensevoice-core", "asr-whisper-wide", "tts-sherpa-core")
$expectedSmall100Components = @($ExpectedSmall100ComponentId)
$imageComponents = @($imagePack.components)
$conversationComponents = @($conversationPack.components)
$small100Components = @($small100Pack.components)
$small100RequiredFiles = @($small100Component.requiredFiles)
$imageComputedBytes = 0L
$conversationComputedBytes = 0L
foreach ($componentId in $imageComponents) {
    if ($componentById.ContainsKey($componentId)) {
        $imageComputedBytes += [long]$componentById[$componentId].sizeBytes
    }
}
foreach ($componentId in $conversationComponents) {
    if ($componentById.ContainsKey($componentId)) {
        $conversationComputedBytes += [long]$componentById[$componentId].sizeBytes
    }
}
$imageComponentDiff = @(Compare-Object -ReferenceObject $expectedImageComponents -DifferenceObject $imageComponents)
$conversationComponentDiff = @(Compare-Object -ReferenceObject $expectedConversationComponents -DifferenceObject $conversationComponents)
$small100ComponentDiff = @(Compare-Object -ReferenceObject $expectedSmall100Components -DifferenceObject $small100Components)
$small100ReleaseStatusOk = [string]::IsNullOrWhiteSpace($ExpectedSmall100ReleaseStatus) -or
    [string]$small100Component.releaseStatus -eq $ExpectedSmall100ReleaseStatus
$recommendedComponents = @()
$recommendedMissingComponents = @()
$recommendedComputedBytes = 0L
$recommendedExcludedIds = @("text-hymt-core", "text-m2m100-418m-int8", "ocr-tesseract-core", "asr-whisper-wide")
$recommendedExcludedPresent = @()
if ($null -ne $recommendedPack) {
    $recommendedComponents = @($recommendedPack.components)
    $recommendedMissingComponents = @($recommendedComponents | Where-Object { -not $componentById.ContainsKey($_) })
    foreach ($componentId in $recommendedComponents) {
        if ($componentById.ContainsKey($componentId)) {
            $recommendedComputedBytes += [long]$componentById[$componentId].sizeBytes
        }
    }
    $recommendedExcludedPresent = @($recommendedComponents | Where-Object { $recommendedExcludedIds -contains $_ })
}

$ok = $components.Count -eq $ExpectedComponentCount -and
    $opusComponents.Count -eq $ExpectedOpusComponentCount -and
    $downloadableOpusComponents.Count -eq $ExpectedDownloadableOpusComponentCount -and
    $plannedOpusComponents.Count -eq $ExpectedPlannedOpusComponentCount -and
    $businessPacks.Count -eq $ExpectedBusinessPackCount -and
    $blockedPresent.Count -eq 0 -and
    $missingOpusMetadata.Count -eq 0 -and
    @($textPack.components).Count -eq 55 -and
    $null -ne $small100Pack -and
    $null -ne $small100Component -and
    $small100Components.Count -eq $ExpectedSmall100PackComponentCount -and
    $small100ComponentDiff.Count -eq 0 -and
    [long]$small100Pack.sizeBytes -eq $ExpectedSmall100PackBytes -and
    [long]$small100Component.sizeBytes -eq $ExpectedSmall100PackBytes -and
    $small100RequiredFiles.Count -eq $ExpectedSmall100RequiredFileCount -and
    $small100ReleaseStatusOk -and
    $imageComponents.Count -eq $ExpectedImagePackComponentCount -and
    $imageComponentDiff.Count -eq 0 -and
    [long]$imagePack.sizeBytes -eq $ExpectedImagePackBytes -and
    $imageComputedBytes -eq $ExpectedImagePackBytes -and
    $conversationComponents.Count -eq $ExpectedConversationPackComponentCount -and
    $conversationComponentDiff.Count -eq 0 -and
    [long]$conversationPack.sizeBytes -eq $ExpectedConversationPackBytes -and
    $conversationComputedBytes -eq $ExpectedConversationPackBytes -and
    $null -ne $recommendedPack -and
    $recommendedComponents.Count -eq $ExpectedRecommendedPackComponentCount -and
    [long]$recommendedPack.sizeBytes -eq $ExpectedRecommendedPackBytes -and
    $recommendedComputedBytes -eq $ExpectedRecommendedPackBytes -and
    $recommendedMissingComponents.Count -eq 0 -and
    $recommendedExcludedPresent.Count -eq 0

$result = [pscustomobject]@{
    jarPath = $JarPath
    jarBytes = (Get-Item -LiteralPath $JarPath).Length
    componentCount = $components.Count
    opusComponentCount = $opusComponents.Count
    downloadableOpusComponentCount = $downloadableOpusComponents.Count
    plannedOpusComponentCount = $plannedOpusComponents.Count
    businessPackCount = $businessPacks.Count
    blockedComponentCount = $blockedPresent.Count
    opusMissingMetadataCount = $missingOpusMetadata.Count
    textPackComponents = @($textPack.components).Count
    small100BusinessPackPresent = $null -ne $small100Pack
    small100ComponentPresent = $null -ne $small100Component
    small100BusinessPackComponents = $small100Components.Count
    small100BusinessPackBytes = $(if ($null -ne $small100Pack) { [long]$small100Pack.sizeBytes } else { 0 })
    small100ComponentBytes = $(if ($null -ne $small100Component) { [long]$small100Component.sizeBytes } else { 0 })
    small100RequiredFileCount = $small100RequiredFiles.Count
    small100ReleaseStatus = $(if ($null -ne $small100Component) { [string]$small100Component.releaseStatus } else { "" })
    small100ReleaseStatusOk = $small100ReleaseStatusOk
    imagePackComponents = $imageComponents.Count
    imagePackBytes = $(if ($null -ne $imagePack) { [long]$imagePack.sizeBytes } else { 0 })
    imagePackComputedBytes = $imageComputedBytes
    imagePackComponentDiffCount = $imageComponentDiff.Count
    conversationPackComponents = $conversationComponents.Count
    conversationPackBytes = $(if ($null -ne $conversationPack) { [long]$conversationPack.sizeBytes } else { 0 })
    conversationPackComputedBytes = $conversationComputedBytes
    conversationPackComponentDiffCount = $conversationComponentDiff.Count
    recommendedPackId = $ExpectedRecommendedPackId
    recommendedPackPresent = $null -ne $recommendedPack
    recommendedPackComponents = $recommendedComponents.Count
    recommendedPackBytes = $(if ($null -ne $recommendedPack) { [long]$recommendedPack.sizeBytes } else { 0 })
    recommendedPackComputedBytes = $recommendedComputedBytes
    recommendedPackMissingComponentCount = $recommendedMissingComponents.Count
    recommendedPackExcludedComponentCount = $recommendedExcludedPresent.Count
    status = $(if ($ok) { "PASS" } else { "FAIL" })
}

$result | ConvertTo-Json -Depth 8

if (-not $ok) {
    exit 1
}
