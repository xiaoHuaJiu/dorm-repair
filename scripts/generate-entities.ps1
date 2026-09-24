[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDirectory = Join-Path $projectRoot 'backend\src\main\java\com\dormrepair\domain\entity'
$classNames = [ordered]@{
    sys_user='SysUser'; repair_area='RepairArea'; repair_worker='RepairWorker';
    repair_fault_type='RepairFaultType'; repair_worker_fault_type='RepairWorkerFaultType';
    repair_worker_area_scope='RepairWorkerAreaScope'; repair_work_schedule='RepairWorkSchedule';
    repair_order='RepairOrder'; repair_process_record='RepairProcessRecord';
    repair_material_usage='RepairMaterialUsage'; repair_transfer_request='RepairTransferRequest';
    repair_leave_request='RepairLeaveRequest'; repair_rework_record='RepairReworkRecord';
    repair_evaluation='RepairEvaluation'; repair_order_flow='RepairOrderFlow';
    repair_reminder_record='RepairReminderRecord'; sys_idempotent_record='SysIdempotentRecord'; sys_operation_log='SysOperationLog'
}

function Convert-ToCamel([string]$Name) {
    $parts = $Name -split '_'
    $result = $parts[0]
    if ($parts.Count -gt 1) {
        foreach ($part in $parts[1..($parts.Count - 1)]) {
            if ($part.Length -gt 0) { $result += $part.Substring(0,1).ToUpperInvariant() + $part.Substring(1) }
        }
    }
    return $result
}

function Resolve-JavaType([string]$DataType) {
    switch ($DataType.ToLowerInvariant()) {
        'bigint' { 'Long' }
        { $_ -in @('tinyint','int') } { 'Integer' }
        { $_ -in @('varchar','char','text','longtext','json') } { 'String' }
        'date' { 'LocalDate' }
        'time' { 'LocalTime' }
        { $_ -in @('datetime','timestamp') } { 'LocalDateTime' }
        'decimal' { 'BigDecimal' }
        default { throw "未登记的数据库类型：$DataType" }
    }
}

$tableList = ($classNames.Keys | ForEach-Object { "'$_'" }) -join ','
$query = "SELECT table_name,column_name,data_type FROM information_schema.columns WHERE table_schema='dorm_repair' AND table_name IN ($tableList) ORDER BY table_name,ordinal_position;"
$rows = docker exec dorm-repair-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --batch --skip-column-names -e "$1"' sh $query 2>$null
if ($LASTEXITCODE -ne 0) { throw '无法读取数据库字段元数据。' }

New-Item -ItemType Directory -Force -Path $targetDirectory | Out-Null
foreach ($table in $classNames.Keys) {
    $columns = @($rows | Where-Object { ($_ -split "`t")[0] -eq $table })
    if ($columns.Count -eq 0) { throw "表 $table 没有字段。" }
    $types = @($columns | ForEach-Object { Resolve-JavaType (($_ -split "`t")[2]) } | Sort-Object -Unique)
    $imports = @()
    if ('BigDecimal' -in $types) { $imports += 'import java.math.BigDecimal;' }
    if ('LocalDate' -in $types) { $imports += 'import java.time.LocalDate;' }
    if ('LocalDateTime' -in $types) { $imports += 'import java.time.LocalDateTime;' }
    if ('LocalTime' -in $types) { $imports += 'import java.time.LocalTime;' }

    $lines = @('package com.dormrepair.domain.entity;', '')
    if ($imports.Count -gt 0) { $lines += $imports; $lines += '' }
    $className = $classNames[$table]
    $lines += "public class $className {"
    foreach ($column in $columns) {
        $parts = $column -split "`t"
        $lines += "    private $(Resolve-JavaType $parts[2]) $(Convert-ToCamel $parts[1]);"
    }
    $lines += ''
    foreach ($column in $columns) {
        $parts = $column -split "`t"
        $type = Resolve-JavaType $parts[2]
        $field = Convert-ToCamel $parts[1]
        $method = $field.Substring(0,1).ToUpperInvariant() + $field.Substring(1)
        $lines += "    public $type get$method() { return $field; }"
        $lines += "    public void set$method($type $field) { this.$field = $field; }"
    }
    $lines += '}'
    Set-Content -LiteralPath (Join-Path $targetDirectory "$className.java") -Value ($lines -join "`r`n") -Encoding utf8NoBOM
}

Write-Output "已根据数据库元数据生成 $($classNames.Count) 个 Entity。"
