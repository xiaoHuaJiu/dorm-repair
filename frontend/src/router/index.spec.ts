import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from './index'
import { useAppStore } from '@/stores/app'

function buildRouter() {
  setActivePinia(createPinia())
  return createAppRouter(createMemoryHistory())
}

const STUDENT = { userId: 1, username: 'student1', realName: '林同学', roleType: 1 }
const WORKER = { userId: 2, username: 'worker1', realName: '张师傅', roleType: 2 }
const ADMIN = { userId: 3, username: 'admin1', realName: '王老师', roleType: 3 }

describe('路由守卫', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('未登录访问业务页跳转登录页并携带 redirect', async () => {
    const router = buildRouter()
    await router.push('/student/home')
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/student/home')
  })

  it('未登录可以访问登录页、403 和 404', async () => {
    const router = buildRouter()
    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('login')

    await router.push('/403')
    expect(router.currentRoute.value.name).toBe('forbidden')

    await router.push('/no-such-page')
    expect(router.currentRoute.value.name).toBe('not-found')
  })

  it('已登录访问登录页跳回角色首页', async () => {
    const router = buildRouter()
    useAppStore().setLoginState('t', STUDENT)

    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/student/home')
  })

  it('学生只能访问学生端', async () => {
    const router = buildRouter()
    useAppStore().setLoginState('t', STUDENT)

    await router.push('/student/home')
    expect(router.currentRoute.value.name).toBe('student-home')

    await router.push('/worker/home')
    expect(router.currentRoute.value.name).toBe('forbidden')

    await router.push('/admin/dashboard')
    expect(router.currentRoute.value.name).toBe('forbidden')
  })

  it('维修人员只能访问维修端', async () => {
    const router = buildRouter()
    useAppStore().setLoginState('t', WORKER)

    await router.push('/worker/home')
    expect(router.currentRoute.value.name).toBe('worker-home')

    await router.push('/student/home')
    expect(router.currentRoute.value.name).toBe('forbidden')
  })

  it('管理员可以访问管理端首页', async () => {
    const router = buildRouter()
    useAppStore().setLoginState('t', ADMIN)

    await router.push('/admin/dashboard')
    expect(router.currentRoute.value.name).toBe('admin-dashboard')
  })

  it('各端根路径重定向到各自首页', async () => {
    const router = buildRouter()
    useAppStore().setLoginState('t', WORKER)

    await router.push('/worker')
    expect(router.currentRoute.value.path).toBe('/worker/home')
  })
})
