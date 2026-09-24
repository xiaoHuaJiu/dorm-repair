import { defineStore } from 'pinia'
import type { LoginUserInfo } from '../api/auth'
import { clearToken, setToken } from '../api/tokenStorage'

export const useAppStore = defineStore('app', {
  state: () => ({
    ready: true,
    currentUser: null as LoginUserInfo | null,
  }),
  actions: {
    setLoginState(token: string, user: LoginUserInfo) {
      setToken(token)
      this.currentUser = user
    },
    clearLoginState() {
      clearToken()
      this.currentUser = null
    },
  },
})
