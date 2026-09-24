[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaDir = Join-Path $projectRoot 'backend\src\main\java\com\dormrepair\domain\mapper'
$xmlDir = Join-Path $projectRoot 'backend\src\main\resources\mapper'
$classNames = [ordered]@{
    sys_user='SysUser'; repair_area='RepairArea'; repair_worker='RepairWorker'; repair_fault_type='RepairFaultType';
    repair_worker_fault_type='RepairWorkerFaultType'; repair_worker_area_scope='RepairWorkerAreaScope';
    repair_work_schedule='RepairWorkSchedule'; repair_order='RepairOrder'; repair_process_record='RepairProcessRecord';
    repair_material_usage='RepairMaterialUsage'; repair_transfer_request='RepairTransferRequest';
    repair_leave_request='RepairLeaveRequest'; repair_rework_record='RepairReworkRecord';
    repair_evaluation='RepairEvaluation'; repair_order_flow='RepairOrderFlow';
    repair_reminder_record='RepairReminderRecord'; sys_idempotent_record='SysIdempotentRecord'; sys_operation_log='SysOperationLog'
}

function Convert-ToCamel([string]$Name) {
    $parts=$Name -split '_'; $result=$parts[0]
    if($parts.Count -gt 1){ foreach($part in $parts[1..($parts.Count-1)]){$result += $part.Substring(0,1).ToUpperInvariant()+$part.Substring(1)} }
    return $result
}
function Resolve-JdbcType([string]$Type) {
    switch($Type.ToLowerInvariant()){
        'bigint'{'BIGINT'}; 'tinyint'{'TINYINT'}; 'int'{'INTEGER'}; 'varchar'{'VARCHAR'}; 'char'{'CHAR'};
        {$_ -in @('text','longtext')}{'LONGVARCHAR'}; 'json'{'VARCHAR'}; 'date'{'DATE'}; 'time'{'TIME'};
        {$_ -in @('datetime','timestamp')}{'TIMESTAMP'}; 'decimal'{'DECIMAL'};
        default{throw "未登记的数据库类型：$Type"}
    }
}

$tableList=($classNames.Keys|ForEach-Object{"'$_'"}) -join ','
$query="SELECT table_name,column_name,data_type FROM information_schema.columns WHERE table_schema='dorm_repair' AND table_name IN ($tableList) ORDER BY table_name,ordinal_position;"
$rows=docker exec dorm-repair-mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --batch --skip-column-names -e "$1"' sh $query 2>$null
if($LASTEXITCODE -ne 0){throw '无法读取数据库字段元数据。'}
New-Item -ItemType Directory -Force -Path $javaDir,$xmlDir | Out-Null

foreach($table in $classNames.Keys){
    $entity=$classNames[$table]; $mapper="${entity}Mapper"; $columns=@($rows|Where-Object{($_ -split "`t")[0] -eq $table})
    $java=@(
        'package com.dormrepair.domain.mapper;','',"import com.dormrepair.domain.entity.$entity;",
        'import org.apache.ibatis.annotations.Mapper;','import org.apache.ibatis.annotations.Param;','',
        '@Mapper',"public interface $mapper {","    $entity selectById(@Param(`"id`") Long id);",'}'
    )
    Set-Content -LiteralPath (Join-Path $javaDir "$mapper.java") -Value ($java -join "`r`n") -Encoding utf8NoBOM

    $resultLines=@(); $columnNames=@()
    foreach($column in $columns){$p=$column -split "`t"; $property=Convert-ToCamel $p[1]; $jdbc=Resolve-JdbcType $p[2]; $resultLines += "    <result column=`"$($p[1])`" property=`"$property`" jdbcType=`"$jdbc`"/>"; $columnNames += $p[1]}
    $resultLines[0]=$resultLines[0].Replace('<result ','<id ')
    $xml=@(
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">',
        "<mapper namespace=`"com.dormrepair.domain.mapper.$mapper`">",
        "  <resultMap id=`"BaseResultMap`" type=`"com.dormrepair.domain.entity.$entity`">"
    ) + $resultLines + @(
        '  </resultMap>','', '  <sql id="Base_Column_List">', "    $($columnNames -join ', ')", '  </sql>','',
        "  <select id=`"selectById`" resultMap=`"BaseResultMap`">", '    SELECT <include refid="Base_Column_List"/>',
        "    FROM $table", '    WHERE id = #{id}', '  </select>', '</mapper>'
    )
    Set-Content -LiteralPath (Join-Path $xmlDir "$mapper.xml") -Value ($xml -join "`r`n") -Encoding utf8NoBOM
}
Write-Output "已生成 $($classNames.Count) 组 Mapper 和 XML。"
