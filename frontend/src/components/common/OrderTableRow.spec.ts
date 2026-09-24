import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderTableRow from './OrderTableRow.vue'
import type { OrderListItem } from '@/types/order'

function buildOrder(overrides: Partial<OrderListItem> = {}): OrderListItem {
  return {
    orderId: 101,
    orderNo: 'WO202609200101',
    status: 1,
    statusName: '待接单',
    studentUid: 7,
    studentName: '林同学',
    faultTypeId: 3,
    faultTypeName: '水管漏水',
    campusId: 1,
    campusName: '东校区',
    areaId: 2,
    areaName: '生活区',
    buildingId: 5,
    buildingName: '3 号楼',
    roomId: 12,
    roomName: '301 室',
    locationText: '3 号楼 301 室',
    currentAssigneeId: 9,
    workerName: '张师傅',
    reportTime: '2026-09-24 09:00:00',
    acceptDeadline: null,
    completeDeadline: null,
    completeTime: null,
    exceptionFlag: null,
    duplicateFlag: null,
    acceptTimeout: null,
    completeTimeout: null,
    ...overrides,
  }
}

describe('管理端工单表格行', () => {
  it('渲染工单号、标题、位置、状态与负责人', () => {
    const wrapper = mount(OrderTableRow, { props: { order: buildOrder() } })
    expect(wrapper.text()).toContain('WO202609200101')
    expect(wrapper.text()).toContain('水管漏水')
    expect(wrapper.text()).toContain('3 号楼 301 室')
    expect(wrapper.text()).toContain('待接单')
    expect(wrapper.text()).toContain('张师傅')
  })

  it('未分配负责人展示待分配', () => {
    const wrapper = mount(OrderTableRow, { props: { order: buildOrder({ workerName: null }) } })
    expect(wrapper.text()).toContain('待分配')
  })

  it('异常与超时映射为标签，无异常展示占位', () => {
    const flagged = mount(OrderTableRow, {
      props: {
        order: buildOrder({ acceptTimeout: true, completeTimeout: true, duplicateFlag: 1, exceptionFlag: 1 }),
      },
    })
    expect(flagged.text()).toContain('接单超时')
    expect(flagged.text()).toContain('完成超时')
    expect(flagged.text()).toContain('疑似重复')
    expect(flagged.text()).toContain('异常')

    const clean = mount(OrderTableRow, { props: { order: buildOrder() } })
    expect(clean.text()).toContain('—')
  })

  it('点击整行触发打开详情', async () => {
    const order = buildOrder()
    const wrapper = mount(OrderTableRow, { props: { order } })
    await wrapper.trigger('click')
    expect(wrapper.emitted('open')?.[0]).toEqual([order])
  })

  it('键盘回车和空格触发打开详情', async () => {
    const order = buildOrder()
    const wrapper = mount(OrderTableRow, { props: { order } })
    await wrapper.trigger('keydown', { key: 'Enter' })
    await wrapper.trigger('keydown', { key: ' ' })
    expect(wrapper.emitted('open')).toHaveLength(2)
  })

  it('点击行内按钮不触发整行跳转', async () => {
    const wrapper = mount(OrderTableRow, {
      props: { order: buildOrder() },
      slots: { actions: '<button class="row-op">操作</button>' },
    })
    await wrapper.find('.row-op').trigger('click')
    expect(wrapper.emitted('open')).toBeUndefined()
  })

  it('行具备链接语义与可访问名称', () => {
    const wrapper = mount(OrderTableRow, { props: { order: buildOrder() } })
    expect(wrapper.attributes('role')).toBe('link')
    expect(wrapper.attributes('tabindex')).toBe('0')
    expect(wrapper.attributes('aria-label')).toContain('WO202609200101')
  })
})
