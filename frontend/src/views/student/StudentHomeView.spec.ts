import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { pageStudentOrders } from '@/api/order'
import { areaChildren } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { notifySuccess } from '@/api/successNotifier'
import type { OrderListItem } from '@/types/order'
import StudentHomeView from './StudentHomeView.vue'

vi.mock('@/api/order', () => ({
  pageStudentOrders: vi.fn(),
  checkRepairDuplicate: vi.fn(),
  createRepairOrder: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  areaChildren: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

vi.mock('@/api/successNotifier', () => ({
  notifySuccess: vi.fn(),
}))

const mockedPage = vi.mocked(pageStudentOrders)
const mockedChildren = vi.mocked(areaChildren)
const mockedFaultTypes = vi.mocked(listEnabledFaultTypes)
const mockedNotifySuccess = vi.mocked(notifySuccess)

function order(overrides: Partial<OrderListItem> = {}): OrderListItem {
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
    locationText: '东校区 学生生活区 3号楼 502室',
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

function mountView() {
  setActivePinia(createPinia())
  const router = createAppRouter(createMemoryHistory())
  const wrapper = mount(StudentHomeView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

describe('学生首页', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedChildren.mockReset()
    mockedFaultTypes.mockReset()
    mockedNotifySuccess.mockReset()
    mockedPage.mockResolvedValue({ total: 0, pageNum: 1, pageSize: 50, records: [] })
    mockedChildren.mockResolvedValue([])
    mockedFaultTypes.mockResolvedValue([])
  })

  it('展示进行中工单 hero 与统计数量', async () => {
    mockedPage.mockResolvedValue({ total: 2, pageNum: 1, pageSize: 50, records: [order(), order({ orderId: 1002, status: 1 })] })
    const { wrapper } = mountView()
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith(expect.objectContaining({ statusList: [0, 1, 2, 4, 5] }))
    expect(wrapper.find('.hero').exists()).toBe(true)
    expect(wrapper.text()).toContain('WO202609200001')
    expect(wrapper.text()).toContain('水暖')
    expect(wrapper.text()).toContain('张师傅')
    expect(wrapper.find('.stat-value').text()).toBe('2')
  })

  it('无进行中工单展示空态卡片', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.find('.hero').exists()).toBe(false)
    expect(wrapper.text()).toContain('当前没有进行中的报修')
  })

  it('加载失败展示错误与重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('网络请求失败，请稍后重试'))
    const { wrapper } = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('网络请求失败，请稍后重试')

    mockedPage.mockResolvedValueOnce({ total: 1, pageNum: 1, pageSize: 50, records: [order()] })
    await wrapper.find('.btn.outline.small').trigger('click')
    await flushPromises()
    expect(wrapper.find('.hero').exists()).toBe(true)
  })

  it('点击我要报修打开报修弹窗', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.find('.student-repair-trigger').trigger('click')
    await flushPromises()
    expect(wrapper.find('.modal').exists()).toBe(true)
    expect(wrapper.find('.modal h2').text()).toBe('提交报修')
    expect(mockedChildren).toHaveBeenCalledWith(0)
    expect(mockedFaultTypes).toHaveBeenCalledTimes(1)
  })

  it('展示全校工单接口待提供提示', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('全校报修动态')
    expect(wrapper.text()).toContain('全校工单列表接口待后端提供')
  })
})
