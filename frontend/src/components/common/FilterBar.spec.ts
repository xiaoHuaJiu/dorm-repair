import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import FilterBar from './FilterBar.vue'

describe('胶囊筛选组', () => {
  const options = [
    { label: '全部', value: '' },
    { label: '待接单', value: 1 },
    { label: '已完成', value: 6 },
  ]

  it('渲染全部选项并标记选中项', () => {
    const wrapper = mount(FilterBar, { props: { modelValue: '', options } })
    const buttons = wrapper.findAll('button')
    expect(buttons).toHaveLength(3)
    expect(buttons[0].text()).toBe('全部')
    expect(buttons[0].classes()).toContain('active')
    expect(buttons[0].attributes('aria-pressed')).toBe('true')
    expect(buttons[1].attributes('aria-pressed')).toBe('false')
  })

  it('点击选项后发出 update:modelValue', async () => {
    const wrapper = mount(FilterBar, { props: { modelValue: '', options } })
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([1])
  })

  it('通过 label 提供可访问名称', () => {
    const wrapper = mount(FilterBar, { props: { modelValue: '', options, label: '工单状态筛选' } })
    expect(wrapper.attributes('aria-label')).toBe('工单状态筛选')
  })
})
