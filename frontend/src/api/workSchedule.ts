import { request } from './http'
import type { PageResult } from './types'
import type { WorkScheduleCreateRequest, WorkScheduleItem } from '@/types/config'

export interface WorkScheduleQuery {
  pageNum?: number
  pageSize?: number
  scheduleName?: string
  status?: number
  date?: string
}

/** 管理员分页查询工作时间方案。 */
export function pageWorkSchedules(query: WorkScheduleQuery = {}): Promise<PageResult<WorkScheduleItem>> {
  return request<PageResult<WorkScheduleItem>>({ method: 'GET', url: '/admin/work-schedules', params: query })
}

/** 管理员工作时间方案详情。 */
export function workScheduleDetail(id: number): Promise<WorkScheduleItem> {
  return request<WorkScheduleItem>({ method: 'GET', url: `/admin/work-schedules/${id}` })
}

/** 管理员新增工作时间方案。 */
export function createWorkSchedule(data: WorkScheduleCreateRequest): Promise<{ id: number }> {
  return request<{ id: number }>({ method: 'POST', url: '/admin/work-schedules', data })
}

/** 管理员修改工作时间方案。 */
export function updateWorkSchedule(id: number, data: WorkScheduleCreateRequest): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/work-schedules/${id}`, data })
}

/** 管理员启停工作时间方案：1 启用 / 0 停用。 */
export function updateWorkScheduleStatus(id: number, status: number): Promise<null> {
  return request<null>({ method: 'PUT', url: `/admin/work-schedules/${id}/status`, params: { status } })
}
