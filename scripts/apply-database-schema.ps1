[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$schemaFile = Join-Path $projectRoot 'doc\database\dorm_repair_schema.sql'
$formalTables = @(
    'sys_user','repair_area','repair_worker','repair_fault_type','repair_worker_fault_type',
    'repair_worker_area_scope','repair_work_schedule','repair_order','repair_process_record',
    'repair_material_usage','repair_transfer_request','repair_leave_request','repair_rework_record',
    'repair_evaluation','repair_order_flow','repair_reminder_record','repair_holiday_calendar',
    'repair_dispatch_alert','sys_idempotent_record','sys_operation_log'
)

if (-not (Test-Path -LiteralPath $schemaFile -PathType Leaf)) {
    throw "缺少数据库结构文件：$schemaFile"
}

$tableList = ($formalTables | ForEach-Object { "'$_'" }) -join ','
$query = "SELECT table_name FROM information_schema.tables WHERE table_schema='dorm_repair' AND table_name IN ($tableList) ORDER BY table_name;"
$existing = @(docker exec dorm-repair-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --batch --skip-column-names -e "$1"' sh $query 2>$null)
if ($LASTEXITCODE -ne 0) { throw '无法读取 dorm_repair 当前表结构。' }

 $missing = @($formalTables | Where-Object { $_ -notin $existing })
$isSupportedUpgrade = $existing.Count -ge 17 -and @($missing | Where-Object { $_ -notin @('sys_idempotent_record','repair_holiday_calendar','repair_dispatch_alert') }).Count -eq 0
if ($existing.Count -gt 0 -and $existing.Count -lt $formalTables.Count -and -not $isSupportedUpgrade) {
    throw "检测到部分正式表已存在（$($existing.Count)/20）。为避免掩盖结构冲突，本脚本不会继续，请先核对这些表：$($existing -join ', ')"
}

if ($existing.Count -eq $formalTables.Count) {
    & (Join-Path $PSScriptRoot 'verify-database-schema.ps1')
    if ($LASTEXITCODE -ne 0) { throw '现有正式表未通过结构验证，未执行覆盖。' }
    Write-Output '20 张正式表已经存在且通过验证，无需重复执行 DDL。'
    exit 0
}

Get-Content -Raw -LiteralPath $schemaFile -Encoding UTF8 |
    docker exec -i dorm-repair-mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' 2>$null
if ($LASTEXITCODE -ne 0) { throw 'DDL 执行失败，请查看 MySQL 容器日志。' }

Write-Output 'DDL 已执行，开始验证数据库结构。'
& (Join-Path $PSScriptRoot 'verify-database-schema.ps1')
exit $LASTEXITCODE
