import { request } from './http'
import type { AreaCreateRequest, AreaDetail, AreaTreeNode, AreaUpdateRequest } from '@/types/config'

/** 管理员区域树（含停用节点）。 */
export function adminAreaTree(): Promise<AreaTreeNode[]> {
  return request<AreaTreeNode[]>({ method: 'GET', url: '/admin/areas/tree' })
}

/** 管理员区域详情。 */
export function areaDetail(id: number): Promise<AreaDetail> {
  return request<AreaDetail>({ method: 'GET', url: `/admin/areas/${id}` })
}

/** 管理员新增区域节点，返回新节点 ID。 */
export function createArea(data: AreaCreateRequest): Promise<{ id: number }> {
  return request<{ id: number }>({ method: 'POST', url: '/admin/areas', data })
}

/** 管理员修改区域节点（名称、排序、备注）。 */
export function updateArea(id: number, data: AreaUpdateRequest): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/areas/${id}`, data })
}

/** 管理员启停区域节点：1 启用 / 0 停用。 */
export function updateAreaStatus(id: number, status: number): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/areas/${id}/status`, params: { status } })
}

/** 任意已登录用户查询有效区域树。 */
export function enabledAreaTree(): Promise<AreaTreeNode[]> {
  return request<AreaTreeNode[]>({ method: 'GET', url: '/areas/tree' })
}

/** 任意已登录用户查询有效直接子节点；根节点使用 parentId=0。 */
export function areaChildren(parentId: number): Promise<AreaDetail[]> {
  return request<AreaDetail[]>({ method: 'GET', url: `/areas/${parentId}/children` })
}
