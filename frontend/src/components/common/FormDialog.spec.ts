import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import FormDialog from './FormDialog.vue'

describe('表单弹窗', () => {
  it('展示标题、表单内容和操作按钮', () => {
    const wrapper = mount(FormDialog, {
      props: { visible: true, title: '新增故障类型', confirmText: '保存' },
      slots: { default: '<label>类型名称<input /></label>' },
    })
    expect(wrapper.find('[role="dialog"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('新增故障类型')
    expect(wrapper.find('label input').exists()).toBe(true)
    expect(wrapper.text()).toContain('保存')
    expect(wrapper.text()).toContain('取消')
  })

  it('提交和取消分别发出事件', async () => {
    const wrapper = mount(FormDialog, { props: { visible: true, title: '编辑区域' } })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
    await buttons[1].trigger('click')
    expect(wrapper.emitted('submit')).toBeTruthy()
  })

  it('取消按钮同时请求关闭弹窗', async () => {
    const wrapper = mount(FormDialog, { props: { visible: true, title: '编辑区域' } })
    await wrapper.findAll('button')[0].trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })

  it('展示表单错误信息', () => {
    const wrapper = mount(FormDialog, {
      props: { visible: true, title: '新增维修人员', error: '用户名已存在' },
    })
    expect(wrapper.find('[role="alert"]').text()).toBe('用户名已存在')
  })

  it('提交中禁用按钮并展示提交中文案', () => {
    const wrapper = mount(FormDialog, {
      props: { visible: true, title: '新增方案', loading: true },
    })
    const buttons = wrapper.findAll('button')
    expect(buttons[0].attributes('disabled')).toBeDefined()
    expect(buttons[1].attributes('disabled')).toBeDefined()
    expect(buttons[1].text()).toBe('提交中…')
  })

  it('Escape 关闭弹窗', async () => {
    const wrapper = mount(FormDialog, { props: { visible: true, title: '编辑' } })
    await window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
  })
})
