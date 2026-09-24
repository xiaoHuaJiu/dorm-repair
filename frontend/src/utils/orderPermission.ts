import { ROLE, type RoleType } from '@/constants/role'
import { ORDER_STATUS, STUDENT_CANCELABLE_STATUSES } from '@/constants/order'

/**
 * 工单操作权限计算结果（F5-04）。根据当前角色、工单状态与是否为
 * 负责人本地计算，用于页面展示操作入口；接口层仍以后端校验为准。
 */
export interface OrderActionPermissions {
  /** 学生取消工单。 */
  cancel: boolean
  /** 维修人员接单。 */
  accept: boolean
  /** 维修人员记录维修过程。 */
  addProcessRecord: boolean
  /** 维修人员中断维修。 */
  interrupt: boolean
  /** 维修人员恢复维修。 */
  resume: boolean
  /** 维修人员申请转派。 */
  transfer: boolean
  /** 维修人员提交维修结果。 */
  submitResult: boolean
  /** 学生确认维修完成。 */
  confirm: boolean
  /** 学生申请返工。 */
  requestRework: boolean
  /** 是否只读查看。 */
  readonly: boolean
}

const READONLY: OrderActionPermissions = {
  cancel: false,
  accept: false,
  addProcessRecord: false,
  interrupt: false,
  resume: false,
  transfer: false,
  submitResult: false,
  confirm: false,
  requestRework: false,
  readonly: true,
}

function of(permissions: Partial<OrderActionPermissions>): OrderActionPermissions {
  const anyAction = Object.entries(permissions).some(([key, value]) => key !== 'readonly' && value === true)
  return { ...READONLY, ...permissions, readonly: !anyAction }
}

function studentPermissions(status: number): OrderActionPermissions {
  return of({
    cancel: STUDENT_CANCELABLE_STATUSES.includes(status as (typeof STUDENT_CANCELABLE_STATUSES)[number]),
    confirm: status === ORDER_STATUS.PENDING_CONFIRMATION,
    requestRework: status === ORDER_STATUS.PENDING_CONFIRMATION,
  })
}

function workerPermissions(status: number, isAssignee: boolean): OrderActionPermissions {
  if (!isAssignee) return READONLY
  return of({
    accept: status === ORDER_STATUS.PENDING_ACCEPTANCE,
    addProcessRecord: status === ORDER_STATUS.REPAIRING,
    interrupt: status === ORDER_STATUS.REPAIRING,
    resume: status === ORDER_STATUS.INTERRUPTED,
    transfer: status === ORDER_STATUS.PENDING_ACCEPTANCE
      || status === ORDER_STATUS.REPAIRING
      || status === ORDER_STATUS.INTERRUPTED,
    submitResult: status === ORDER_STATUS.REPAIRING || status === ORDER_STATUS.REWORKING,
  })
}

/**
 * 计算工单操作权限。
 *
 * @param role 当前用户角色，与后端 UserRoleEnum 一致：1 学生、2 维修人员、3 管理员。
 * @param status 工单状态，与后端 RepairOrderStatusEnum 一致（0—7）。
 * @param isAssignee 当前维修人员是否为该工单负责人；其他角色忽略。
 */
export function computeOrderPermissions(
  role: RoleType | number,
  status: number,
  isAssignee = false,
): OrderActionPermissions {
  if (role === ROLE.STUDENT) return studentPermissions(status)
  if (role === ROLE.WORKER) return workerPermissions(status, isAssignee)
  // 管理员操作（审批、人工派单等）不属于工单内操作，一律只读，F8 阶段扩展。
  return READONLY
}
