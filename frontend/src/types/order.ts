/**
 * 工单公共类型。字段与后端 `RepairOrderListItem`、`RepairOrderDetailResponse`
 * 及各类操作请求/响应契约一致。
 */
import type { OrderFile } from './file'

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

/** 工单详情基础信息。字段与后端 `RepairOrderBaseInfoResponse` 一致。 */
export interface OrderBaseInfo {
  id: number
  orderNo: string
  studentUid: number
  contactName: string
  contactPhone: string
  campusId: number
  areaId: number
  buildingId: number
  roomId: number
  locationDetail: string | null
  faultTypeId: number
  problemDescription: string
  /** 旧字段，仅兼容保留；图片统一走 files。 */
  imageUrls: string | null
  /** 报修图片列表（后端 `FileResponse`）。 */
  files: OrderFile[]
  status: number
  currentAssigneeId: number | null
  dispatchTime: string | null
  acceptDeadline: string | null
  acceptTime: string | null
  expectedCompleteTime: string | null
  completeDeadline: string | null
  repairSubmitTime: string | null
  confirmTime: string | null
  completeTime: string | null
  reworkCount: number | null
  exceptionFlag: number | null
  duplicateFlag: number | null
  duplicateOrderId: number | null
  reportTime: string
  cancelTime: string | null
  cancelReason: string | null
}

/** 维修过程记录。字段与后端 `RepairProcessRecord` 一致。 */
export interface OrderProcessRecord {
  id: number
  orderId: number
  workerId: number
  /** 1 普通过程、2 中断、3 恢复、4 提交维修结果。 */
  recordType: number
  content: string
  /** 过程图片列表（后端 `FileResponse`）。 */
  files: OrderFile[]
  interruptReasonType: number | null
  recordTime: string
  createTime: string
}

/** 材料使用记录。字段与后端 `RepairMaterialUsage` 一致。 */
export interface OrderMaterialUsage {
  id: number
  orderId: number
  workerId: number
  materialName: string
  specification: string | null
  quantity: number
  unit: string | null
  remark: string | null
  useTime: string | null
  createTime: string
}

/** 返工记录。字段与后端 `RepairReworkRecord` 一致。 */
export interface OrderReworkRecord {
  id: number
  orderId: number
  reworkNo: number
  applicantUid: number
  reason: string
  /** 返工图片列表（后端 `FileResponse`）。 */
  files: OrderFile[]
  originalAssigneeId: number | null
  status: number
  adminIntervention: number
  createTime: string
  finishTime: string | null
}

/** 服务评价。字段与后端 `RepairEvaluation` 一致。 */
export interface OrderEvaluation {
  id: number
  orderId: number
  studentUid: number
  workerId: number
  score: number
  content: string | null
  createTime: string
  updateTime: string
}

/** 聚合工单详情。结构与后端 `RepairOrderDetailResponse` 一致。 */
export interface OrderDetail {
  baseInfo: OrderBaseInfo
  processRecords: OrderProcessRecord[]
  materialRecords: OrderMaterialUsage[]
  reworkRecords: OrderReworkRecord[]
  evaluation: OrderEvaluation | null
  flows: OrderFlowItem[]
}

/** 疑似重复工单。字段与后端 `SuspectedRepairOrder` 一致。 */
export interface SuspectedOrder {
  orderId: number
  orderNo: string
  faultTypeName: string
  locationText: string
  reportTime: string
  status: number
  statusName: string
}

/** 疑似重复检测请求。字段与后端 `RepairOrderDuplicateCheckRequest` 一致。 */
export interface DuplicateCheckRequest {
  campusId: number
  areaId: number
  buildingId: number
  roomId: number
  faultTypeId: number
}

/** 疑似重复检测结果。 */
export interface DuplicateCheckResponse {
  duplicate: boolean
  suspectedOrders: SuspectedOrder[]
}

/** 创建报修请求。字段与后端 `CreateRepairOrderRequest` 一致；bizNo 由前端生成保证幂等。 */
export interface CreateRepairOrderRequest extends DuplicateCheckRequest {
  bizNo: string
  locationDetail?: string
  problemDescription: string
  contactName: string
  contactPhone: string
  /** 已上传完成的附件引用，最多 9 个。 */
  fileIds?: number[]
  /** 疑似重复时用户是否仍然提交。 */
  confirmDuplicate: boolean
}

/** 创建报修结果。字段与后端 `CreateRepairOrderResponse` 一致；created=false 且 orderId=null 表示服务端拦截（需确认仍然提交）。 */
export interface CreateRepairOrderResponse {
  created: boolean
  duplicate: boolean
  orderId: number | null
  orderNo: string
  suspectedOrders: SuspectedOrder[]
}

/** 维修人员接单请求。字段与后端 `AcceptRepairOrderRequest` 一致。 */
export interface AcceptRepairOrderRequest {
  /** 预计完成时间；不传时后端按接单后 24 小时计算。 */
  expectedCompleteTime?: string
}

/** 维修过程记录请求。字段与后端 `AddRepairProcessRequest` 一致。 */
export interface AddRepairProcessRequest {
  content: string
  /** 已上传完成的附件引用，最多 9 个。 */
  fileIds?: number[]
}

/** 材料使用登记请求。字段与后端 `AddMaterialUsageRequest` 一致。 */
export interface AddMaterialUsageRequest {
  materialName: string
  specification?: string
  quantity: number
  unit: string
  remark?: string
}

/** 中断维修请求。字段与后端 `InterruptRepairRequest` 一致。 */
export interface InterruptRepairRequest {
  /** 1 等待材料、2 第三方介入、3 现场条件限制、4 其他。 */
  interruptReasonType: number
  content: string
}

/** 恢复维修请求。字段与后端 `ResumeRepairRequest` 一致，备注可不传。 */
export interface ResumeRepairRequest {
  remark?: string
}

/** 提交维修结果请求。字段与后端 `SubmitRepairResultRequest` 一致。 */
export interface SubmitRepairResultRequest {
  resultDescription: string
  /** 已上传完成的附件引用，最多 9 个。 */
  fileIds?: number[]
}

/** 学生提交服务评价请求。字段与后端 `CreateRepairEvaluationRequest` 一致。 */
export interface CreateRepairEvaluationRequest {
  /** 1-5 星。 */
  score: number
  content?: string
}

/** 学生申请返工请求。字段与后端 `CreateReworkRequest` 一致。 */
export interface CreateReworkRequest {
  reason: string
  /** 已上传完成的附件引用，最多 9 个。 */
  fileIds?: number[]
}

/** 三端工单分页查询参数。字段与后端 `RepairOrderQueryRequest` 一致。 */
export interface OrderQuery {
  pageNum: number
  pageSize: number
  orderNo?: string
  statusList?: number[]
  faultTypeId?: number
  campusId?: number
  areaId?: number
  buildingId?: number
  roomId?: number
  /** 负责人筛选（管理员端使用）。 */
  workerId?: number
  reportStartTime?: string
  reportEndTime?: string
  exceptionFlag?: boolean
  suspectedDuplicate?: boolean
  acceptTimeout?: boolean
  completeTimeout?: boolean
}
