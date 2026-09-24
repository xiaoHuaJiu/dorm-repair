import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { studentOrderDetail } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import type { AreaTreeNode } from '@/types/config'
import type { OrderDetail } from '@/types/order'
import StudentOrderDetailView from './StudentOrderDetailView.vue'

vi.mock('@/api/order', () => ({
  studentOrderDetail: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  enabledAreaTree: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

const mockedDetail = vi.mocked(studentOrderDetail)
const mockedTree = vi.mocked(enabledAreaTree)
const mockedFaultTypes = vi.mocked(listEnabledFaultTypes)

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
      acceptTime: null,
      expectedCompleteTime: null,
      completeDeadline: null,
      repairSubmitTime: null,
      confirmTime: null,
      completeTime: null,
      reworkCount: 0,
      exceptionFlag: null,
      duplicateFlag: null,
      duplicateOrderId: null,
      reportTime: '2026-09-20 09:40:00',
      cancelTime: null,
      cancelReason: null,
    },
    processRecords: [],
    materialRecords: [],
    reworkRecords: [],
    evaluation: null,
    flows: [
      {
        id: 1,
        orderId: 1001,
        operationType: 1,
        fromStatus: null,
        toStatus: 0,
        originalAssigneeId: null,
        newAssigneeId: null,
        operatorId: 1,
        operatorRole: 1,
        sourceType: null,
        relatedBusinessId: null,
        reason: '填写并提交报修信息',
        operationTime: '2026-09-20 09:40:00',
        createTime: '2026-09-20 09:40:00',
      },
    ],
    ...overrides,
  }
}

async function mountView(id = '1001') {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 1, username: 'student01', realName: '林同学', roleType: 1 }
  const router = createAppRouter(createMemoryHistory())
  await router.push(`/student/orders/${id}`)
  const wrapper = mount(StudentOrderDetailView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

describe('学生工单详情', () => {
  beforeEach(() => {
    mockedDetail.mockReset()
    mockedTree.mockReset()
    mockedFaultTypes.mockReset()
    mockedDetail.mockResolvedValue(detail())
    mockedTree.mockResolvedValue(tree)
    mockedFaultTypes.mockResolvedValue([{ id: 5, typeCode: 'WATER', typeName: '水暖', status: 1, sortNo: 0, remark: null }])
  })

  it('并行加载详情、区域树与故障类型并还原位置与类型名称', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedDetail).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('水暖')
    expect(wrapper.text()).toContain('东校区 学生生活区 3号楼 502室 · 卫生间洗手池下方')
    expect(wrapper.text()).toContain('维修人员 #11')
    expect(wrapper.text()).toContain('共 1 个节点')
  })

  it('待派单状态展示禁用的取消按钮', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 0, currentAssigneeId: null } }))
    const { wrapper } = await mountView()
    await flushPromises()

    const button = wrapper.find('.section-title .btn.danger')
    expect(button.exists()).toBe(true)
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.attributes('title')).toBe('后端取消工单接口待提供')
    expect(wrapper.text()).toContain('待分配')
  })

  it('待确认状态展示验收入口并跳转验收页', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 3 } }))
    const { router, wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('等待你的确认')
    await wrapper.find('.btn.primary').trigger('click')
    await vi.waitFor(() => {
      expect(router.currentRoute.value.name).toBe('student-order-review')
    }, { timeout: 10000 })
  })

  it('非待确认状态不展示验收入口', async () => {
    const { wrapper } = await mountView()
    await flushPromises()
    expect(wrapper.text()).not.toContain('等待你的确认')
  })

  it('加载失败展示错误与重试', async () => {
    mockedDetail.mockRejectedValueOnce(new Error('数据不存在'))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('数据不存在')
    mockedDetail.mockResolvedValueOnce(detail())
    await wrapper.find('.btn.outline.small').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('WO202609200001')
  })
})
