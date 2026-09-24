import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppStore } from './app'

const USER = { userId: 7, username: 'student1', realName: '林同学', roleType: 1 }

describe('应用登录状态', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('初始状态未登录', () => {
    const app = useAppStore()
    expect(app.currentUser).toBeNull()
    expect(app.isLoggedIn).toBe(false)
    expect(app.roleType).toBeNull()
  })

  it('登录保存令牌和用户，登出全部清理', () => {
    const app = useAppStore()
    app.setLoginState('jwt-token', USER)

    expect(localStorage.getItem('dorm-repair-token')).toBe('jwt-token')
    expect(localStorage.getItem('dorm-repair-user')).toBe(JSON.stringify(USER))
    expect(app.currentUser).toEqual(USER)
    expect(app.isLoggedIn).toBe(true)

    app.clearLoginState()
    expect(localStorage.getItem('dorm-repair-token')).toBeNull()
    expect(localStorage.getItem('dorm-repair-user')).toBeNull()
    expect(app.currentUser).toBeNull()
  })

  it('刷新页面后从本地缓存恢复用户信息', () => {
    localStorage.setItem('dorm-repair-token', 'jwt-token')
    localStorage.setItem('dorm-repair-user', JSON.stringify(USER))

    // 重新创建 Pinia 模拟页面刷新。
    setActivePinia(createPinia())
    const app = useAppStore()
    expect(app.currentUser).toEqual(USER)
    expect(app.isLoggedIn).toBe(true)
  })

  it('本地缓存损坏时忽略并清理，不抛出异常', () => {
    localStorage.setItem('dorm-repair-token', 'jwt-token')
    localStorage.setItem('dorm-repair-user', '{broken json')

    setActivePinia(createPinia())
    const app = useAppStore()
    expect(app.currentUser).toBeNull()
    expect(localStorage.getItem('dorm-repair-user')).toBeNull()
  })

  it('本地缓存缺少必要字段时按未登录处理', () => {
    localStorage.setItem('dorm-repair-user', JSON.stringify({ username: 'student1' }))

    setActivePinia(createPinia())
    const app = useAppStore()
    expect(app.currentUser).toBeNull()
    expect(localStorage.getItem('dorm-repair-user')).toBeNull()
  })
})
