import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import {
  createFaultType,
  pageFaultTypes,
  updateFaultType,
  updateFaultTypeStatus,
} from '@/api/faultType'
import { notifySuccess } from '@/api/successNotifier'
import type { FaultTypeItem } from '@/types/config'
import type { PageResult } from '@/api/types'
import FaultTypeView from './FaultTypeView.vue'

vi.mock('@/api/faultType', () => ({
  pageFaultTypes: vi.fn(),
  createFaultType: vi.fn(),
  updateFaultType: vi.fn(),
  updateFaultTypeStatus: vi.fn(),
}))

vi.mock('@/api/successNotifier', () => ({
  notifySuccess: vi.fn(),
}))

const mockedPage = vi.mocked(pageFaultTypes)
const mockedCreate = vi.mocked(createFaultType)
const mockedUpdate = vi.mocked(updateFaultType)
const mockedStatus = vi.mocked(updateFaultTypeStatus)
const mockedNotifySuccess = vi.mocked(notifySuccess)

const ITEMS: FaultTypeItem[] = [
  { id: 1, typeCode: 'SHUI', typeName: '水暖维修', status: 1, sortNo: 0, remark: null },
  { id: 2, typeCode: 'DIAN', typeName: '电路维修', status: 0, sortNo: 1, remark: null },
]

function buildPage(records: FaultTypeItem[] = ITEMS): PageResult<FaultTypeItem> {
  return { total: records.length, pageNum: 1, pageSize: 9, records }
}

function mountView() {
  setActivePinia(createPinia())
  const router = createAppRouter(createMemoryHistory())
  const wrapper = mount(FaultTypeView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

describe('故障类型管理页', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedCreate.mockReset()
    mockedUpdate.mockReset()
    mockedStatus.mockReset()
    mockedNotifySuccess.mockReset()
    mockedPage.mockResolvedValue(buildPage())
  })

  it('渲染类型卡片列表与启停状态', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 9 })
    expect(wrapper.findAll('.fault-card')).toHaveLength(2)
    expect(wrapper.text()).toContain('水暖维修')
    expect(wrapper.find('.tag.green').text()).toBe('启用')
    expect(wrapper.find('.tag.amber').text()).toBe('停用')
  })

  it('删除按钮禁用并提示后端无接口', async () => {
    mockedPage.mockResolvedValue(buildPage([ITEMS[0]]))
    const { wrapper } = mountView()
    await flushPromises()
    const removeButton = wrapper.find('.fault-card button[disabled][title]')
    expect(removeButton.exists()).toBe(true)
    expect(removeButton.text()).toBe('删除')
    expect(removeButton.attributes('title')).toContain('后端暂无删除接口')
  })

  it('新增弹窗校验编码与名称，成功提交并刷新', async () => {
    mockedCreate.mockResolvedValue({ id: 3 })
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.find('.page-head .btn.primary').trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('新增故障类型')

    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()
    expect(wrapper.find('.modal .error').text()).toBe('类型编码不能为空')

    await wrapper.find('#fault-code').setValue('MEN')
    await wrapper.find('#fault-name').setValue('门窗维修')
    await wrapper.find('#fault-sort').setValue('2')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith({ typeCode: 'MEN', typeName: '门窗维修', sortNo: 2, remark: undefined })
    expect(mockedNotifySuccess).toHaveBeenCalledWith('已新增故障类型')
    expect(mockedPage).toHaveBeenCalledTimes(2)
  })

  it('修改弹窗展示只读编码并提交修改', async () => {
    mockedUpdate.mockResolvedValue(null)
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.findAll('.fault-card .btn.outline')[0].trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('修改故障类型')
    expect((wrapper.find('#fault-code').element as HTMLInputElement).disabled).toBe(true)
    expect((wrapper.find('#fault-code').element as HTMLInputElement).value).toBe('SHUI')

    await wrapper.find('#fault-name').setValue('水暖管线维修')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedUpdate).toHaveBeenCalledWith(1, expect.objectContaining({ typeName: '水暖管线维修' }))
  })

  it('启停按钮切换状态并刷新列表', async () => {
    mockedStatus.mockResolvedValue(null)
    const { wrapper } = mountView()
    await flushPromises()

    const cardButtons = wrapper.findAll('.fault-card')[0].findAll('button')
    const toggle = cardButtons.find((button) => button.text() === '停用')
    await toggle?.trigger('click')
    await flushPromises()

    expect(mockedStatus).toHaveBeenCalledWith(1, 0)
    expect(mockedNotifySuccess).toHaveBeenCalledWith('已停用')
  })

  it('翻页时携带新页码重新查询', async () => {
    mockedPage.mockResolvedValue({ total: 20, pageNum: 2, pageSize: 9, records: [ITEMS[0]] })
    const { wrapper } = mountView()
    await flushPromises()

    const buttons = wrapper.findAll('.pagination-bar button')
    await buttons[1].trigger('click')
    await flushPromises()
    expect(mockedPage).toHaveBeenLastCalledWith({ pageNum: 2, pageSize: 9 })
  })

  it('空数据显示空状态', async () => {
    mockedPage.mockResolvedValue(buildPage([]))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无故障类型')
  })

  it('加载失败显示错误状态并可重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('网络错误'))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('重试')

    mockedPage.mockResolvedValueOnce(buildPage())
    await wrapper.find('.empty .btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('水暖维修')
  })
})
