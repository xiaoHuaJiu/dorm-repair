import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import type { FaultTypeItem, WorkerDetail } from '@/types/config'
import { listWorkerAreaScopes, listWorkerFaultTypes } from '@/api/worker'
import WorkerCard from './WorkerCard.vue'

vi.mock('@/api/worker', () => ({
  listWorkerFaultTypes: vi.fn(),
  listWorkerAreaScopes: vi.fn(),
}))

const mockedFaultTypes = vi.mocked(listWorkerFaultTypes)
const mockedScopes = vi.mocked(listWorkerAreaScopes)

function buildWorker(overrides: Partial<WorkerDetail> = {}): WorkerDetail {
  return {
    workerId: 1,
    userId: 10,
    username: 'worker1',
    realName: '张师傅',
    phone: '13800138000',
    workerNo: 'W001',
    userStatus: 1,
    workStatus: 0,
    remark: null,
    ...overrides,
  }
}

const FAULT_OPTIONS: FaultTypeItem[] = [
  { id: 11, typeCode: 'SHUI', typeName: '水暖维修', status: 1, sortNo: 0, remark: null },
  { id: 12, typeCode: 'DIAN', typeName: '电路维修', status: 1, sortNo: 0, remark: null },
]

function areaNameOf(id: number): string | undefined {
  return ({ 1: '东校区', 2: '宿舍区', 3: '1 号楼' } as Record<number, string>)[id]
}

function mountCard(worker: WorkerDetail = buildWorker()) {
  return mount(WorkerCard, {
    props: { worker, faultTypeOptions: FAULT_OPTIONS, areaNameOf },
  })
}

describe('维修人员卡片', () => {
  beforeEach(() => {
    mockedFaultTypes.mockReset()
    mockedScopes.mockReset()
    mockedFaultTypes.mockResolvedValue([{ id: 1, workerId: 1, faultTypeId: 11, status: 1 }])
    mockedScopes.mockResolvedValue([
      { id: 1, workerId: 1, campusId: 1, areaId: 2, buildingId: 3, status: 1 },
    ])
  })

  it('渲染姓名、工作状态、账号与编号', () => {
    const wrapper = mountCard()
    expect(wrapper.find('h2').text()).toBe('张师傅')
    expect(wrapper.find('.tag.green').text()).toBe('正常')
    expect(wrapper.text()).toContain('worker1')
    expect(wrapper.text()).toContain('W001')
  })

  it('工作状态映射：请假中琥珀色、停用红色', () => {
    const onLeave = mountCard(buildWorker({ workStatus: 1 }))
    expect(onLeave.find('.tag.amber').text()).toBe('请假中')

    const disabled = mountCard(buildWorker({ workStatus: 2 }))
    expect(disabled.find('.tag.red').text()).toBe('停用')
  })

  it('加载技能与负责区域后展示名称', async () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('加载中…')

    await flushPromises()
    expect(wrapper.text()).toContain('水暖维修')
    expect(wrapper.text()).toContain('东校区/宿舍区/1 号楼')
  })

  it('技能与区域为空时显示未配置', async () => {
    mockedFaultTypes.mockResolvedValue([])
    mockedScopes.mockResolvedValue([])
    const wrapper = mountCard()
    await flushPromises()
    expect(wrapper.text()).toContain('技能：')
    expect(wrapper.text()).toContain('未配置')
  })

  it('技能或区域加载失败时显示占位符', async () => {
    mockedFaultTypes.mockRejectedValue(new Error('接口失败'))
    const wrapper = mountCard()
    await flushPromises()
    expect(wrapper.text()).toContain('—')
  })

  it('操作按钮分别发出事件', async () => {
    const wrapper = mountCard()
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    await buttons[2].trigger('click')

    expect(wrapper.emitted('edit')).toHaveLength(1)
    expect(wrapper.emitted('config')).toHaveLength(1)
    expect(wrapper.emitted('toggle-account')).toHaveLength(1)
  })

  it('账号停用时按钮文案为启用账号', () => {
    const wrapper = mountCard(buildWorker({ userStatus: 0 }))
    const buttons = wrapper.findAll('button')
    expect(buttons[2].text()).toBe('启用账号')
    expect(buttons[2].classes()).toContain('primary')
  })
})
