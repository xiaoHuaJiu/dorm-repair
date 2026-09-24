import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import MobileAppShell from './MobileAppShell.vue'

const STUDENT = { userId: 1, username: 'student1', realName: '林同学', roleType: 1 }

const STUDENT_NAV = [
  { label: '首页', to: '/student/home' },
  { label: '我的工单', to: '/student/orders' },
]

async function buildShell(navItems = STUDENT_NAV) {
  setActivePinia(createPinia())
  useAppStore().setLoginState('jwt-token', STUDENT)
  const router = createAppRouter(createMemoryHistory())
  await router.push('/student/home')
  await router.isReady()
  const wrapper = mount(MobileAppShell, {
    props: { role: 1 as const, title: '首页', subtitle: '查看本人报修进度', navItems },
    global: { plugins: [router] },
    slots: { default: '<div class="page-body">页面内容</div>', 'before-actions': '', 'head-action': '' },
  })
  return { wrapper, router }
}

describe('移动端布局壳', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('渲染标题、副标题、用户名和页面内容', async () => {
    const { wrapper } = await buildShell()
    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('查看本人报修进度')
    expect(wrapper.text()).toContain('林同学')
    expect(wrapper.text()).toContain('学生')
    expect(wrapper.find('.page-body').text()).toBe('页面内容')
  })

  it('底部导航只展示已注册路由', async () => {
    const { wrapper } = await buildShell()
    const links = wrapper.findAll('.bottom-nav a')
    expect(links).toHaveLength(1)
    expect(links[0].text()).toBe('首页')
    expect(links[0].classes()).toContain('active')
    expect(wrapper.text()).not.toContain('我的工单')
  })

  it('退出登录清理状态并跳转登录页', async () => {
    const { wrapper, router } = await buildShell()
    await wrapper.find('button').trigger('click')
    await flushPromises()

    const app = useAppStore()
    expect(app.currentUser).toBeNull()
    expect(localStorage.getItem('dorm-repair-token')).toBeNull()
    // login 页面首次懒加载耗时较长，放宽轮询等待时间。
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('login'), { timeout: 10000 })
  })

  it('维修端三栏导航渲染已注册项', async () => {
    const workerNav = [
      { label: '工作台', to: '/worker/home' },
      { label: '我的工单', to: '/worker/orders' },
      { label: '请假', to: '/worker/leave' },
    ]
    setActivePinia(createPinia())
    useAppStore().setLoginState('jwt-token', { userId: 2, username: 'worker1', realName: '张师傅', roleType: 2 })
    const router = createAppRouter(createMemoryHistory())
    await router.push('/worker/home')
    await router.isReady()
    const wrapper = mount(MobileAppShell, {
      props: { role: 2 as const, title: '今日工作台', navItems: workerNav },
      global: { plugins: [router] },
    })
    const links = wrapper.findAll('.bottom-nav a')
    expect(links).toHaveLength(1)
    expect(links[0].text()).toBe('工作台')
  })
})
