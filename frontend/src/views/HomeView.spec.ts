import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import HomeView from './HomeView.vue'

describe('HomeView', () => {
  it('显示技术骨架已启动且不展示虚构业务数据', () => {
    const wrapper = mount(HomeView)

    expect(wrapper.get('h1').text()).toBe('宿舍报修系统')
    expect(wrapper.text()).toContain('前端技术骨架已启动')
    expect(wrapper.text()).not.toContain('模拟工单')
  })
})
