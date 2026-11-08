$ErrorActionPreference = 'Stop'
$configPath = Join-Path $PSScriptRoot '../.env'
if (!(Test-Path -LiteralPath $configPath)) { throw 'Create MoonCake/.env from .env.example first.' }
$settings = @{}
Get-Content -LiteralPath $configPath | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') { $settings[$matches[1]] = $matches[2].Trim() }
}
foreach ($key in @('VNPAY_ENABLED','VNPAY_TMN_CODE','VNPAY_HASH_SECRET','APP_PUBLIC_URL')) {
    $processValue = [Environment]::GetEnvironmentVariable($key)
    if ($null -ne $processValue) { $settings[$key] = $processValue }
}
if ($settings['VNPAY_ENABLED'] -ne 'true') { throw 'Set VNPAY_ENABLED=true.' }
foreach ($key in @('VNPAY_TMN_CODE','VNPAY_HASH_SECRET')) {
    if ([string]::IsNullOrWhiteSpace($settings[$key])) { throw "Missing $key. Use credentials issued by VNPay Sandbox." }
}
$baseUrl = $settings['APP_PUBLIC_URL']
if ([string]::IsNullOrWhiteSpace($baseUrl)) { $baseUrl = 'http://localhost:8080' }
$uri = [Uri]$baseUrl
if (!$uri.IsAbsoluteUri -or $uri.Scheme -notin @('http','https')) { throw 'APP_PUBLIC_URL must be an absolute http(s) URL.' }
Write-Output 'Merchant fields are present. Credentials have not been verified with VNPay.'
if ($uri.IsLoopback) { Write-Warning 'Localhost cannot receive VNPay IPN. Configure a public HTTPS URL/tunnel in APP_PUBLIC_URL and register its IPN URL in VNPay portal.' }
Write-Output 'IPN path to register in VNPay portal: /payments/vnpay/ipn'
Write-Output 'Return path: /payments/vnpay/return'
Write-Output 'Start from MoonCake directory: .\mvnw.cmd spring-boot:run'
