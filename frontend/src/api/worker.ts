import { request } from './http'
import type { PageResult } from './types'
import type {
  AreaScopeItem,
  WorkerAreaScope,
  WorkerCreateRequest,
  WorkerDetail,
  WorkerFaultType,
  WorkerUpdateRequest,
} from '@/types/config'

export interface WorkerQuery {
  pageNum?: number
  pageSize?: number
  workerNo?: string
  realName?: string
  phone?: string
  workStatus?: number
  faultTypeId?: number
}

/** 管理员分页查询维修人员。 */
export function pageWorkers(query: WorkerQuery = {}): Promise<PageResult<WorkerDetail>> {
  return request<PageResult<WorkerDetail>>({ method: 'GET', url: '/admin/workers', params: query })
}

/** 管理员维修人员详情。 */
export function workerDetail(id: number): Promise<WorkerDetail> {
  return request<WorkerDetail>({ method: 'GET', url: `/admin/workers/${id}` })
}

/** 管理员创建维修人员（绑定已有账号或同事务新建账号）。 */
export function createWorker(data: WorkerCreateRequest): Promise<{ id: number }> {
  return request<{ id: number }>({ method: 'POST', url: '/admin/workers', data })
}

/** 管理员修改维修人员编号和备注。 */
export function updateWorker(id: number, data: WorkerUpdateRequest): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/workers/${id}`, data })
}

/** 管理员更新维修业务状态：0 正常 / 1 请假中 / 2 停用。 */
export function updateWorkStatus(id: number, status: number): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/workers/${id}/work-status`, params: { status } })
}

/** 管理员更新登录账号状态：1 启用 / 0 停用。 */
export function updateAccountStatus(id: number, status: number): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/workers/${id}/account-status`, params: { status } })
}

/** 查询维修人员全部技能关系。 */
export function listWorkerFaultTypes(id: number): Promise<WorkerFaultType[]> {
  return request<WorkerFaultType[]>({ method: 'GET', url: `/admin/workers/${id}/fault-types` })
}

/** 批量保存维修人员技能。 */
export function saveWorkerFaultTypes(id: number, faultTypeIds: number[]): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/workers/${id}/fault-types`, data: { faultTypeIds } })
}

/** 查询维修人员全部负责范围。 */
export function listWorkerAreaScopes(id: number): Promise<WorkerAreaScope[]> {
  return request<WorkerAreaScope[]>({ method: 'GET', url: `/admin/workers/${id}/area-scopes` })
}

/** 批量保存维修人员负责范围。 */
export function saveWorkerAreaScopes(id: number, scopes: AreaScopeItem[]): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/workers/${id}/area-scopes`, data: { scopes } })
}
