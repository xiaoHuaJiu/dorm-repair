# PLAN-6 维修人员主流程设计

## 目标

让当前负责人把已派单工单从待接单推进至待确认，并完整记录维修过程、材料、状态流转和操作日志。

## 接口

- `POST /api/worker/repair-orders/{id}/accept`
- `POST /api/worker/repair-orders/{id}/process-records`
- `POST /api/worker/repair-orders/{id}/materials`
- `POST /api/worker/repair-orders/{id}/interrupt`
- `POST /api/worker/repair-orders/{id}/resume`
- `POST /api/worker/repair-orders/{id}/submit-result`

所有接口仅允许维修人员角色，操作人由 `UserContext` 解析为 `repair_worker.id`，请求不得指定 workerId。

## 状态和并发

- 接单：待接单→维修中，且当前时间不得超过接单截止时间。
- 普通过程/材料：仅维修中、返工中可新增，不改变状态。
- 中断：仅维修中→已中断。
- 恢复：仅已中断→维修中。
- 提交结果：维修中或返工中→待确认，写 repair_submit_time，不写 complete_time。
- 所有状态变化使用包含 orderId、当前负责人和原状态的 CAS；CAS 失败后重读工单，负责人变化按 403，状态变化按 409。

## 数据与事务

- 接单：order + flow + operation log。
- 普通过程：process + operation log。
- 材料：material + operation log。
- 中断/恢复/提交：order + process + flow + operation log。
- 中断原因沿用 DDL：1等待材料、2第三方介入、3现场条件不允许、4其他。
- 过程图片只保存已有对象引用，本阶段不实现上传接口。
- 学生详情不返回材料明细，管理员和当前负责人可查看。

## 错误约定

- 工单不存在：HTTP 404 / 17001。
- 非当前负责人：HTTP 403 / 17002。
- 状态、截止时间或 CAS 冲突：HTTP 409 / 10005。
- 参数校验：HTTP 400 / 10001。

## 验证

覆盖合法全链路、所有非法状态、越权、接单超时、预计时间、CAS 并发、事务回滚、角色授权、学生材料隐藏、真实 MySQL 和 HTTP 冒烟测试。
