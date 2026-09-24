import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfirmDialog from './ConfirmDialog.vue'

describe('确认弹窗', () => {
  it('不可见时不渲染', () => {
    const wrapper = mount(ConfirmDialog, { props: { visible: false, title: '确认操作' } })
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })

  it('展示标题、内容和按钮文案', () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: '取消工单', content: '取消后不可恢复，确定继续？', confirmText: '确认取消' },
    })
    expect(wrapper.text()).toContain('取消工单')
    expect(wrapper.text()).toContain('取消后不可恢复，确定继续？')
    expect(wrapper.text()).toContain('确认取消')
  })

  it('点击确认与取消分别发出事件', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: '确认操作', confirmText: '确认', cancelText: '取消' },
    })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
    await buttons[1].trigger('click')
    expect(wrapper.emitted('confirm')).toBeTruthy()
  })

  it('取消按钮同时请求关闭弹窗', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: '确认操作', confirmText: '确认', cancelText: '取消' },
    })
    await wrapper.findAll('button')[0].trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })

  it('危险操作使用 danger 样式', () => {
    const wrapper = mount(ConfirmDialog, { props: { visible: true, title: '停用账号', danger: true } })
    expect(wrapper.findAll('button')[1].classes()).toContain('danger')
  })

  it('提交中禁用按钮并阻止关闭', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: '确认操作', loading: true },
    })
    const buttons = wrapper.findAll('button')
    expect(buttons[0].attributes('disabled')).toBeDefined()
    expect(buttons[1].attributes('disabled')).toBeDefined()
    expect(buttons[1].text()).toBe('处理中…')
  })

  it('Escape 关闭弹窗', async () => {
    const wrapper = mount(ConfirmDialog, { props: { visible: true, title: '确认操作' } })
    await window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })
})
