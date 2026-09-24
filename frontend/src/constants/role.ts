/**
 * 角色枚举与路由落点。
 * 数值与后端 `UserRoleEnum` 保持一致：1 学生、2 维修人员、3 管理员。
 */
export const ROLE = {
  STUDENT: 1,
  WORKER: 2,
  ADMIN: 3,
} as const

export type RoleType = (typeof ROLE)[keyof typeof ROLE]

export const ROLE_LABEL: Record<RoleType, string> = {
  [ROLE.STUDENT]: '学生',
  [ROLE.WORKER]: '维修人员',
  [ROLE.ADMIN]: '管理员',
}

/** 登录后各角色的默认首页。 */
export const ROLE_HOME: Record<RoleType, string> = {
  [ROLE.STUDENT]: '/student/home',
  [ROLE.WORKER]: '/worker/home',
  [ROLE.ADMIN]: '/admin/dashboard',
}

export function isRoleType(value: unknown): value is RoleType {
  return value === ROLE.STUDENT || value === ROLE.WORKER || value === ROLE.ADMIN
}

/** 根据后端返回的数值取角色首页，未知角色返回 undefined。 */
export function roleHomeOf(roleType: number): string | undefined {
  return isRoleType(roleType) ? ROLE_HOME[roleType] : undefined
}

/** 根据后端返回的数值取角色名称，未知角色返回固定文案。 */
export function roleLabelOf(roleType: number | null | undefined): string {
  return isRoleType(roleType) ? ROLE_LABEL[roleType] : '未知角色'
}
