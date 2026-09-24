import { request } from './http'

export interface LoginRequest { username: string; password: string }
export interface LoginUserInfo { userId: number; username: string; realName: string; roleType: number }
export interface LoginResponse { token: string; user: LoginUserInfo }

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>({ method: 'POST', url: '/auth/login', data })
}
