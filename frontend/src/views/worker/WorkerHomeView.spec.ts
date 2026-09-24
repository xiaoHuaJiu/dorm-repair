import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { pageWorkerOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'
import WorkerHomeView from './WorkerHomeView.vue'

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
  await router.push('/worker/home')
  const wrapper = mount(WorkerHomeView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('维修人员工作台', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedPage.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 5 })
  })

  it('并行加载维修中、系统派单与异常三个区域', async () => {
    await mountView()
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 5, statusList: [2] })
    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 5, statusList: [1] })
    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 5, exceptionFlag: true })
  })

  it('展示各区域空态文案', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('当前没有维修中的工单')
    expect(wrapper.text()).toContain('暂无待接单任务')
    expect(wrapper.text()).toContain('暂无返工或中断工单')
  })

  it('全校工单区域登记接口缺失提示', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('全校工单查询接口待提供，暂无法展示。')
    expect(mockedPage).toHaveBeenCalledTimes(3)
  })

  it('区域加载失败展示错误并可单独重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('服务不可用'))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('服务不可用')
    mockedPage.mockResolvedValue({ records: [item()], total: 1, pageNum: 1, pageSize: 5 })
    const retryButtons = wrapper.findAll('.btn.outline.small')
    await retryButtons[0].trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('点击卡片跳转工单详情', async () => {
    mockedPage.mockResolvedValue({ records: [item()], total: 1, pageNum: 1, pageSize: 5 })
    const { router, wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.clickable-order').trigger('click')
    await vi.waitFor(
      () => {
        expect(router.currentRoute.value.name).toBe('worker-order-detail')
      },
      { timeout: 10000 },
    )
  })
})
