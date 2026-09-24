/**
 * 工单状态枚举。数值与后端 `RepairOrderStatusEnum` 一致。
 */
export const ORDER_STATUS = {
  PENDING_DISPATCH: 0,
  PENDING_ACCEPTANCE: 1,
  REPAIRING: 2,
  PENDING_CONFIRMATION: 3,
  REWORKING: 4,
  INTERRUPTED: 5,
  COMPLETED: 6,
  CANCELLED: 7,
} as const

export type OrderStatus = (typeof ORDER_STATUS)[keyof typeof ORDER_STATUS]

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  [ORDER_STATUS.PENDING_DISPATCH]: '待派单',
  [ORDER_STATUS.PENDING_ACCEPTANCE]: '待接单',
  [ORDER_STATUS.REPAIRING]: '维修中',
  [ORDER_STATUS.PENDING_CONFIRMATION]: '待确认',
  [ORDER_STATUS.REWORKING]: '返工中',
  [ORDER_STATUS.INTERRUPTED]: '已中断',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消',
}

export type StatusTone = 'red' | 'amber' | 'green' | 'gray'

/** 状态标签色调：已完成绿色、已取消灰色、待接单/待确认琥珀色、其余红色。 */
export const ORDER_STATUS_TONE: Record<OrderStatus, StatusTone> = {
  [ORDER_STATUS.PENDING_DISPATCH]: 'red',
  [ORDER_STATUS.PENDING_ACCEPTANCE]: 'amber',
  [ORDER_STATUS.REPAIRING]: 'red',
  [ORDER_STATUS.PENDING_CONFIRMATION]: 'amber',
  [ORDER_STATUS.REWORKING]: 'red',
  [ORDER_STATUS.INTERRUPTED]: 'red',
  [ORDER_STATUS.COMPLETED]: 'green',
  [ORDER_STATUS.CANCELLED]: 'gray',
}

export function isOrderStatus(value: unknown): value is OrderStatus {
  return typeof value === 'number' && value in ORDER_STATUS_LABEL
}

export function orderStatusLabel(status: number, fallback = '未知状态'): string {
  return isOrderStatus(status) ? ORDER_STATUS_LABEL[status] : fallback
}

export function orderStatusTone(status: number): StatusTone {
  return isOrderStatus(status) ? ORDER_STATUS_TONE[status] : 'gray'
}

/**
 * 工单流转操作类型。数值与后端 `RepairOrderOperationTypeEnum` 一致，
 * 后续阶段随后端扩展补充转派、取消等节点。
 */
export const ORDER_OPERATION = {
  CREATE: 1,
  AUTO_DISPATCH: 2,
  ACCEPT: 3,
  INTERRUPT: 4,
  RESUME: 5,
  SUBMIT_RESULT: 6,
  STUDENT_CONFIRM: 7,
  REWORK: 8,
} as const

export const ORDER_OPERATION_LABEL: Record<number, string> = {
  [ORDER_OPERATION.CREATE]: '提交报修',
  [ORDER_OPERATION.AUTO_DISPATCH]: '系统派单',
  [ORDER_OPERATION.ACCEPT]: '接单',
  [ORDER_OPERATION.INTERRUPT]: '中断维修',
  [ORDER_OPERATION.RESUME]: '恢复维修',
  [ORDER_OPERATION.SUBMIT_RESULT]: '提交维修结果',
  [ORDER_OPERATION.STUDENT_CONFIRM]: '确认维修完成',
  [ORDER_OPERATION.REWORK]: '申请返工',
}

export function orderOperationLabel(operationType: number): string {
  return ORDER_OPERATION_LABEL[operationType] ?? '状态更新'
}

/**
 * 学生可取消的状态集合（与 F6-03 需求及原型口径一致）：
 * 待派单、待接单、维修中、已中断、返工中。
 */
export const STUDENT_CANCELABLE_STATUSES: readonly OrderStatus[] = [
  ORDER_STATUS.PENDING_DISPATCH,
  ORDER_STATUS.PENDING_ACCEPTANCE,
  ORDER_STATUS.REPAIRING,
  ORDER_STATUS.INTERRUPTED,
  ORDER_STATUS.REWORKING,
]

export function isStudentCancelable(status: number): boolean {
  return STUDENT_CANCELABLE_STATUSES.includes(status as OrderStatus)
}

/** 维修过程记录类型。数值与后端 `ProcessRecordTypeEnum` 一致。 */
export const PROCESS_RECORD_TYPE = {
  NORMAL: 1,
  INTERRUPT: 2,
  RESUME: 3,
  SUBMIT_RESULT: 4,
} as const

/** 中断原因枚举，与《前端联调接口说明》10.5 一致。 */
export const INTERRUPT_REASON: Record<number, string> = {
  1: '等待材料',
  2: '第三方介入',
  3: '现场条件限制',
  4: '其他',
}

export function interruptReasonLabel(type: number): string {
  return INTERRUPT_REASON[type] ?? '其他'
}

/** 维修端“我的工单”筛选状态集合（F7-03，与原型一致）。 */
export const WORKER_FILTER_STATUSES: readonly { value: OrderStatus; label: string }[] = [
  { value: ORDER_STATUS.REPAIRING, label: '维修中' },
  { value: ORDER_STATUS.PENDING_CONFIRMATION, label: '待确认' },
  { value: ORDER_STATUS.REWORKING, label: '返工中' },
  { value: ORDER_STATUS.INTERRUPTED, label: '已中断' },
  { value: ORDER_STATUS.COMPLETED, label: '已完成' },
  { value: ORDER_STATUS.CANCELLED, label: '已取消' },
]

export const PROCESS_RECORD_TYPE_LABEL: Record<number, string> = {
  [PROCESS_RECORD_TYPE.NORMAL]: '维修过程',
  [PROCESS_RECORD_TYPE.INTERRUPT]: '维修中断',
  [PROCESS_RECORD_TYPE.RESUME]: '恢复维修',
  [PROCESS_RECORD_TYPE.SUBMIT_RESULT]: '提交维修结果',
}

export function processRecordTypeLabel(recordType: number): string {
  return PROCESS_RECORD_TYPE_LABEL[recordType] ?? '过程记录'
}
