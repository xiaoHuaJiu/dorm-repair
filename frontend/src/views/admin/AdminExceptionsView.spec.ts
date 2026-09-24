import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { pageAdminOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'
import AdminExceptionsView from './AdminExceptionsView.vue'

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
    acceptTimeout: true,
    completeTimeout: null,
    ...overrides,
  }
}

async function mountView(query = '') {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 3, username: 'admin', realName: '管理员', roleType: 3 }
  const router = createAppRouter(createMemoryHistory())
  await router.push(`/admin/orders/exceptions${query}`)
  const wrapper = mount(AdminExceptionsView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('管理员异常工单', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedPage.mockResolvedValue({ records: [item()], total: 1, pageNum: 1, pageSize: 10 })
  })

  it('默认按全部异常 exceptionFlag 查询并渲染', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10, exceptionFlag: true })
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('带 type=timeout 参数时按即将超时 acceptTimeout 查询', async () => {
    await mountView('?type=timeout')
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10, acceptTimeout: true })
  })

  it('切换异常类型筛选重新查询并重置页码', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    const filters = wrapper.findAll('.filter')
    expect(filters).toHaveLength(2)
    await filters[1].trigger('click')
    await flushPromises()

    expect(mockedPage).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, acceptTimeout: true })

    await filters[0].trigger('click')
    await flushPromises()
    expect(mockedPage).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, exceptionFlag: true })
  })

  it('空数据展示空态', async () => {
    mockedPage.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10 })
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('当前没有异常工单')
  })

  it('点击行跳转工单详情', async () => {
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
})
