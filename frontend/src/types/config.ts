/**
 * 基础配置类型。字段与后端 `AreaTreeNode`、`RepairArea`、`FaultTypeResponse`、
 * `RepairWorkSchedule`、`WorkerDetailResponse` 契约一致。
 */

/** 位置类型：1 单位/校区、2 区域、3 楼栋、4 房间。 */
export type AreaType = 1 | 2 | 3 | 4

/** 区域树节点。 */
export interface AreaTreeNode {
  id: number
  parentId: number | null
  areaCode: string
  areaName: string
  areaType: number
  status: number
  sortNo: number
  children: AreaTreeNode[]
}

/** 区域详情（含备注）。 */
export interface AreaDetail {
  id: number
  parentId: number | null
  areaCode: string
  areaName: string
  areaType: number
  sortNo: number
  status: number
  remark: string | null
}

/** 区域创建请求。 */
export interface AreaCreateRequest {
  parentId: number
  areaCode: string
  areaName: string
  areaType: number
  sortNo: number
  remark?: string
}

/** 区域修改请求。 */
export interface AreaUpdateRequest {
  areaName: string
  sortNo: number
  remark?: string
}

/** 故障类型。 */
export interface FaultTypeItem {
  id: number
  typeCode: string
  typeName: string
  status: number
  sortNo: number
  remark: string | null
}

export interface FaultTypeCreateRequest {
  typeCode: string
  typeName: string
  sortNo: number
  remark?: string
}

export interface FaultTypeUpdateRequest {
  typeName: string
  sortNo: number
  remark?: string
}

/** 工作时间方案。 */
export interface WorkScheduleItem {
  id: number
  scheduleName: string
  startDate: string
  endDate: string
  workStartTime: string
  workEndTime: string
  status: number
  remark: string | null
  createBy: number | null
  createTime: string | null
  updateTime: string | null
}

export interface WorkScheduleCreateRequest {
  scheduleName: string
  startDate: string
  endDate: string
  workStartTime: string
  workEndTime: string
  status: number
  remark?: string
}

/** 维修人员列表与详情。 */
export interface WorkerDetail {
  workerId: number
  userId: number | null
  username: string | null
  realName: string | null
  phone: string | null
  workerNo: string
  userStatus: number
  workStatus: number
  remark: string | null
}

export interface WorkerCreateRequest {
  /** 绑定已有账号时传用户 ID；否则提供 username/password/realName 新建账号。 */
  existingUserId?: number
  username?: string
  password?: string
  realName?: string
  phone?: string
  workerNo: string
  remark?: string
}

export interface WorkerUpdateRequest {
  workerNo: string
  remark?: string
}

/** 维修人员负责区域范围。 */
export interface WorkerAreaScope {
  id: number
  workerId: number
  campusId: number
  areaId: number | null
  buildingId: number | null
  status: number
}

export interface AreaScopeItem {
  campusId: number
  areaId?: number | null
  buildingId?: number | null
}

/** 维修人员技能关系。 */
export interface WorkerFaultType {
  id: number
  workerId: number
  faultTypeId: number
  status: number
}
