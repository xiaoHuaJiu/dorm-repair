/**
 * 基础配置枚举与文案。数值与后端 `AreaTypeEnum`、`WorkerWorkStatusEnum`
 * 及通用启停约定（1 启用 / 0 停用）一致。
 */

export const AREA_TYPE = {
  CAMPUS: 1,
  AREA: 2,
  BUILDING: 3,
  ROOM: 4,
} as const

export type AreaTypeValue = (typeof AREA_TYPE)[keyof typeof AREA_TYPE]

export const AREA_TYPE_LABEL: Record<number, string> = {
  [AREA_TYPE.CAMPUS]: '单位/校区',
  [AREA_TYPE.AREA]: '区域',
  [AREA_TYPE.BUILDING]: '楼栋',
  [AREA_TYPE.ROOM]: '房间',
}

/** 节点类型对应的下级类型；房间没有下级。 */
export const AREA_CHILD_TYPE: Partial<Record<number, number>> = {
  [AREA_TYPE.CAMPUS]: AREA_TYPE.AREA,
  [AREA_TYPE.AREA]: AREA_TYPE.BUILDING,
  [AREA_TYPE.BUILDING]: AREA_TYPE.ROOM,
}

export function areaTypeLabel(areaType: number): string {
  return AREA_TYPE_LABEL[areaType] ?? '未知类型'
}

/** 通用启停状态：1 启用 / 0 停用。 */
export const COMMON_STATUS = {
  DISABLED: 0,
  ENABLED: 1,
} as const

export const COMMON_STATUS_LABEL: Record<number, string> = {
  [COMMON_STATUS.DISABLED]: '停用',
  [COMMON_STATUS.ENABLED]: '启用',
}

export function commonStatusLabel(status: number): string {
  return COMMON_STATUS_LABEL[status] ?? '未知'
}

/** 维修人员工作状态：0 正常 / 1 请假中 / 2 停用。 */
export const WORK_STATUS = {
  NORMAL: 0,
  ON_LEAVE: 1,
  DISABLED: 2,
} as const

export const WORK_STATUS_LABEL: Record<number, string> = {
  [WORK_STATUS.NORMAL]: '正常',
  [WORK_STATUS.ON_LEAVE]: '请假中',
  [WORK_STATUS.DISABLED]: '停用',
}

export function workStatusLabel(status: number): string {
  return WORK_STATUS_LABEL[status] ?? '未知'
}

export function workStatusTone(status: number): 'green' | 'amber' | 'red' {
  if (status === WORK_STATUS.NORMAL) return 'green'
  if (status === WORK_STATUS.ON_LEAVE) return 'amber'
  return 'red'
}
