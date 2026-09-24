import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ImageUploader from './ImageUploader.vue'

describe('图片上传与预览', () => {
  it('展示已上传图片缩略图', () => {
    const wrapper = mount(ImageUploader, {
      props: { modelValue: ['https://example.com/a.jpg', 'https://example.com/b.jpg'] },
    })
    const images = wrapper.findAll('img')
    expect(images).toHaveLength(2)
    expect(images[0].attributes('src')).toBe('https://example.com/a.jpg')
  })

  it('移除缩略图后发出 update:modelValue', async () => {
    const wrapper = mount(ImageUploader, {
      props: { modelValue: ['https://example.com/a.jpg'] },
    })
    await wrapper.find('.thumb-remove').trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([[]])
  })

  it('选择文件后发出 add-files，并限制剩余数量', async () => {
    const wrapper = mount(ImageUploader, {
      props: { modelValue: ['https://example.com/a.jpg'], max: 2 },
    })
    const input = wrapper.find('input[type="file"]')
    const files = [new File(['1'], 'a.png'), new File(['2'], 'b.png'), new File(['3'], 'c.png')]
    Object.defineProperty(input.element, 'files', { value: files })

    await input.trigger('change')
    const emitted = wrapper.emitted('add-files')?.[0]?.[0] as File[]
    expect(emitted).toHaveLength(1)
  })

  it('达到上限后不再展示添加按钮', () => {
    const wrapper = mount(ImageUploader, {
      props: { modelValue: ['a.jpg', 'b.jpg', 'c.jpg'], max: 3 },
    })
    expect(wrapper.find('.thumb.add').exists()).toBe(false)
  })

  it('禁用状态禁止添加与移除', () => {
    const wrapper = mount(ImageUploader, {
      props: { modelValue: ['a.jpg'], disabled: true },
    })
    expect(wrapper.find('.thumb.add').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.thumb-remove').attributes('disabled')).toBeDefined()
  })

  it('通过 hint 插槽提示上传限制', () => {
    const wrapper = mount(ImageUploader, {
      props: { modelValue: [] },
      slots: { hint: '图片上传接口尚未提供，暂不支持上传' },
    })
    expect(wrapper.text()).toContain('图片上传接口尚未提供，暂不支持上传')
  })
})
