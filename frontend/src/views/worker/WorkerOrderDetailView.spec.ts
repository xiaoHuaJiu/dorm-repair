import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { workerOrderDetail, acceptRepairOrder, interruptRepair, resumeRepair } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import type { AreaTreeNode } from '@/types/config'
import type { OrderDetail } from '@/types/order'
import WorkerOrderDetailView from './WorkerOrderDetailView.vue'

vi.mock('@/api/order', () => ({
  workerOrderDetail: vi.fn(),
  acceptRepairOrder: vi.fn(),
  interruptRepair: vi.fn(),
  resumeRepair: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  enabledAreaTree: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

const mockedDetail = vi.mocked(workerOrderDetail)
const mockedAccept = vi.mocked(acceptRepairOrder)
const mockedInterrupt = vi.mocked(interruptRepair)
const mockedResume = vi.mocked(resumeRepair)
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
  app.currentUser = { userId: 2, username: 'worker01', realName: '张师傅', roleType: 2 }
  const router = createAppRouter(createMemoryHistory())
  await router.push(`/worker/orders/${id}`)
  const wrapper = mount(WorkerOrderDetailView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('维修人员工单详情', () => {
  beforeEach(() => {
    mockedDetail.mockReset()
    mockedAccept.mockReset()
    mockedInterrupt.mockReset()
    mockedResume.mockReset()
    mockedTree.mockReset()
    mockedFaultTypes.mockReset()
    mockedDetail.mockResolvedValue(detail())
    mockedTree.mockResolvedValue(tree)
    mockedFaultTypes.mockResolvedValue([{ id: 5, typeCode: 'WATER', typeName: '水暖', status: 1, sortNo: 0, remark: null }])
    mockedAccept.mockResolvedValue(null)
    mockedInterrupt.mockResolvedValue(null)
    mockedResume.mockResolvedValue(null)
  })

  it('并行加载详情、区域树与故障类型并渲染信息与轨迹', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(mockedDetail).toHaveBeenCalledWith(1001)
    expect(wrapper.text()).toContain('水暖')
    expect(wrapper.text()).toContain('东校区 学生生活区 3号楼 502室 · 卫生间洗手池下方')
    expect(wrapper.text()).toContain('林同学 13800138000')
    expect(wrapper.text()).toContain('共 1 个节点')
  })

  it('维修中展示中断与转派入口，转派因接口缺失禁用', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('中断维修')
    expect(wrapper.text()).toContain('申请转派')
    const transfer = wrapper.find('button[title="后端转派申请接口待提供"]')
    expect(transfer.exists()).toBe(true)
    expect(transfer.attributes('disabled')).toBeDefined()
  })

  it('待接单状态接单：填写预计完成时间并提交后刷新', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 1 } }))
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.btn.primary').trigger('click')
    const dialog = wrapper.find('[role="dialog"]')
    expect(dialog.exists()).toBe(true)
    expect(dialog.text()).toContain('确认接单')

    await dialog.find('#accept-deadline').setValue('2026-09-25T18:00')
    const buttons = dialog.findAll('button')
    await buttons[1].trigger('click')
    await flushPromises()

    expect(mockedAccept).toHaveBeenCalledWith(1001, { expectedCompleteTime: '2026-09-25T18:00:00' })
    expect(mockedDetail).toHaveBeenCalledTimes(2)
  })

  it('已中断状态点击恢复维修调用接口并刷新', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 5 } }))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('恢复维修')
    const resume = wrapper.findAll('button').find((button) => button.text() === '恢复维修')
    expect(resume).toBeDefined()
    await resume!.trigger('click')
    await flushPromises()

    expect(mockedResume).toHaveBeenCalledWith(1001)
    expect(mockedDetail).toHaveBeenCalledTimes(2)
  })

  it('已完成状态无维修操作按钮', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 6 } }))
    const { wrapper } = await mountView()
    await flushPromises()

    const buttons = wrapper.findAll('button').map((button) => button.text())
    expect(buttons).not.toContain('接单')
    expect(buttons).not.toContain('中断维修')
    expect(buttons).not.toContain('恢复维修')
    expect(buttons).not.toContain('申请转派')
  })

  it('中断弹窗校验说明必填', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.btn.danger').trigger('click')
    const dialog = wrapper.find('[role="dialog"]')
    const buttons = dialog.findAll('button')
    await buttons[1].trigger('click')
    await flushPromises()

    expect(mockedInterrupt).not.toHaveBeenCalled()
    expect(dialog.text()).toContain('请填写中断说明')
  })

  it('中断弹窗提交调用接口并刷新', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.btn.danger').trigger('click')
    const dialog = wrapper.find('[role="dialog"]')
    await dialog.find('#interrupt-reason').setValue('3')
    await dialog.find('#interrupt-content').setValue('现场暂时无法施工')
    const buttons = dialog.findAll('button')
    await buttons[1].trigger('click')
    await flushPromises()

    expect(mockedInterrupt).toHaveBeenCalledWith(1001, { interruptReasonType: 3, content: '现场暂时无法施工' })
    expect(mockedDetail).toHaveBeenCalledTimes(2)
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
