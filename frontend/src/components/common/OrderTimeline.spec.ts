import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderTimeline from './OrderTimeline.vue'
import type { OrderFlowItem } from '@/types/order'

function buildFlow(overrides: Partial<OrderFlowItem> = {}): OrderFlowItem {
  return {
    id: 1,
    orderId: 101,
    operationType: 1,
    fromStatus: null,
    toStatus: 0,
    originalAssigneeId: null,
    newAssigneeId: null,
    operatorId: 7,
    operatorRole: 1,
    sourceType: null,
    relatedBusinessId: null,
    reason: null,
    operationTime: '2026-09-24 09:00:00',
    createTime: '2026-09-24 09:00:00',
    ...overrides,
  }
}

describe('工单生命周期轨迹', () => {
  it('按最新在前倒序展示流转记录', () => {
    const flows = [
      buildFlow({ id: 1, operationType: 1, toStatus: 0, operationTime: '2026-09-24 09:00:00' }),
      buildFlow({ id: 2, operationType: 3, fromStatus: 1, toStatus: 2, operationTime: '2026-09-24 10:00:00' }),
    ]
    const wrapper = mount(OrderTimeline, { props: { flows } })
    const titles = wrapper.findAll('.timeline-title').map((node) => node.text())
    expect(titles).toEqual(['接单', '提交报修'])
    const times = wrapper.findAll('.timeline-time').map((node) => node.text())
    expect(times[0]).toBe('2026-09-24 10:00:00')
  })

  it('展示状态变化与原因', () => {
    const flows = [
      buildFlow({
        operationType: 4,
        fromStatus: 2,
        toStatus: 5,
        reason: '缺少配件，需采购',
      }),
    ]
    const wrapper = mount(OrderTimeline, { props: { flows } })
    expect(wrapper.text()).toContain('中断维修')
    expect(wrapper.text()).toContain('维修中 → 已中断')
    expect(wrapper.text()).toContain('缺少配件，需采购')
  })

  it('转派节点展示负责人变更', () => {
    const flows = [
      buildFlow({
        operationType: 99,
        fromStatus: 2,
        toStatus: 2,
        originalAssigneeId: 5,
        newAssigneeId: 9,
      }),
    ]
    const wrapper = mount(OrderTimeline, {
      props: {
        flows,
        assigneeNameOf: (id) => (id === 5 ? '张师傅' : '李师傅'),
      },
    })
    expect(wrapper.text()).toContain('负责人：张师傅 → 李师傅')
  })

  it('提供操作人映射时标题包含操作人姓名', () => {
    const flows = [buildFlow({ operationType: 3, fromStatus: 1, toStatus: 2 })]
    const wrapper = mount(OrderTimeline, {
      props: { flows, operatorNameOf: () => '张师傅' },
    })
    expect(wrapper.find('.timeline-title').text()).toBe('接单 · 张师傅')
  })

  it('未提供操作人映射时不伪造操作人', () => {
    const flows = [buildFlow({ operationType: 3, fromStatus: 1, toStatus: 2 })]
    const wrapper = mount(OrderTimeline, { props: { flows } })
    expect(wrapper.find('.timeline-title').text()).toBe('接单')
  })

  it('空流转记录展示空状态文案', () => {
    const wrapper = mount(OrderTimeline, { props: { flows: [], emptyText: '暂无流转记录' } })
    expect(wrapper.text()).toContain('暂无流转记录')
  })
})
