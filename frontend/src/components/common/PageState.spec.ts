import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PageState from './PageState.vue'

describe('页面通用反馈状态', () => {
  it('加载中展示骨架屏', () => {
    const wrapper = mount(PageState, { props: { loading: true } })
    expect(wrapper.find('.skeleton-block').exists()).toBe(true)
    expect(wrapper.attributes('aria-busy')).toBe('true')
  })

  it('出错时展示错误信息并可通过重试恢复', async () => {
    const wrapper = mount(PageState, { props: { error: '网络开小差了', loading: false } })
    expect(wrapper.find('[role="alert"]').text()).toContain('网络开小差了')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('retry')).toBeTruthy()
  })

  it('空数据展示空状态文案', () => {
    const wrapper = mount(PageState, {
      props: { loading: false, error: null, empty: true, emptyText: '暂无工单', emptyMark: '✓' },
    })
    expect(wrapper.text()).toContain('暂无工单')
    expect(wrapper.find('.empty-mark').text()).toBe('✓')
  })

  it('正常状态渲染默认插槽内容', () => {
    const wrapper = mount(PageState, {
      props: { loading: false, error: null, empty: false },
      slots: { default: '<div class="real-content">真实内容</div>' },
    })
    expect(wrapper.find('.real-content').exists()).toBe(true)
    expect(wrapper.find('.empty').exists()).toBe(false)
  })
})
