import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { workerOrderDetail, addRepairProcess } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import type { OrderDetail } from '@/types/order'
import WorkerProcessView from './WorkerProcessView.vue'

vi.mock('@/api/order', () => ({
  workerOrderDetail: vi.fn(),
  addRepairProcess: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  enabledAreaTree: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

const mockedDetail = vi.mocked(workerOrderDetail)
const mockedAddProcess = vi.mocked(addRepairProcess)
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
      locationDetail: '卫生间洗手池下方',
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
    materialRecords: [],
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
  await router.push('/worker/orders/1001/process')
  const wrapper = mount(WorkerProcessView, { global: { plugins: [router] } })
  return { router, wrapper }
}

describe('维修过程记录页', () => {
  beforeEach(() => {
    mockedDetail.mockReset()
    mockedAddProcess.mockReset()
    mockedTree.mockReset()
    mockedFaultTypes.mockReset()
    mockedDetail.mockResolvedValue(detail())
    mockedTree.mockResolvedValue([])
    mockedFaultTypes.mockResolvedValue([{ id: 5, typeCode: 'WATER', typeName: '水暖', status: 1, sortNo: 0, remark: null }])
    mockedAddProcess.mockResolvedValue(null)
  })

  it('内容必填校验', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedAddProcess).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请填写处理内容')
  })

  it('提交成功后返回详情页', async () => {
    const { router, wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('#process-content').setValue('已检查接口，准备更换密封圈')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedAddProcess).toHaveBeenCalledWith(1001, { content: '已检查接口，准备更换密封圈' })
    await vi.waitFor(() => {
      expect(router.currentRoute.value.name).toBe('worker-order-detail')
    })
  })

  it('图片上传入口因接口缺失禁用并提示', async () => {
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('文件上传接口待提供，暂不能上传图片。')
    const uploader = wrapper.find('.thumb.add')
    expect(uploader.attributes('disabled')).toBeDefined()
  })

  it('状态不允许时提交按钮禁用', async () => {
    mockedDetail.mockResolvedValue(detail({ baseInfo: { ...detail().baseInfo, status: 3 } }))
    const { wrapper } = await mountView()
    await flushPromises()

    const submit = wrapper.find('button[type="submit"]')
    expect(submit.attributes('disabled')).toBeDefined()
    expect(submit.attributes('title')).toBe('当前状态不能添加处理记录')
  })
})
