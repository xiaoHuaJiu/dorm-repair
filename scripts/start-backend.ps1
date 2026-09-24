[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$backendDirectory = Join-Path $projectRoot 'backend'
$mavenRepository = Join-Path $projectRoot '.m2\repository'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw "缺少本机环境变量文件：$envFile"
}

$values = @{}
Get-Content -LiteralPath $envFile -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $values[$matches[1]] = $matches[2]
    }
}

$requiredVariables = @(
    'MYSQL_PORT',
    'MYSQL_DATABASE',
    'MYSQL_USER',
    'MYSQL_PASSWORD',
    'REDIS_PASSWORD',
    'MINIO_ROOT_USER',
    'MINIO_ROOT_PASSWORD',
    'MINIO_BUCKET_NAME',
    'JWT_SECRET'
)

foreach ($name in $requiredVariables) {
    if ([string]::IsNullOrWhiteSpace($values[$name])) {
        throw "本机环境变量文件缺少有效配置：$name"
    }
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
$env:JWT_TTL = if ($values['JWT_TTL']) { $values['JWT_TTL'] } else { '8h' }
$env:DEV_SEED_ENABLED = if ($values['DEV_SEED_ENABLED']) { $values['DEV_SEED_ENABLED'] } else { 'false' }
foreach ($name in @('DEV_ADMIN_USERNAME','DEV_ADMIN_PASSWORD','DEV_WORKER_USERNAME','DEV_WORKER_PASSWORD','DEV_STUDENT_USERNAME','DEV_STUDENT_PASSWORD','DEV_DISABLED_USERNAME','DEV_DISABLED_PASSWORD')) {
    if ($values[$name]) { Set-Item -Path "Env:$name" -Value $values[$name] }
}

Set-Location -LiteralPath $backendDirectory
mvn "-Dmaven.repo.local=$mavenRepository" spring-boot:run
exit $LASTEXITCODE
