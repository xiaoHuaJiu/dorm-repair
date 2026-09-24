import { request } from './http'
import type { PageResult } from './types'
import type { FaultTypeCreateRequest, FaultTypeItem, FaultTypeUpdateRequest } from '@/types/config'

export interface FaultTypeQuery {
  pageNum?: number
  pageSize?: number
  typeCode?: string
  typeName?: string
  status?: number
}

/** 管理员分页查询故障类型。 */
export function pageFaultTypes(query: FaultTypeQuery = {}): Promise<PageResult<FaultTypeItem>> {
  return request<PageResult<FaultTypeItem>>({ method: 'GET', url: '/admin/fault-types', params: query })
}

/** 管理员故障类型详情。 */
export function faultTypeDetail(id: number): Promise<FaultTypeItem> {
  return request<FaultTypeItem>({ method: 'GET', url: `/admin/fault-types/${id}` })
}

/** 管理员新增故障类型。 */
export function createFaultType(data: FaultTypeCreateRequest): Promise<{ id: number }> {
  return request<{ id: number }>({ method: 'POST', url: '/admin/fault-types', data })
}

/** 管理员修改故障类型（编码创建后不可改）。 */
export function updateFaultType(id: number, data: FaultTypeUpdateRequest): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/fault-types/${id}`, data })
}

/** 管理员启停故障类型：1 启用 / 0 停用。 */
export function updateFaultTypeStatus(id: number, status: number): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/fault-types/${id}/status`, params: { status } })
}

/** 任意已登录用户查询启用中的故障类型。 */
export function listEnabledFaultTypes(): Promise<FaultTypeItem[]> {
  return request<FaultTypeItem[]>({ method: 'GET', url: '/fault-types/enabled' })
}
