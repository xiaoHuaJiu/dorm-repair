import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { pageAdminOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'
import AdminDashboardView from './AdminDashboardView.vue'

vi.mock('@/api/order', () => ({
  pageAdminOrders: vi.fn(),
}))

const mockedPage = vi.mocked(pageAdminOrders)

function item(overrides: Partial<OrderListItem> = {}): OrderListItem {
  return {
    orderId: 1001,
    orderNo: 'WO202609200001',
    status: 2,
    statusName: '维修中',
    studentUid: 1,
    studentName: '林同学',
    faultTypeId: 5,
    faultTypeName: '水暖',
    campusId: 1,
    campusName: '东校区',
    areaId: 2,
    areaName: '学生生活区',
    buildingId: 3,
    buildingName: '3号楼',
    roomId: 4,
    roomName: '502室',
    locationText: '东校区 / 学生生活区 / 3号楼 / 502室',
    currentAssigneeId: 11,
    workerName: '张师傅',
    reportTime: '2026-09-20 09:40:00',
    acceptDeadline: '2026-09-20 10:30:00',
    completeDeadline: null,
    completeTime: null,
    exceptionFlag: 1,
    duplicateFlag: null,
    acceptTimeout: null,
    completeTimeout: null,
    ...overrides,
  }
}

async function mountView() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 3, username: 'admin', realName: '管理员', roleType: 3 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/admin/dashboard')
  const wrapper = mount(AdminDashboardView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('管理工作台', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedPage.mockResolvedValue({ records: [item()], total: 1, pageNum: 1, pageSize: 5 })
  })

  it('渲染四张统计卡片且数字占位（统计接口未提供）', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    const cards = wrapper.findAll('.metric-card')
    expect(cards).toHaveLength(4)
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.text()).toContain('待人工派单')
    expect(wrapper.text()).toContain('超时风险')
    expect(wrapper.text()).toContain('异常数量')
  })

  it('点击待审批卡片展开抽屉并可收起', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    const approval = wrapper.find('button.metric-card.approval')
    expect(approval.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('.approval-drawer').exists()).toBe(false)

    await approval.trigger('click')
    expect(approval.attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('.approval-drawer').exists()).toBe(true)
    expect(wrapper.text()).toContain('转派审批接口待提供')
    expect(wrapper.text()).toContain('请假审批接口待提供')

    await approval.trigger('click')
    expect(approval.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('.approval-drawer').exists()).toBe(false)
  })

  it('今日优先事项按异常工单查询并渲染', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 5, exceptionFlag: true })
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('点击优先事项行跳转工单详情', async () => {
    const { router, wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.clickable-order').trigger('click')
    await vi.waitFor(
      () => {
        expect(router.currentRoute.value.name).toBe('admin-order-detail')
        expect(router.currentRoute.value.params.id).toBe('1001')
      },
      { timeout: 10000 },
    )
  })

  it('优先事项加载失败展示错误并可重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('统计查询失败'))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('统计查询失败')
    await wrapper.find('.btn.outline.small').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('WO202609200001')
  })
})
