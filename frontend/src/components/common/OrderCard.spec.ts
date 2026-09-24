import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderCard from './OrderCard.vue'
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
    currentAssigneeId: null,
    workerName: null,
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

describe('工单卡片', () => {
  it('展示工单号、故障类型、位置、状态和期限', () => {
    const wrapper = mount(OrderCard, {
      props: {
        order: buildOrder({ completeDeadline: '2026-09-25 18:00:00', statusName: '维修中' }),
      },
    })
    expect(wrapper.text()).toContain('WO202609200101')
    expect(wrapper.text()).toContain('水管漏水')
    expect(wrapper.text()).toContain('3 号楼 301 室')
    expect(wrapper.text()).toContain('维修中')
    expect(wrapper.text()).toContain('2026-09-25 18:00:00')
  })

  it('无期限时展示待确定期限', () => {
    const wrapper = mount(OrderCard, { props: { order: buildOrder() } })
    expect(wrapper.text()).toContain('待确定期限')
  })

  it('showOwner 时展示负责人，未分配展示待分配', () => {
    const wrapper = mount(OrderCard, {
      props: { order: buildOrder({ workerName: '张师傅' }), showOwner: true },
    })
    expect(wrapper.text()).toContain('负责人：张师傅')

    const unassigned = mount(OrderCard, { props: { order: buildOrder(), showOwner: true } })
    expect(unassigned.text()).toContain('负责人：待分配')
  })

  it('点击、回车和空格都触发打开详情', async () => {
    const order = buildOrder()
    const wrapper = mount(OrderCard, { props: { order } })

    await wrapper.trigger('click')
    expect(wrapper.emitted('open')?.[0]).toEqual([order])

    await wrapper.trigger('keydown', { key: 'Enter' })
    expect(wrapper.emitted('open')).toHaveLength(2)

    await wrapper.trigger('keydown', { key: ' ' })
    expect(wrapper.emitted('open')).toHaveLength(3)
  })

  it('通过 actions 插槽承载操作按钮', () => {
    const wrapper = mount(OrderCard, {
      props: { order: buildOrder() },
      slots: { actions: '<button class="cancel">取消工单</button>' },
    })
    expect(wrapper.find('.order-actions button.cancel').exists()).toBe(true)
  })

  it('卡片具备链接语义和可访问名称', () => {
    const wrapper = mount(OrderCard, { props: { order: buildOrder() } })
    expect(wrapper.attributes('role')).toBe('link')
    expect(wrapper.attributes('tabindex')).toBe('0')
    expect(wrapper.attributes('aria-label')).toContain('WO202609200101')
    expect(wrapper.attributes('aria-label')).toContain('水管漏水')
  })
})
