import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import AdminAppShell from './AdminAppShell.vue'

const ADMIN = { userId: 3, username: 'admin1', realName: '王老师', roleType: 3 }

async function buildShell() {
  setActivePinia(createPinia())
  useAppStore().setLoginState('jwt-token', ADMIN)
  const router = createAppRouter(createMemoryHistory())
  await router.push('/admin/dashboard')
  await router.isReady()
  const wrapper = mount(AdminAppShell, {
    props: { title: '工作台', subtitle: '全校报修与维修状态总览' },
    global: { plugins: [router] },
    slots: { default: '<div class="page-body">页面内容</div>', 'head-action': '' },
  })
  return { wrapper, router }
}

describe('管理端布局壳', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('渲染标题、副标题、用户名和页面内容', async () => {
    const { wrapper } = await buildShell()
    expect(wrapper.text()).toContain('工作台')
    expect(wrapper.text()).toContain('全校报修与维修状态总览')
    expect(wrapper.text()).toContain('王老师')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.find('.page-body').text()).toBe('页面内容')
  })

  it('侧栏展示已注册路由的分组，未注册路由自动隐藏', async () => {
    const { wrapper } = await buildShell()
    const sections = wrapper.findAll('.nav-section')
    // F4 已注册基础维护路由；F8 已注册工单管理路由，三组全部展示。
    expect(sections).toHaveLength(3)
    expect(sections[0].text()).toContain('工作台')
    expect(sections[1].text()).toContain('维修人员')
    expect(sections[1].text()).toContain('区域配置')
    expect(sections[1].text()).toContain('工作时间配置')
    expect(sections[1].text()).toContain('故障类型管理')
    expect(sections[2].text()).toContain('全部工单')
    expect(sections[2].text()).toContain('异常工单')
    expect(sections[2].text()).toContain('转派审批')
    expect(sections[2].text()).toContain('待人工派单')
    expect(sections[2].text()).toContain('请假审批')
  })

  it('当前路由对应导航项高亮', async () => {
    const { wrapper } = await buildShell()
    const active = wrapper.find('.nav-link.active')
    expect(active.exists()).toBe(true)
    expect(active.text()).toBe('工作台')
  })

  it('面包屑展示所属导航组与页面标题', async () => {
    const { wrapper } = await buildShell()
    const crumb = wrapper.find('.breadcrumb')
    expect(crumb.exists()).toBe(true)
    // 工作台页所在组名与页面标题相同，去重后只显示一个。
    expect(crumb.text()).toContain('工作台')
    expect(crumb.find('.breadcrumb-sep').exists()).toBe(false)
  })

  it('退出登录清理状态并跳转登录页', async () => {
    const { wrapper, router } = await buildShell()
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(useAppStore().currentUser).toBeNull()
    // login 页面首次懒加载耗时较长，放宽轮询等待时间。
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('login'), { timeout: 10000 })
  })
})
