import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { workerOrderDetail, addMaterialUsage } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import type { OrderDetail } from '@/types/order'
import WorkerMaterialsView from './WorkerMaterialsView.vue'

vi.mock('@/api/order', () => ({
  workerOrderDetail: vi.fn(),
  addMaterialUsage: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  enabledAreaTree: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

const mockedDetail = vi.mocked(workerOrderDetail)
const mockedAddMaterial = vi.mocked(addMaterialUsage)
const mockedTree = vi.mocked(enabledAreaTree)
const mockedFaultTypes = vi.mocked(listEnabledFaultTypes)

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
      locationDetail: null,
      faultTypeId: 5,
      problemDescription: '洗手池持续漏水。',
      imageUrls: null,
      status: 2,
      currentAssigneeId: 11,
      dispatchTime: null,
      acceptDeadline: null,
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
    materialRecords: [
      {
        id: 1,
        orderId: 1001,
        workerId: 11,
        materialName: '密封圈',
        specification: '通用型',
        quantity: 1,
        unit: '个',
        remark: null,
        useTime: '2026-09-20 10:00:00',
        createTime: '2026-09-20 10:00:00',
      },
    ],
    reworkRecords: [],
    evaluation: null,
    flows: [],
    ...overrides,
  }
}

async function mountView() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 2, username: 'worker01', realName: '张师傅', roleType: 2 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/worker/orders/1001/materials')
  const wrapper = mount(WorkerMaterialsView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('维修材料登记页', () => {
  beforeEach(() => {
    mockedDetail.mockReset()
    mockedAddMaterial.mockReset()
    mockedTree.mockReset()
    mockedFaultTypes.mockReset()
    mockedDetail.mockResolvedValue(detail())
    mockedTree.mockResolvedValue([])
    mockedFaultTypes.mockResolvedValue([{ id: 5, typeCode: 'WATER', typeName: '水暖', status: 1, sortNo: 0, remark: null }])
    mockedAddMaterial.mockResolvedValue(null)
  })

  it('渲染已用材料列表', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('密封圈')
    expect(wrapper.text()).toContain('通用型 · 1个')
  })

  it('材料名称必填校验', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedAddMaterial).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请填写材料名称')
  })

  it('提交成功后返回详情页', async () => {
    const { router, wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('#material-name').setValue('LED灯泡')
    await wrapper.find('#material-spec').setValue('20W')
    await wrapper.find('#material-quantity').setValue('2')
    await wrapper.find('#material-unit').setValue('个')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedAddMaterial).toHaveBeenCalledWith(1001, {
      materialName: 'LED灯泡',
      specification: '20W',
      quantity: 2,
      unit: '个',
      remark: undefined,
    })
    await vi.waitFor(() => {
      expect(router.currentRoute.value.name).toBe('worker-order-detail')
    })
  })

  it('状态不允许时提交按钮禁用', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 5 } }))
    const { wrapper } = await mountView()
    await flushPromises()

    const submit = wrapper.find('button[type="submit"]')
    expect(submit.attributes('disabled')).toBeDefined()
    expect(submit.attributes('title')).toBe('当前状态不能登记材料')
  })
})
