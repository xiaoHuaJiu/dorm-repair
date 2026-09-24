[CmdletBinding()]
param(
    [switch]$Full
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$values = @{}
Get-Content -LiteralPath (Join-Path $projectRoot '.env') -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $values[$matches[1]] = $matches[2] }
}

$env:DB_HOST = 'localhost'
$env:DB_PORT = $values['MYSQL_PORT']
$env:DB_NAME = $values['MYSQL_DATABASE']
$env:DB_USER = $values['MYSQL_USER']
$env:DB_PASSWORD = $values['MYSQL_PASSWORD']
$env:REDIS_HOST = 'localhost'
$env:REDIS_PORT = '6379'
$env:REDIS_PASSWORD = $values['REDIS_PASSWORD']
$env:MINIO_ENDPOINT = 'http://localhost:9000'
$env:MINIO_ACCESS_KEY = $values['MINIO_ROOT_USER']
$env:MINIO_SECRET_KEY = $values['MINIO_ROOT_PASSWORD']
$env:MINIO_BUCKET_NAME = $values['MINIO_BUCKET_NAME']
$env:JWT_SECRET = $values['JWT_SECRET']
$env:DEV_SEED_ENABLED = 'false'

Set-Location -LiteralPath (Join-Path $projectRoot 'backend')
if ($Full) {
    Write-Host 'PLAN-8 Maven 全量回归测试'
    mvn test
} else {
    Write-Host 'PLAN-8 专项、MySQL 全链路与并发审批测试'
    mvn '-Dtest=TransferContractTest,WorkerTransferRequestServiceTest,AdminTransferRequestServiceTest,Plan8MySqlIntegrationTest,Plan8ConcurrencyMySqlTest' test
}
$testExitCode = $LASTEXITCODE
$reportNames = if ($Full) { @() } else { @(
    'AdminTransferRequestServiceTest', 'Plan8ConcurrencyMySqlTest', 'Plan8MySqlIntegrationTest',
    'TransferContractTest', 'WorkerTransferRequestServiceTest'
) }
$reports = Get-ChildItem -LiteralPath 'target/surefire-reports' -Filter 'TEST-*.xml' | Where-Object {
    $Full -or $reportNames -contains $_.BaseName.Replace('TEST-com.dormrepair.transfer.', '')
}
$tests = 0; $failures = 0; $errors = 0; $skipped = 0
foreach ($report in $reports) {
    [xml]$result = Get-Content -LiteralPath $report.FullName
    $tests += [int]$result.testsuite.tests
    $failures += [int]$result.testsuite.failures
    $errors += [int]$result.testsuite.errors
    $skipped += [int]$result.testsuite.skipped
}
Clear-Host
$scopeLabel = if ($Full) { '范围：Maven 全量回归' } else { '范围：专项、MySQL 全链路、并发审批' }
Write-Host 'PLAN-8 真实终端验收结果'
Write-Host $scopeLabel
Write-Host "测试数：$tests，失败：$failures，错误：$errors，跳过：$skipped"
if ($testExitCode -eq 0) { Write-Host '结果：BUILD SUCCESS / PLAN-8 验收测试通过' -ForegroundColor Green }
else { Write-Host "结果：BUILD FAILURE，退出码：$testExitCode" -ForegroundColor Red }
Write-Host "执行时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
if ($Host.Name -eq 'ConsoleHost') { Read-Host '按 Enter 关闭此验收终端' | Out-Null }
exit $testExitCode
