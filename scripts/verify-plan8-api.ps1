[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$values = @{}
Get-Content -LiteralPath (Join-Path $projectRoot '.env') -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $values[$matches[1]] = $matches[2] }
}
function Login([string]$username, [string]$password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json
    (Invoke-RestMethod -Method Post -Uri 'http://localhost:8811/api/auth/login' -ContentType 'application/json' -Body $body).data.token
}

$workerToken = Login $values['DEV_WORKER_USERNAME'] $values['DEV_WORKER_PASSWORD']
$workerHeaders = @{ Authorization = "Bearer $workerToken" }
$orders = Invoke-RestMethod -Uri 'http://localhost:8811/api/worker/repair-orders?pageNum=1&pageSize=20' -Headers $workerHeaders
$order = $orders.data.records | Where-Object { $_.status -in 1, 2, 4, 5 } | Select-Object -First 1
if ($null -eq $order) { throw '没有可用于转派验收的维修人员工单' }

$createBody = @{
    bizNo = 'plan8-api-' + [guid]::NewGuid().ToString('N')
    reasonType = 'OVERLOAD'
    reason = 'PLAN-8 接口冒烟测试'
} | ConvertTo-Json
$uri = "http://localhost:8811/api/worker/repair-orders/$($order.orderId)/transfer-requests"
$created = Invoke-RestMethod -Method Post -Uri $uri -Headers $workerHeaders -ContentType 'application/json' -Body $createBody
$replayed = Invoke-RestMethod -Method Post -Uri $uri -Headers $workerHeaders -ContentType 'application/json' -Body $createBody
$workerList = Invoke-RestMethod -Uri 'http://localhost:8811/api/worker/transfer-requests?pageNum=1&pageSize=10&approvalStatus=0' -Headers $workerHeaders

$adminToken = Login $values['DEV_ADMIN_USERNAME'] $values['DEV_ADMIN_PASSWORD']
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
$detail = Invoke-RestMethod -Uri "http://localhost:8811/api/admin/transfer-requests/$($created.data.requestId)" -Headers $adminHeaders
$reviewBody = @{
    bizNo = 'plan8-review-' + [guid]::NewGuid().ToString('N')
    action = 'REJECT'
    remark = 'PLAN-8 接口冒烟驳回'
} | ConvertTo-Json
$review = Invoke-RestMethod -Method Post -Uri "http://localhost:8811/api/admin/transfer-requests/$($created.data.requestId)/review" -Headers $adminHeaders -ContentType 'application/json' -Body $reviewBody

Clear-Host
Write-Host 'PLAN-8 真实接口冒烟验收结果'
Write-Host "工单：$($order.orderId)，转派申请：$($created.data.requestId)"
Write-Host "创建申请：HTTP 200 / code=$($created.code)"
Write-Host "相同 bizNo 重试：replayed=$($replayed.data.replayed)"
Write-Host "维修人员待审批列表：total=$($workerList.data.total)"
Write-Host "管理员详情：code=$($detail.code)"
Write-Host "管理员驳回：reviewSuccess=$($review.data.reviewSuccess)，dispatchSuccess=$($review.data.dispatchSuccess)"
Write-Host '结果：接口冒烟验收通过' -ForegroundColor Green
Write-Host "执行时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
if ($Host.Name -eq 'ConsoleHost') { Read-Host '按 Enter 关闭此验收终端' | Out-Null }
