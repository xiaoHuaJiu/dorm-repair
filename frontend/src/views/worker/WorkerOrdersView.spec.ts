import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { pageWorkerOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'
import WorkerOrdersView from './WorkerOrdersView.vue'

vi.mock('@/api/order', () => ({
  pageWorkerOrders: vi.fn(),
}))

const mockedPage = vi.mocked(pageWorkerOrders)

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
    exceptionFlag: null,
    duplicateFlag: null,
    acceptTimeout: null,
    completeTimeout: null,
    ...overrides,
  }
}

async function mountView() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 2, username: 'worker01', realName: '张师傅', roleType: 2 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/worker/orders')
  const wrapper = mount(WorkerOrdersView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('维修人员我的工单', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedPage.mockResolvedValue({ records: [item()], total: 1, pageNum: 1, pageSize: 10 })
  })

  it('默认查询全部状态并渲染工单', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('筛选按状态查询并重置页码', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    const filters = wrapper.findAll('.filter')
    // 选项顺序：全部、维修中、待确认、返工中、已中断、已完成、已取消
    expect(filters).toHaveLength(7)
    await filters[1].trigger('click')
    await flushPromises()

    expect(mockedPage).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, statusList: [2] })
  })

  it('空数据展示空态', async () => {
    mockedPage.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10 })
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('当前状态下没有负责工单')
  })

  it('加载失败展示错误并可重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('查询失败'))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('查询失败')
    await wrapper.find('.btn.outline.small').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('点击卡片跳转详情', async () => {
    const { router, wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.clickable-order').trigger('click')
    await vi.waitFor(
      () => {
        expect(router.currentRoute.value.name).toBe('worker-order-detail')
        expect(router.currentRoute.value.params.id).toBe('1001')
      },
      { timeout: 10000 },
    )
  })
})
