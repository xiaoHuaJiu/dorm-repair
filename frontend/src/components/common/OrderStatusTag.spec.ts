import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderStatusTag from './OrderStatusTag.vue'

describe('工单状态标签', () => {
  it('后端返回状态文案时优先展示', () => {
    const wrapper = mount(OrderStatusTag, { props: { status: 1, statusName: '待接单' } })
    expect(wrapper.text()).toBe('待接单')
  })

  it('后端未返回文案时按状态数值映射', () => {
    const wrapper = mount(OrderStatusTag, { props: { status: 0, statusName: null } })
    expect(wrapper.text()).toBe('待派单')
  })

  it('色调与状态对应', () => {
    expect(mount(OrderStatusTag, { props: { status: 6 } }).classes()).toContain('green')
    expect(mount(OrderStatusTag, { props: { status: 7 } }).classes()).toContain('gray')
    expect(mount(OrderStatusTag, { props: { status: 1 } }).classes()).toContain('amber')
    expect(mount(OrderStatusTag, { props: { status: 2 } }).classes()).toContain('red')
  })

  it('未知状态数值显示默认文案', () => {
    const wrapper = mount(OrderStatusTag, { props: { status: 99 } })
    expect(wrapper.text()).toBe('未知状态')
  })
})
