import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { confirmRepairOrder, studentOrderDetail, submitRepairEvaluation, submitRepairRework } from '@/api/order'
import { deleteFile, uploadFile } from '@/api/file'
import type { OrderDetail } from '@/types/order'
import StudentOrderReviewView from './StudentOrderReviewView.vue'

vi.mock('@/api/order', () => ({
  studentOrderDetail: vi.fn(),
  confirmRepairOrder: vi.fn(),
  submitRepairEvaluation: vi.fn(),
  submitRepairRework: vi.fn(),
}))

vi.mock('@/api/file', () => ({
  uploadFile: vi.fn(),
  deleteFile: vi.fn(),
}))

const mockedDetail = vi.mocked(studentOrderDetail)
const mockedConfirm = vi.mocked(confirmRepairOrder)
const mockedEvaluation = vi.mocked(submitRepairEvaluation)
const mockedRework = vi.mocked(submitRepairRework)
const mockedUpload = vi.mocked(uploadFile)
const mockedDelete = vi.mocked(deleteFile)

function detail(status: number, processRecords: OrderDetail['processRecords'] = [], evaluation: OrderDetail['evaluation'] = null): OrderDetail {
  return {
    baseInfo: {
      id: 1001,
      orderNo: 'WO202609200001',
      studentUid: 1,
      contactName: '林同学',
      contactPhone: '13800138000',
      campusId: 1,
      areaId: 2,
      buildingId: 3,
      roomId: 4,
      locationDetail: null,
      faultTypeId: 5,
      problemDescription: '洗手池持续漏水，阀门关闭后仍然滴水。',
      imageUrls: null,
      files: [],
      status,
      currentAssigneeId: 11,
      dispatchTime: null,
      acceptDeadline: null,
      acceptTime: null,
      expectedCompleteTime: null,
      completeDeadline: null,
      repairSubmitTime: '2026-09-20 11:30:00',
      confirmTime: null,
      completeTime: status === 6 ? '2026-09-20 12:00:00' : null,
      reworkCount: 0,
      exceptionFlag: null,
      duplicateFlag: null,
      duplicateOrderId: null,
      reportTime: '2026-09-20 09:40:00',
      cancelTime: null,
      cancelReason: null,
    },
    processRecords,
    materialRecords: [],
    reworkRecords: [],
    evaluation,
    flows: [],
  }
}

async function mountView() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 1, username: 'student01', realName: '林同学', roleType: 1 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/student/orders/1001/review')
  const wrapper = mount(StudentOrderReviewView, {
    global: { plugins: [router] },
  })
  return { wrapper }
}

describe('验收维修结果', () => {
  beforeEach(() => {
    mockedDetail.mockReset()
    mockedConfirm.mockReset()
    mockedEvaluation.mockReset()
    mockedRework.mockReset()
    mockedUpload.mockReset()
    mockedDelete.mockReset()
    mockedConfirm.mockResolvedValue(null)
    mockedEvaluation.mockResolvedValue(null)
    mockedRework.mockResolvedValue(null)
    mockedDelete.mockResolvedValue(null)
  })

  it('展示最新一条提交维修结果说明', async () => {
    mockedDetail.mockResolvedValue(detail(3, [
      { id: 1, orderId: 1001, workerId: 11, recordType: 1, content: '检查中', files: [], interruptReasonType: null, recordTime: '2026-09-20 10:00:00', createTime: '2026-09-20 10:00:00' },
      { id: 2, orderId: 1001, workerId: 11, recordType: 4, content: '已更换密封圈，不再渗水。', files: [], interruptReasonType: null, recordTime: '2026-09-20 11:30:00', createTime: '2026-09-20 11:30:00' },
    ]))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已更换密封圈，不再渗水。')
  })

  it('无维修结果时展示默认说明', async () => {
    mockedDetail.mockResolvedValue(detail(3))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('维修人员尚未提交完成说明')
  })

  it('待确认状态展示确认与返工按钮，点击确认调用接口并刷新', async () => {
    mockedDetail.mockResolvedValue(detail(3))
    const { wrapper } = await mountView()
    await flushPromises()

    const confirmButton = wrapper.find('.inline-actions .btn.primary')
    const reworkButton = wrapper.find('.inline-actions .btn.danger')
    expect(confirmButton.exists()).toBe(true)
    expect(confirmButton.attributes('disabled')).toBeUndefined()
    expect(reworkButton.exists()).toBe(true)
    expect(wrapper.find('.stars').exists()).toBe(false)

    await confirmButton.trigger('click')
    await flushPromises()

    expect(mockedConfirm).toHaveBeenCalledWith(1001)
    expect(mockedDetail).toHaveBeenCalledTimes(2)
  })

  it('待确认状态打开返工弹窗，填写原因并上传图片后提交', async () => {
    mockedUpload.mockResolvedValue({
      fileId: 8,
      originalName: 'r.jpg',
      fileType: 'IMAGE',
      contentType: 'image/jpeg',
      fileSize: 10,
      previewUrl: 'https://example.com/r.jpg',
    })
    mockedDetail.mockResolvedValue(detail(3))
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.inline-actions .btn.danger').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('申请返工')

    const input = wrapper.find('input[type="file"]')
    const file = new File(['x'], 'r.jpg', { type: 'image/jpeg' })
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()
    expect(mockedUpload).toHaveBeenCalledWith(file, 'REWORK')

    await wrapper.find('#rework-reason').setValue('仍有渗水')
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedRework).toHaveBeenCalledWith(1001, { reason: '仍有渗水', fileIds: [8] })
    expect(mockedDetail).toHaveBeenCalledTimes(2)
  })

  it('已完成状态展示评价区，选择星级后提交评价', async () => {
    mockedDetail.mockResolvedValue(detail(6))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.find('.stars').exists()).toBe(true)
    const stars = wrapper.findAll('.star')
    expect(stars).toHaveLength(5)

    await stars[2].trigger('click')
    expect(stars[0].classes()).toContain('active')
    expect(stars[1].classes()).toContain('active')
    expect(stars[2].classes()).toContain('active')
    expect(stars[3].classes()).not.toContain('active')

    const submitRating = wrapper.find('.btn.submit-rating')
    expect(submitRating.attributes('disabled')).toBeUndefined()
    await submitRating.trigger('click')
    await flushPromises()

    expect(mockedEvaluation).toHaveBeenCalledWith(1001, { score: 3, content: undefined })
    expect(mockedDetail).toHaveBeenCalledTimes(2)
  })

  it('未选择星级时提交评价提示校验错误', async () => {
    mockedDetail.mockResolvedValue(detail(6))
    const { wrapper } = await mountView()
    await flushPromises()

    await wrapper.find('.btn.submit-rating').trigger('click')
    await flushPromises()

    expect(mockedEvaluation).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请选择评分')
  })

  it('已有评价时展示评分并禁用编辑', async () => {
    mockedDetail.mockResolvedValue(detail(6, [], {
      id: 9,
      orderId: 1001,
      studentUid: 1,
      workerId: 11,
      score: 4,
      content: '师傅很认真',
      createTime: '2026-09-20 12:10:00',
      updateTime: '2026-09-20 12:10:00',
    }))
    const { wrapper } = await mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已评价 4 星')
    expect(wrapper.find('.star').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.textarea').attributes('disabled')).toBeDefined()
  })
})
