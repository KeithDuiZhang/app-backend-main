param(
    [string] $AppBaseUrl = "https://translation.superpowersai.cn/app-api",
    [string] $AdminBaseUrl = "https://translation.superpowersai.cn/admin-api",
    [string] $PublishedRoot = "D:\Code_Project\md_CN_model_repo\published\model-repo\cn\1.0.0",
    [string] $Mobile = "19927621043",
    [string] $PaymentReturnUrl = "https://translation.superpowersai.cn/app-api/pay/alipay/return",
    [int] $ExpectedComponentCount = 59,
    [int] $ExpectedOpusModelCount = 54,
    [switch] $PublishLocal,
    [switch] $SkipCatalog,
    [switch] $SkipSms,
    [switch] $StrictCatalog,
    [switch] $CheckDownloadRange,
    [switch] $CheckPayment
)

$ErrorActionPreference = "Stop"

function Normalize-BaseUrl([string] $Value) {
    return $Value.TrimEnd("/")
}

function New-BearerHeaders([string] $EnvName) {
    $token = [Environment]::GetEnvironmentVariable($EnvName)
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "Required environment variable is not set: $EnvName"
    }
    return @{ Authorization = "Bearer $token" }
}

function Invoke-JsonRequest([string] $Method, [string] $Uri, [hashtable] $Headers = @{}, [object] $Body = $null) {
    $request = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $request.ContentType = "application/json"
        $request.Body = ($Body | ConvertTo-Json -Depth 16)
    }
    $response = Invoke-WebRequest @request
    $data = $null
    if (-not [string]::IsNullOrWhiteSpace($response.Content)) {
        $data = $response.Content | ConvertFrom-Json
    }
    return [pscustomobject]@{
        StatusCode = $response.StatusCode
        Data = $data
    }
}

function Add-Result([System.Collections.Generic.List[object]] $Results, [string] $Name, [string] $Status, [hashtable] $Details = @{}) {
    $Results.Add([pscustomobject]@{
        check = $Name
        status = $Status
        details = [pscustomobject]$Details
    }) | Out-Null
}

function Has-SignedUrlLeak([object] $Value) {
    $json = $Value | ConvertTo-Json -Depth 80
    return $json -match "downloadUrl|X-Amz-Signature|X-Amz-Credential|X-Amz-Algorithm"
}

function Get-Catalog() {
    return Invoke-JsonRequest -Method "GET" -Uri "$script:AppBase/offline-models/catalog"
}

function Select-FirstDownloadableOpusComponent([object] $CatalogData) {
    return @($CatalogData.components | Where-Object {
        $_.packId -like "text-opus-marian-*" -and
        -not [string]::IsNullOrWhiteSpace($_.url) -and
        @($_.requiredFiles).Count -gt 0
    } | Select-Object -First 1)[0]
}

$script:AppBase = Normalize-BaseUrl $AppBaseUrl
$script:AdminBase = Normalize-BaseUrl $AdminBaseUrl
$results = [System.Collections.Generic.List[object]]::new()
$catalogResponse = $null

