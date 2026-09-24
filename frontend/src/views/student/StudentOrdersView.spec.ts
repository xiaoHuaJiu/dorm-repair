import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { pageStudentOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'
import StudentOrdersView from './StudentOrdersView.vue'

vi.mock('@/api/order', () => ({
  pageStudentOrders: vi.fn(),
}))

const mockedPage = vi.mocked(pageStudentOrders)

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
  const wrapper = mount(StudentOrdersView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

describe('我的工单', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedPage.mockResolvedValue({ total: 0, pageNum: 1, pageSize: 10, records: [] })
  })

  it('渲染筛选栏（全部 + 8 个状态）与列表', async () => {
    mockedPage.mockResolvedValue({ total: 1, pageNum: 1, pageSize: 10, records: [order()] })
    const { wrapper } = mountView()
    await flushPromises()

    const filters = wrapper.findAll('.filter')
    expect(filters).toHaveLength(9)
    expect(filters[0].text()).toBe('全部')
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('切换状态筛选按状态重新请求并重置页码', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(mockedPage).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 1 }))

    const filters = wrapper.findAll('.filter')
    await filters[3].trigger('click')
    await flushPromises()

    expect(mockedPage).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 1, statusList: [2] }))
  })

  it('可取消状态展示取消按钮并禁用（接口待后端提供）', async () => {
    mockedPage.mockResolvedValue({ total: 1, pageNum: 1, pageSize: 10, records: [order({ status: 0, statusName: '待派单' })] })
    const { wrapper } = mountView()
    await flushPromises()

    const button = wrapper.find('.order-actions .btn.danger')
    expect(button.exists()).toBe(true)
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.attributes('title')).toBe('后端取消工单接口待提供')
  })

  it('不可取消状态隐藏取消按钮', async () => {
    mockedPage.mockResolvedValue({ total: 1, pageNum: 1, pageSize: 10, records: [order({ status: 6, statusName: '已完成' })] })
    const { wrapper } = mountView()
    await flushPromises()

    expect(wrapper.find('.order-actions .btn.danger').exists()).toBe(false)
  })

  it('空数据显示空状态文案', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('当前状态下没有工单')
  })

  it('加载失败展示错误并可重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('网络请求失败，请稍后重试'))
    const { wrapper } = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('网络请求失败，请稍后重试')
    mockedPage.mockResolvedValueOnce({ total: 1, pageNum: 1, pageSize: 10, records: [order()] })
    await wrapper.find('.btn.outline.small').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('WO202609200001')
  })
})
