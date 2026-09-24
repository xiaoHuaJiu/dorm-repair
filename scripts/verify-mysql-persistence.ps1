[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

function Wait-MySqlHealthy {
    param([int]$TimeoutSeconds = 180)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $status = docker inspect --format '{{.State.Health.Status}}' dorm-repair-mysql 2>$null
        if ($status -eq 'healthy') {
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw '等待 dorm-repair-mysql 健康状态超时。'
}

$serviceId = docker compose ps -q mysql
if (-not $serviceId) {
    docker compose up -d mysql
}

Wait-MySqlHealthy

$writeSql = @'
CREATE TABLE IF NOT EXISTS scaffold_persistence_check (
    check_key VARCHAR(64) PRIMARY KEY,
    check_value VARCHAR(64) NOT NULL
);
INSERT INTO scaffold_persistence_check (check_key, check_value)
VALUES ('mysql-container-recreate', 'persisted')
ON DUPLICATE KEY UPDATE check_value = VALUES(check_value);
'@

$writeSql | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'
if ($LASTEXITCODE -ne 0) {
    throw '写入 MySQL 持久化验证数据失败。'
}

docker compose rm --stop --force mysql
if ($LASTEXITCODE -ne 0) {
    throw '删除本项目 MySQL 容器失败。'
}

docker compose up -d mysql
if ($LASTEXITCODE -ne 0) {
    throw '重建本项目 MySQL 容器失败。'
}

Wait-MySqlHealthy

$querySql = "SELECT check_value FROM scaffold_persistence_check WHERE check_key = 'mysql-container-recreate';"
$result = $querySql | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -N -B -u"$MYSQL_USER" "$MYSQL_DATABASE"'
if ($LASTEXITCODE -ne 0) {
    throw '查询 MySQL 持久化验证数据失败。'
}

if ($result.Trim() -ne 'persisted') {
    throw "MySQL 持久化验证失败，实际值：$result"
}

Write-Output 'MySQL 持久化验证通过：容器删除并重建后，测试数据仍然存在。'
