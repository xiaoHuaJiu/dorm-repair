import { request } from './http'
import type { PageResult } from './types'
import type {
  AddMaterialUsageRequest,
  AddRepairProcessRequest,
  AcceptRepairOrderRequest,
  CreateRepairOrderRequest,
  CreateRepairOrderResponse,
  DuplicateCheckRequest,
  DuplicateCheckResponse,
  InterruptRepairRequest,
  OrderDetail,
  OrderListItem,
  OrderQuery,
  ResumeRepairRequest,
  SubmitRepairResultRequest,
} from '@/types/order'

/**
 * 三端工单接口。路径与后端 StudentRepairOrderController、
 * AdminRepairOrderController、WorkerRepairOrderController 一致。
 * 学生与维修人员分页查询由后端强制限定为当前用户本人数据。
 */

/** 学生分页查询本人工单。 */
export function pageStudentOrders(query: OrderQuery): Promise<PageResult<OrderListItem>> {
  return request<PageResult<OrderListItem>>({ method: 'GET', url: '/student/repair-orders', params: query })
}

/** 维修人员分页查询本人负责工单。 */
export function pageWorkerOrders(query: OrderQuery): Promise<PageResult<OrderListItem>> {
  return request<PageResult<OrderListItem>>({ method: 'GET', url: '/worker/repair-orders', params: query })
}

/** 管理员分页查询学校全部工单。 */
export function pageAdminOrders(query: OrderQuery): Promise<PageResult<OrderListItem>> {
  return request<PageResult<OrderListItem>>({ method: 'GET', url: '/admin/repair-orders', params: query })
}

/** 学生查询工单详情。 */
export function studentOrderDetail(id: number): Promise<OrderDetail> {
  return request<OrderDetail>({ method: 'GET', url: `/student/repair-orders/${id}` })
}

/** 维修人员查询工单详情。 */
export function workerOrderDetail(id: number): Promise<OrderDetail> {
  return request<OrderDetail>({ method: 'GET', url: `/worker/repair-orders/${id}` })
}

/** 管理员查询工单详情。 */
export function adminOrderDetail(id: number): Promise<OrderDetail> {
  return request<OrderDetail>({ method: 'GET', url: `/admin/repair-orders/${id}` })
}

/** 学生报修前疑似重复检测。 */
export function checkRepairDuplicate(data: DuplicateCheckRequest): Promise<DuplicateCheckResponse> {
  return request<DuplicateCheckResponse>({ method: 'POST', url: '/student/repair-orders/check-duplicate', data })
}

/** 学生创建报修。以 bizNo 幂等，同键请求完成前禁止重复提交。 */
export function createRepairOrder(data: CreateRepairOrderRequest): Promise<CreateRepairOrderResponse> {
  return request<CreateRepairOrderResponse>({
    method: 'POST',
    url: '/student/repair-orders',
    data,
    dedupeKey: `create-repair-order:${data.bizNo}`,
  })
}

/** 维修人员接单。 */
export function acceptRepairOrder(id: number, data?: AcceptRepairOrderRequest): Promise<null> {
  return request<null>({ method: 'POST', url: `/worker/repair-orders/${id}/accept`, data })
}

/** 维修人员新增维修过程记录。 */
export function addRepairProcess(id: number, data: AddRepairProcessRequest): Promise<null> {
  return request<null>({ method: 'POST', url: `/worker/repair-orders/${id}/process-records`, data })
}

/** 维修人员登记材料使用。 */
export function addMaterialUsage(id: number, data: AddMaterialUsageRequest): Promise<null> {
  return request<null>({ method: 'POST', url: `/worker/repair-orders/${id}/materials`, data })
}

/** 维修人员中断维修。 */
export function interruptRepair(id: number, data: InterruptRepairRequest): Promise<null> {
  return request<null>({ method: 'POST', url: `/worker/repair-orders/${id}/interrupt`, data })
}

/** 维修人员恢复维修。 */
export function resumeRepair(id: number, data?: ResumeRepairRequest): Promise<null> {
  return request<null>({ method: 'POST', url: `/worker/repair-orders/${id}/resume`, data })
}

/** 维修人员提交维修结果。 */
export function submitRepairResult(id: number, data: SubmitRepairResultRequest): Promise<null> {
  return request<null>({ method: 'POST', url: `/worker/repair-orders/${id}/submit-result`, data })
}
