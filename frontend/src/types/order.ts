/**
 * 工单公共类型。字段与后端 `RepairOrderListItem`、`RepairOrderFlow` 已确认契约一致；
 * 完整领域类型（详情聚合、操作权限等）在 F5 阶段统一定义。
 */

/** 三端工单列表记录。 */
export interface OrderListItem {
  orderId: number
  orderNo: string
  status: number
  statusName: string
  studentUid: number
  studentName: string
  faultTypeId: number
  faultTypeName: string
  campusId: number
  campusName: string
  areaId: number
  areaName: string
  buildingId: number
  buildingName: string
  roomId: number
  roomName: string
  locationText: string
  currentAssigneeId: number | null
  workerName: string | null
  reportTime: string
  acceptDeadline: string | null
  completeDeadline: string | null
  completeTime: string | null
  exceptionFlag: number | null
  duplicateFlag: number | null
  acceptTimeout: boolean | null
  completeTimeout: boolean | null
}

/** 工单生命周期流转记录。 */
export interface OrderFlowItem {
  id: number
  orderId: number
  operationType: number
  fromStatus: number | null
  toStatus: number | null
  originalAssigneeId: number | null
  newAssigneeId: number | null
  operatorId: number | null
  operatorRole: number | null
  sourceType: number | null
  relatedBusinessId: number | null
  reason: string | null
  operationTime: string
  createTime: string
}
