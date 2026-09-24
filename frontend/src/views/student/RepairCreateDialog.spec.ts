import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { areaChildren } from '@/api/area'
import { deleteFile, uploadFile } from '@/api/file'
import { listEnabledFaultTypes } from '@/api/faultType'
import { checkRepairDuplicate, createRepairOrder } from '@/api/order'
import { notifySuccess } from '@/api/successNotifier'
import type { AreaDetail } from '@/types/config'
import RepairCreateDialog from './RepairCreateDialog.vue'

vi.mock('@/api/area', () => ({
  areaChildren: vi.fn(),
}))

vi.mock('@/api/file', () => ({
  uploadFile: vi.fn(),
  deleteFile: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

vi.mock('@/api/order', () => ({
  checkRepairDuplicate: vi.fn(),
  createRepairOrder: vi.fn(),
}))

vi.mock('@/api/successNotifier', () => ({
  notifySuccess: vi.fn(),
}))

const mockedChildren = vi.mocked(areaChildren)
const mockedFaultTypes = vi.mocked(listEnabledFaultTypes)
const mockedCheck = vi.mocked(checkRepairDuplicate)
const mockedCreate = vi.mocked(createRepairOrder)
const mockedUpload = vi.mocked(uploadFile)
const mockedDelete = vi.mocked(deleteFile)
const mockedNotifySuccess = vi.mocked(notifySuccess)

function node(id: number, name: string): AreaDetail {
  return { id, parentId: 0, areaCode: `C${id}`, areaName: name, areaType: 1, sortNo: 0, status: 1, remark: null }
}

async function mountDialog() {
  const wrapper = mount(RepairCreateDialog, {
    props: { visible: true },
  })
  await flushPromises()
  return wrapper
}

async function fillLocation(wrapper: ReturnType<typeof mount>) {
  await wrapper.find('#modal-campus').setValue('1')
  await flushPromises()
  await wrapper.find('#modal-area').setValue('2')
  await flushPromises()
  await wrapper.find('#modal-building').setValue('3')
  await flushPromises()
  await wrapper.find('#modal-room').setValue('4')
}

async function fillForm(wrapper: ReturnType<typeof mount>) {
  await fillLocation(wrapper)
  await wrapper.find('#modal-fault').setValue('5')
  await wrapper.find('#modal-description').setValue('水龙头持续漏水')
  await wrapper.find('#modal-contact').setValue('林同学')
  await wrapper.find('#modal-phone').setValue('13800138000')
}

describe('提交报修弹窗', () => {
  beforeEach(() => {
    mockedChildren.mockReset()
    mockedFaultTypes.mockReset()
    mockedCheck.mockReset()
    mockedCreate.mockReset()
    mockedUpload.mockReset()
    mockedDelete.mockReset()
    mockedNotifySuccess.mockReset()
    mockedDelete.mockResolvedValue(null)
    mockedChildren.mockImplementation((parentId: number) => {
      if (parentId === 0) return Promise.resolve([node(1, '东校区')])
      if (parentId === 1) return Promise.resolve([node(2, '学生生活区')])
      if (parentId === 2) return Promise.resolve([node(3, '3号楼')])
      if (parentId === 3) return Promise.resolve([node(4, '502室')])
      return Promise.resolve([])
    })
    mockedFaultTypes.mockResolvedValue([{ id: 5, typeCode: 'WATER', typeName: '水暖', status: 1, sortNo: 0, remark: null }])
    mockedCheck.mockResolvedValue({ duplicate: false, suspectedOrders: [] })
    mockedCreate.mockResolvedValue({ created: true, duplicate: false, orderId: 1001, orderNo: 'WO202609200001', suspectedOrders: [] })
  })

  it('打开后加载根节点校区与启用故障类型', async () => {
    await mountDialog()
    expect(mockedChildren).toHaveBeenCalledWith(0)
    expect(mockedFaultTypes).toHaveBeenCalledTimes(1)
  })

  it('选择校区后加载下级区域并重置下级选择', async () => {
    const wrapper = await mountDialog()
    await wrapper.find('#modal-campus').setValue('1')
    await flushPromises()

    expect(mockedChildren).toHaveBeenCalledWith(1)
    expect(wrapper.find('#modal-area option').exists()).toBe(true)
  })

  it('必填校验失败不调用接口', async () => {
    const wrapper = await mountDialog()
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal .error').text()).toBe('请完整选择校区、区域、楼栋和房间')
    expect(mockedCheck).not.toHaveBeenCalled()
  })

  it('无重复时直接创建并通知成功', async () => {
    const wrapper = await mountDialog()
    await fillForm(wrapper)

    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCheck).toHaveBeenCalledWith({ campusId: 1, areaId: 2, buildingId: 3, roomId: 4, faultTypeId: 5 })
    expect(mockedCreate).toHaveBeenCalledWith(expect.objectContaining({
      bizNo: expect.any(String),
      campusId: 1,
      areaId: 2,
      buildingId: 3,
      roomId: 4,
      faultTypeId: 5,
      problemDescription: '水龙头持续漏水',
      contactName: '林同学',
      contactPhone: '13800138000',
      confirmDuplicate: false,
    }))
    expect(mockedNotifySuccess).toHaveBeenCalledWith('报修提交成功')
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('created')).toBeTruthy()
  })

  it('查重发现疑似工单时弹出确认，仍然提交携带 confirmDuplicate', async () => {
    mockedCheck.mockResolvedValue({
      duplicate: true,
      suspectedOrders: [
        { orderId: 900, orderNo: 'WO202609190010', faultTypeName: '水暖', locationText: '东校区 学生生活区 3号楼 502室', reportTime: '2026-09-19 20:00:00', status: 2, statusName: '维修中' },
      ],
    })
    const wrapper = await mountDialog()
    await fillForm(wrapper)

    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('发现相似未完成工单')
    expect(wrapper.text()).toContain('WO202609190010')

    // 疑似弹窗的"仍然提交"按钮（danger）。
    await wrapper.find('.modal .btn.danger').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith(expect.objectContaining({ confirmDuplicate: true }))
    expect(mockedNotifySuccess).toHaveBeenCalledWith('报修提交成功')
  })

  it('服务端创建时仍拦截重复则再次弹出疑似工单弹窗', async () => {
    mockedCreate.mockResolvedValue({
      created: false,
      duplicate: true,
      orderId: null,
      orderNo: '',
      suspectedOrders: [
        { orderId: 900, orderNo: 'WO202609190010', faultTypeName: '水暖', locationText: '东校区 学生生活区 3号楼 502室', reportTime: '2026-09-19 20:00:00', status: 2, statusName: '维修中' },
      ],
    })
    const wrapper = await mountDialog()
    await fillForm(wrapper)

    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('发现相似未完成工单')
    expect(mockedNotifySuccess).not.toHaveBeenCalled()
  })

  it('创建失败展示错误信息', async () => {
    mockedCreate.mockRejectedValue(new Error('重复提交过于频繁'))
    const wrapper = await mountDialog()
    await fillForm(wrapper)

    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal .error').text()).toBe('重复提交过于频繁')
  })

  it('上传现场图片后提交携带 fileIds，移除时删除未绑定文件', async () => {
    mockedUpload.mockResolvedValue({
      fileId: 9,
      originalName: 'a.jpg',
      fileType: 'IMAGE',
      contentType: 'image/jpeg',
      fileSize: 10,
      previewUrl: 'https://example.com/a.jpg',
    })
    const wrapper = await mountDialog()
    await fillForm(wrapper)

    const input = wrapper.find('input[type="file"]')
    const file = new File(['x'], 'a.jpg', { type: 'image/jpeg' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    expect(mockedUpload).toHaveBeenCalledWith(file, 'REPAIR')
    expect(wrapper.find('.thumb img').exists()).toBe(true)

    // 移除图片后删除未绑定文件并清空列表。
    await wrapper.find('.thumb-remove').trigger('click')
    await flushPromises()
    expect(mockedDelete).toHaveBeenCalledWith(9)
    expect(wrapper.find('.thumb img').exists()).toBe(false)

    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith(expect.objectContaining({ fileIds: [] }))
  })
})
