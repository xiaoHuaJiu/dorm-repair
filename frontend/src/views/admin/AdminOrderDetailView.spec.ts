import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { adminOrderDetail } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { pageWorkers } from '@/api/worker'
import type { AreaTreeNode } from '@/types/config'
import type { OrderDetail } from '@/types/order'
import AdminOrderDetailView from './AdminOrderDetailView.vue'

vi.mock('@/api/order', () => ({
  adminOrderDetail: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  enabledAreaTree: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

vi.mock('@/api/worker', () => ({
  pageWorkers: vi.fn(),
}))

const mockedDetail = vi.mocked(adminOrderDetail)
const mockedTree = vi.mocked(enabledAreaTree)
const mockedFaultTypes = vi.mocked(listEnabledFaultTypes)
const mockedWorkers = vi.mocked(pageWorkers)

const tree: AreaTreeNode[] = [
  {
    id: 1, parentId: null, areaCode: 'C01', areaName: '东校区', areaType: 1, status: 1, sortNo: 0,
    children: [
      {
        id: 2, parentId: 1, areaCode: 'A01', areaName: '学生生活区', areaType: 2, status: 1, sortNo: 0,
        children: [
          {
            id: 3, parentId: 2, areaCode: 'B01', areaName: '3号楼', areaType: 3, status: 1, sortNo: 0,
            children: [
              { id: 4, parentId: 3, areaCode: 'R01', areaName: '502室', areaType: 4, status: 1, sortNo: 0, children: [] },
            ],
          },
        ],
      },
    ],
  },
]

function detail(overrides: Partial<OrderDetail> = {}): OrderDetail {
  return {
    baseInfo: {
      id: 1001,
      orderNo: 'WO202609200001',
      studentUid: 1,
      contactName: '林同学',
      contactPhone: '13800138000',
      campusId: 1,
      areaId: 2,
      buildingId: 3,
      roomId: 4,
      locationDetail: '卫生间洗手池下方',
      faultTypeId: 5,
      problemDescription: '洗手池持续漏水，阀门关闭后仍然滴水。',
      imageUrls: null,
      files: [],
      status: 2,
      currentAssigneeId: 11,
      dispatchTime: '2026-09-20 09:41:00',
      acceptDeadline: '2026-09-20 10:30:00',
      acceptTime: '2026-09-20 09:41:00',
      expectedCompleteTime: null,
      completeDeadline: '2026-09-21 09:41:00',
      repairSubmitTime: null,
      confirmTime: null,
      completeTime: null,
      reworkCount: 0,
      exceptionFlag: 1,
      duplicateFlag: null,
      duplicateOrderId: null,
      reportTime: '2026-09-20 09:40:00',
      cancelTime: null,
      cancelReason: null,
    },
    processRecords: [
      {
        id: 1,
        orderId: 1001,
        workerId: 11,
        recordType: 1,
        content: '已更换洗手池阀门并检查密封圈。',
        files: [],
        interruptReasonType: null,
        recordTime: '2026-09-20 10:00:00',
        createTime: '2026-09-20 10:00:00',
      },
    ],
    materialRecords: [
      {
        id: 1,
        orderId: 1001,
        workerId: 11,
        materialName: '水阀',
        specification: 'DN15',
        quantity: 1,
        unit: '个',
        remark: null,
        useTime: '2026-09-20 09:55:00',
        createTime: '2026-09-20 09:55:00',
      },
    ],
    reworkRecords: [],
    evaluation: null,
    flows: [
      {
        id: 1,
        orderId: 1001,
        operationType: 3,
        fromStatus: 1,
        toStatus: 2,
        originalAssigneeId: 11,
        newAssigneeId: 11,
        operatorId: 2,
        operatorRole: 2,
        sourceType: null,
        relatedBusinessId: null,
        reason: '维修人员接单',
        operationTime: '2026-09-20 09:41:00',
        createTime: '2026-09-20 09:41:00',
      },
    ],
    ...overrides,
  }
}

async function mountView(id = '1001') {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 3, username: 'admin', realName: '管理员', roleType: 3 }
  const router = createAppRouter(createMemoryHistory())
  await router.push(`/admin/orders/${id}`)
  const wrapper = mount(AdminOrderDetailView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('管理员工单详情', () => {
  beforeEach(() => {
    mockedDetail.mockReset()
    mockedTree.mockReset()
    mockedFaultTypes.mockReset()
    mockedWorkers.mockReset()
    mockedDetail.mockResolvedValue(detail())
    mockedTree.mockResolvedValue(tree)
    mockedFaultTypes.mockResolvedValue([{ id: 5, typeCode: 'WATER', typeName: '水暖', status: 1, sortNo: 0, remark: null }])
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

  it('并行加载详情与基础配置并渲染信息、负责人与轨迹', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedDetail).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('水暖')
    expect(wrapper.text()).toContain('东校区 学生生活区 3号楼 502室 · 卫生间洗手池下方')
    expect(wrapper.text()).toContain('张师傅')
    expect(wrapper.text()).toContain('林同学 13800138000')
    expect(wrapper.text()).toContain('共 1 个节点')
  })

  it('展示过程记录与材料使用记录', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已更换洗手池阀门并检查密封圈。')
    expect(wrapper.text()).toContain('水阀 × 1 个')
  })

  it('负责人列表加载失败不阻塞详情展示', async () => {
    mockedWorkers.mockRejectedValueOnce(new Error('人员列表失败'))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('水暖')
    expect(wrapper.text()).toContain('维修人员 #11')
  })

  it('加载失败展示错误并可重试', async () => {
    mockedDetail.mockRejectedValueOnce(new Error('查询失败'))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('查询失败')
    await wrapper.find('.btn.outline.small').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('水暖')
  })

  it('工单不存在时展示空态并可返回列表', async () => {
    mockedDetail.mockResolvedValue(null as unknown as OrderDetail)
    const { router, wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('工单不存在')
    const back = wrapper.findAll('button').find((button) => button.text() === '返回全部工单')
    expect(back).toBeDefined()
    await back!.trigger('click')
    await vi.waitFor(
      () => {
        expect(router.currentRoute.value.name).toBe('admin-orders')
      },
      { timeout: 10000 },
    )
  })
})
