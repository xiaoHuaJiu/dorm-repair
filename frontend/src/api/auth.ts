import { request } from './http'

export interface LoginRequest { username: string; password: string }
export interface LoginUserInfo { userId: number; username: string; realName: string; roleType: number }
export interface LoginResponse { token: string; user: LoginUserInfo }

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>({ method: 'POST', url: '/auth/login', data })
}

/**
 * 学生注册。服务端固定创建 STUDENT 角色账号，前端不提交角色字段；
 * 注册成功不签发令牌，需要重新登录。
 */
export interface StudentRegisterRequest {
  username: string
  password: string
  confirmPassword: string
  realName: string
  phone: string
}

export function registerStudent(data: StudentRegisterRequest): Promise<null> {
  return request<null>({ method: 'POST', url: '/auth/register', data })
}
