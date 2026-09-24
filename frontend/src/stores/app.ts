import { defineStore } from 'pinia'
import type { LoginUserInfo } from '../api/auth'
import { clearToken, setToken } from '../api/tokenStorage'
import { isRoleType } from '../constants/role'

const USER_KEY = 'dorm-repair-user'

/**
 * 页面刷新后从本地缓存恢复用户信息，仅用于界面展示和路由守卫，
 * 不替代服务端鉴权；接口 401 时仍会统一清理登录状态。
 */
function readStoredUser(): LoginUserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<LoginUserInfo> | null
    if (
      typeof parsed?.userId === 'number'
      && typeof parsed?.username === 'string'
      && typeof parsed?.realName === 'string'
      && isRoleType(parsed?.roleType)
    ) {
      return parsed as LoginUserInfo
    }
    localStorage.removeItem(USER_KEY)
    return null
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAppStore = defineStore('app', {
  state: () => ({
    ready: true,
    currentUser: readStoredUser(),
  }),
  getters: {
    isLoggedIn: (state) => state.currentUser !== null,
    roleType: (state) => state.currentUser?.roleType ?? null,
  },
  actions: {
    setLoginState(token: string, user: LoginUserInfo) {
      setToken(token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      this.currentUser = user
    },
    clearLoginState() {
      clearToken()
      localStorage.removeItem(USER_KEY)
      this.currentUser = null
    },
  },
})