if ($PublishLocal) {
    try {
        if (-not (Test-Path -LiteralPath $PublishedRoot -PathType Container)) {
            throw "PublishedRoot does not exist"
        }
        foreach ($required in @("index.json", "component-packs.json", "business-packs.json")) {
            if (-not (Test-Path -LiteralPath (Join-Path $PublishedRoot $required) -PathType Leaf)) {
                throw "PublishedRoot is missing $required"
            }
        }
        $headers = New-BearerHeaders "ADMIN_AUTH_BEARER"
        $publish = Invoke-JsonRequest -Method "POST" -Uri "$script:AdminBase/integration/offline-models/publish-local" -Headers $headers -Body @{
            localRoot = $PublishedRoot.Replace("\", "/")
        }
        $data = $publish.Data.data
        $ok = $publish.StatusCode -eq 200 -and $publish.Data.code -eq 0 -and $data.uploadedFiles -gt 0
        Add-Result $results "cos-publish-local" ($(if ($ok) { "PASS" } else { "FAIL" })) @{
            httpOk = $publish.StatusCode -eq 200
            codeOk = $publish.Data.code -eq 0
            uploadedFiles = $data.uploadedFiles
            uploadedBytes = $data.uploadedBytes
            verifiedFiles = $data.verifiedFiles
            verifiedBytes = $data.verifiedBytes
            keyJsonObjects = @($data.keyJsonObjects).Count
            prefixConfigured = -not [string]::IsNullOrWhiteSpace($data.prefix)
        }
    } catch {
        Add-Result $results "cos-publish-local" "FAIL" @{ reason = $_.Exception.Message }
    }
}

if (-not $SkipCatalog) {
    try {
        $catalogResponse = Get-Catalog
        $catalog = $catalogResponse.Data
        $components = @($catalog.data.components)
        $models = @($catalog.data.models)
        $opusModels = @($models | Where-Object { $_.modelId -like "text-opus-marian-*" })
        $opusDownloadable = @($opusModels | Where-Object { $_.capabilityStatus -eq "downloadable" })
        $signedUrlLeak = Has-SignedUrlLeak $catalog
        $strictOk = -not $StrictCatalog -or (
            $catalog.data.schemaVersion -eq 2 -and
            $components.Count -eq $ExpectedComponentCount -and
            $opusModels.Count -eq $ExpectedOpusModelCount -and
            $opusDownloadable.Count -eq $ExpectedOpusModelCount
        )
        $ok = $catalogResponse.StatusCode -eq 200 -and $catalog.code -eq 0 -and -not $signedUrlLeak -and $strictOk
        Add-Result $results "offline-model-catalog" ($(if ($ok) { "PASS" } else { "FAIL" })) @{
            httpOk = $catalogResponse.StatusCode -eq 200
            codeOk = $catalog.code -eq 0
            schemaVersion = $catalog.data.schemaVersion
            componentCount = $components.Count
            opusModelCount = $opusModels.Count
            opusDownloadableCount = $opusDownloadable.Count
            signedUrlLeak = $signedUrlLeak
            strictCatalog = [bool]$StrictCatalog
        }
    } catch {
        Add-Result $results "offline-model-catalog" "FAIL" @{ reason = $_.Exception.Message }
    }
}

if (-not $SkipSms) {
    try {
        $sms = Invoke-JsonRequest -Method "POST" -Uri "$script:AppBase/auth/sms-code/send" -Body @{
            mobile = $Mobile
            scene = 1
        }
        $ok = $sms.StatusCode -ne 401 -and $sms.Data.code -eq 0
        Add-Result $results "sms-code-send" ($(if ($ok) { "PASS" } else { "FAIL" })) @{
            httpNot401 = $sms.StatusCode -ne 401
            codeOk = $sms.Data.code -eq 0
        }
    } catch {
        Add-Result $results "sms-code-send" "FAIL" @{ reason = $_.Exception.Message }
    }
}

if ($CheckDownloadRange) {
    try {
        $headers = New-BearerHeaders "APP_AUTH_BEARER"
        if ($null -eq $catalogResponse -or $null -eq $catalogResponse.Data) {
            $catalogResponse = Get-Catalog
        }
        $component = Select-FirstDownloadableOpusComponent $catalogResponse.Data.data
        if ($null -eq $component) {
            throw "No downloadable OPUS component is present in the production catalog"
        }
        $download = Invoke-JsonRequest -Method "POST" -Uri "$script:AppBase/offline-models/download-urls" -Headers $headers -Body @{
            componentIds = @($component.packId)
        }
        $signedComponent = @($download.Data.data.components)[0]
        $signedUrl = $signedComponent.downloadUrl
        if ([string]::IsNullOrWhiteSpace($signedUrl)) {
            throw "download-urls response did not contain a signed URL"
        }
        $tmp = New-TemporaryFile
        try {
            $rangeResponse = Invoke-WebRequest -Uri $signedUrl -Headers @{ Range = "bytes=0-0" } -OutFile $tmp -PassThru -UseBasicParsing
            $bytes = (Get-Item -LiteralPath $tmp).Length
            $rangeOk = $rangeResponse.StatusCode -in @(200, 206) -and $bytes -ge 1
            Add-Result $results "download-url-range-read" ($(if ($rangeOk -and $download.Data.code -eq 0) { "PASS" } else { "FAIL" })) @{
                codeOk = $download.Data.code -eq 0
                componentId = $component.packId
                rangeStatusCode = $rangeResponse.StatusCode
                bytesRead = $bytes
            }
        } finally {
            Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
            Remove-Variable signedUrl -ErrorAction SilentlyContinue
        }
    } catch {
        Add-Result $results "download-url-range-read" "FAIL" @{ reason = $_.Exception.Message }
    }
}

if ($CheckPayment) {
    try {
        $headers = New-BearerHeaders "APP_AUTH_BEARER"
        $products = Invoke-JsonRequest -Method "GET" -Uri "$script:AppBase/pay/products/offline-membership"
        $product = @($products.Data.data | Where-Object { $_.priceCent -gt 0 -and $_.status -ne 0 } | Select-Object -First 1)[0]
        if ($null -eq $product) {
            $product = @($products.Data.data | Select-Object -First 1)[0]
        }
        if ($null -eq $product) {
            throw "No offline membership product is available"
        }
        $pay = Invoke-JsonRequest -Method "POST" -Uri "$script:AppBase/pay/alipay/wap/create" -Headers $headers -Body @{
            productType = "offline_membership"
            productId = $product.id
            clientType = "android"
            returnUrl = $PaymentReturnUrl
        }
        $payData = $pay.Data.data
        $hasPayUrl = -not [string]::IsNullOrWhiteSpace($payData.payUrl)
        $ok = $pay.StatusCode -eq 200 -and $pay.Data.code -eq 0 -and $hasPayUrl
        Add-Result $results "alipay-wap-create" ($(if ($ok) { "PASS" } else { "FAIL" })) @{
            httpOk = $pay.StatusCode -eq 200
            codeOk = $pay.Data.code -eq 0
            hasPayUrl = $hasPayUrl
            displayMode = $payData.displayMode
            orderNoSet = -not [string]::IsNullOrWhiteSpace($payData.orderNo)
        }
    } catch {
        Add-Result $results "alipay-wap-create" "FAIL" @{ reason = $_.Exception.Message }
    }
}

$summary = [pscustomobject]@{
    checkedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    appBaseUrl = $script:AppBase
    adminBaseUrl = $script:AdminBase
    results = $results
}

$summary | ConvertTo-Json -Depth 12

if (@($results | Where-Object { $_.status -eq "FAIL" }).Count -gt 0) {
    exit 1
}
