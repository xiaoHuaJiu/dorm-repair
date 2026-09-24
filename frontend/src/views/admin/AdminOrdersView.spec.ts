import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { pageAdminOrders } from '@/api/order'
import { pageWorkers } from '@/api/worker'
import { enabledAreaTree } from '@/api/area'
import type { AreaTreeNode } from '@/types/config'
import type { OrderListItem } from '@/types/order'
import AdminOrdersView from './AdminOrdersView.vue'

vi.mock('@/api/order', () => ({
  pageAdminOrders: vi.fn(),
}))

vi.mock('@/api/worker', () => ({
  pageWorkers: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  enabledAreaTree: vi.fn(),
}))

const mockedPage = vi.mocked(pageAdminOrders)
const mockedWorkers = vi.mocked(pageWorkers)
const mockedTree = vi.mocked(enabledAreaTree)

const tree: AreaTreeNode[] = [
  {
    id: 1, parentId: null, areaCode: 'C01', areaName: '东校区', areaType: 1, status: 1, sortNo: 0,
    children: [],
  },
]

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
  app.currentUser = { userId: 3, username: 'admin', realName: '管理员', roleType: 3 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/admin/orders')
  const wrapper = mount(AdminOrdersView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('管理员全部工单', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedWorkers.mockReset()
    mockedTree.mockReset()
    mockedPage.mockResolvedValue({ records: [item()], total: 1, pageNum: 1, pageSize: 10 })
    mockedTree.mockResolvedValue(tree)
    mockedWorkers.mockResolvedValue({
      records: [
        {
          workerId: 11,
          userId: 2,
          username: 'worker01',
          realName: '张师傅',
          phone: '13800138000',
          workerNo: 'W001',
          userStatus: 1,
          workStatus: 1,
          remark: null,
        },
      ],
      total: 1,
      pageNum: 1,
      pageSize: 500,
    })
  })

  it('并行加载筛选选项并默认查询全部工单', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(mockedTree).toHaveBeenCalled()
    expect(mockedWorkers).toHaveBeenCalledWith({ pageNum: 1, pageSize: 500 })
    expect(wrapper.text()).toContain('WO202609200001')
  })

  it('按校区与负责人筛选时携带参数并重置页码', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('#filter-campus').setValue(1)
    await wrapper.find('#filter-worker').setValue(11)
    await flushPromises()

    expect(mockedPage).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, campusId: 1, workerId: 11 })
  })

  it('按状态筛选传递 statusList', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('#filter-status').setValue(2)
    await flushPromises()

    expect(mockedPage).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10, statusList: [2] })
  })

  it('空数据展示空态', async () => {
    mockedPage.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10 })
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('当前筛选条件下没有工单')
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
