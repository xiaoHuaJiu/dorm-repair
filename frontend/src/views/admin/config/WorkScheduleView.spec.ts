import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { createWorkSchedule, pageWorkSchedules, updateWorkScheduleStatus } from '@/api/workSchedule'
import { notifySuccess } from '@/api/successNotifier'
import type { WorkScheduleItem } from '@/types/config'
import type { PageResult } from '@/api/types'
import WorkScheduleView from './WorkScheduleView.vue'

vi.mock('@/api/workSchedule', () => ({
  pageWorkSchedules: vi.fn(),
  createWorkSchedule: vi.fn(),
  updateWorkScheduleStatus: vi.fn(),
}))

vi.mock('@/api/successNotifier', () => ({
  notifySuccess: vi.fn(),
}))

const mockedPage = vi.mocked(pageWorkSchedules)
const mockedCreate = vi.mocked(createWorkSchedule)
const mockedStatus = vi.mocked(updateWorkScheduleStatus)
const mockedNotifySuccess = vi.mocked(notifySuccess)

const ITEMS: WorkScheduleItem[] = [
  {
    id: 1,
    scheduleName: '夏令时',
    startDate: '2026-06-01',
    endDate: '2026-09-30',
    workStartTime: '08:00',
    workEndTime: '18:00',
    status: 1,
    remark: null,
    createBy: 3,
    createTime: '2026-06-01 10:00:00',
    updateTime: null,
  },
  {
    id: 2,
    scheduleName: '冬令时',
    startDate: '2026-10-01',
    endDate: '2026-12-31',
    workStartTime: '08:30',
    workEndTime: '17:30',
    status: 0,
    remark: null,
    createBy: 3,
    createTime: '2026-06-01 10:00:00',
    updateTime: null,
  },
]

function buildPage(records: WorkScheduleItem[] = ITEMS): PageResult<WorkScheduleItem> {
  return { total: records.length, pageNum: 1, pageSize: 10, records }
}

function mountView() {
  setActivePinia(createPinia())
  const router = createAppRouter(createMemoryHistory())
  const wrapper = mount(WorkScheduleView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

describe('工作时间配置页', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedCreate.mockReset()
    mockedStatus.mockReset()
    mockedNotifySuccess.mockReset()
    mockedPage.mockResolvedValue(buildPage())
  })

  it('渲染方案卡片：名称、日期区间、时间区间与启停状态', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    const cards = wrapper.findAll('.schedule-card')
    expect(cards).toHaveLength(2)
    expect(cards[0].text()).toContain('夏令时')
    expect(cards[0].text()).toContain('2026-06-01 至 2026-09-30')
    expect(cards[0].text()).toContain('08:00 — 18:00')
    expect(cards[0].find('.tag.green').text()).toBe('当前启用')
    expect(cards[0].classes()).toContain('enabled')
    expect(cards[1].find('.tag.amber').text()).toBe('未启用')
  })

  it('启停按钮调用对应状态并刷新', async () => {
    mockedStatus.mockResolvedValue(null)
    const { wrapper } = mountView()
    await flushPromises()

    const firstCard = wrapper.findAll('.schedule-card')[0]
    await firstCard.find('button').trigger('click')
    await flushPromises()
    expect(mockedStatus).toHaveBeenCalledWith(1, 0)
    expect(mockedNotifySuccess).toHaveBeenCalledWith('方案已停用')
  })

  it('新增弹窗校验：名称为空不提交', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('新增时间方案')

    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()
    expect(wrapper.find('.modal .error').text()).toBe('方案名称不能为空')
    expect(mockedCreate).not.toHaveBeenCalled()
  })

  it('新增弹窗校验：结束日期早于开始日期', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')

    await wrapper.find('#schedule-name').setValue('测试方案')
    await wrapper.find('#schedule-start-date').setValue('2026-09-01')
    await wrapper.find('#schedule-end-date').setValue('2026-08-01')
    await wrapper.find('#schedule-start-time').setValue('08:00')
    await wrapper.find('#schedule-end-time').setValue('18:00')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal .error').text()).toBe('结束日期不能早于开始日期')
  })

  it('新增弹窗校验：下班时间必须晚于上班时间', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')

    await wrapper.find('#schedule-name').setValue('测试方案')
    await wrapper.find('#schedule-start-date').setValue('2026-09-01')
    await wrapper.find('#schedule-end-date').setValue('2026-09-30')
    await wrapper.find('#schedule-start-time').setValue('09:00')
    await wrapper.find('#schedule-end-time').setValue('09:00')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal .error').text()).toBe('下班时间必须晚于上班时间')
  })

  it('新增成功提交 status=1 并刷新列表', async () => {
    mockedCreate.mockResolvedValue({ id: 3 })
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')

    await wrapper.find('#schedule-name').setValue('寒假方案')
    await wrapper.find('#schedule-start-date').setValue('2027-01-15')
    await wrapper.find('#schedule-end-date').setValue('2027-02-20')
    await wrapper.find('#schedule-start-time').setValue('09:00')
    await wrapper.find('#schedule-end-time').setValue('17:00')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith({
      scheduleName: '寒假方案',
      startDate: '2027-01-15',
      endDate: '2027-02-20',
      workStartTime: '09:00',
      workEndTime: '17:00',
      status: 1,
      remark: undefined,
    })
    expect(mockedNotifySuccess).toHaveBeenCalledWith('已新增时间方案')
    expect(mockedPage).toHaveBeenCalledTimes(2)
  })

  it('空数据显示空状态', async () => {
    mockedPage.mockResolvedValue(buildPage([]))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无工作时间方案')
  })

  it('加载失败显示错误状态并可重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('网络错误'))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('重试')

    mockedPage.mockResolvedValueOnce(buildPage())
    await wrapper.find('.empty .btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('夏令时')
  })
})
