[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$formalTables = @(
    'sys_user','repair_area','repair_worker','repair_fault_type','repair_worker_fault_type',
    'repair_worker_area_scope','repair_work_schedule','repair_order','repair_process_record',
    'repair_material_usage','repair_transfer_request','repair_leave_request','repair_rework_record',
    'repair_evaluation','repair_order_flow','repair_reminder_record','repair_holiday_calendar',
    'repair_dispatch_alert','repair_order_alert','sys_idempotent_record','sys_operation_log'
)
$requiredIndexes = @(
    'idx_order_duplicate_check','idx_order_assignee_status','idx_order_status_report',
    'idx_order_accept_timeout','idx_order_complete_timeout','idx_order_exception',
    'idx_order_duplicate_relation','uk_worker_fault','idx_fault_worker','idx_scope_worker',
    'idx_scope_location','idx_schedule_effective','idx_leave_worker_time',
    'idx_leave_pending_review','idx_leave_start_task','idx_leave_end_task',
    'idx_process_order_time','idx_process_worker_time','idx_process_order_type',
    'idx_material_order_time','idx_material_worker_time','uk_rework_order_no',
    'idx_rework_order_time','idx_rework_admin','idx_flow_order_time',
    'uk_evaluation_order','uk_reminder_deduplicate','uk_idempotent_biz_user_no',
    'uk_holiday_date','idx_holiday_year','uk_dispatch_alert_open','idx_dispatch_alert_status_time',
    'uk_order_alert_rework_type','idx_order_alert_status_time'
)

function Invoke-DatabaseScalar([string]$Sql) {
    $result = docker exec dorm-repair-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --batch --skip-column-names -e "$1"' sh $Sql 2>$null
    if ($LASTEXITCODE -ne 0) { throw '数据库结构验证查询失败。' }
    return @($result)
}

$tableList = ($formalTables | ForEach-Object { "'$_'" }) -join ','
$actualTables = Invoke-DatabaseScalar "SELECT table_name FROM information_schema.tables WHERE table_schema='dorm_repair' AND table_name IN ($tableList) ORDER BY table_name;"
$missingTables = @($formalTables | Where-Object { $_ -notin $actualTables })
if ($missingTables.Count -gt 0) { throw "缺少正式表：$($missingTables -join ', ')" }

$invalidTables = Invoke-DatabaseScalar "SELECT table_name FROM information_schema.tables WHERE table_schema='dorm_repair' AND table_name IN ($tableList) AND (engine <> 'InnoDB' OR table_collation NOT LIKE 'utf8mb4%');"
if ($invalidTables.Count -gt 0) { throw "引擎或字符集不符合规范：$($invalidTables -join ', ')" }

$foreignKeyCount = Invoke-DatabaseScalar "SELECT COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema='dorm_repair' AND table_name IN ($tableList);"
if ([int]$foreignKeyCount -ne 0) { throw "检测到非预期外键：$foreignKeyCount" }

$indexList = ($requiredIndexes | ForEach-Object { "'$_'" }) -join ','
$actualIndexes = Invoke-DatabaseScalar "SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema='dorm_repair' AND index_name IN ($indexList);"
$missingIndexes = @($requiredIndexes | Where-Object { $_ -notin $actualIndexes })
if ($missingIndexes.Count -gt 0) { throw "缺少关键索引：$($missingIndexes -join ', ')" }

Write-Output "数据库结构验证通过：正式表 $($actualTables.Count)/21，关键索引 $($actualIndexes.Count)/$($requiredIndexes.Count)，外键 0。"
