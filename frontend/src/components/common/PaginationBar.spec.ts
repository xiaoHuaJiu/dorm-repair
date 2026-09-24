import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PaginationBar from './PaginationBar.vue'

describe('分页条', () => {
  it('渲染总数与页码', () => {
    const wrapper = mount(PaginationBar, {
      props: { total: 25, pageNum: 1, pageSize: 10 },
    })
    expect(wrapper.text()).toContain('共 25 条')
    expect(wrapper.text()).toContain('第 1 / 3 页')
  })

  it('第一页禁用上一页，最后一页禁用下一页', () => {
    const first = mount(PaginationBar, {
      props: { total: 25, pageNum: 1, pageSize: 10 },
    })
    const firstButtons = first.findAll('button')
    expect(firstButtons[0].attributes('disabled')).toBeDefined()
    expect(firstButtons[1].attributes('disabled')).toBeUndefined()

    const last = mount(PaginationBar, {
      props: { total: 25, pageNum: 3, pageSize: 10 },
    })
    const lastButtons = last.findAll('button')
    expect(lastButtons[0].attributes('disabled')).toBeUndefined()
    expect(lastButtons[1].attributes('disabled')).toBeDefined()
  })

  it('点击上一页/下一页发出页码变化', async () => {
    const wrapper = mount(PaginationBar, {
      props: { total: 25, pageNum: 2, pageSize: 10 },
    })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    expect(wrapper.emitted('update:pageNum')).toEqual([[1], [3]])
  })

  it('越界页码点击不发出事件', async () => {
    const wrapper = mount(PaginationBar, {
      props: { total: 25, pageNum: 3, pageSize: 10 },
    })
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('update:pageNum')).toBeUndefined()
  })

  it('总数为 0 时页码为 1/1', () => {
    const wrapper = mount(PaginationBar, {
      props: { total: 0, pageNum: 1, pageSize: 10 },
    })
    expect(wrapper.text()).toContain('第 1 / 1 页')
  })
})
