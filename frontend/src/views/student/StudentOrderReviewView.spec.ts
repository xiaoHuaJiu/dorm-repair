import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import { studentOrderDetail } from '@/api/order'
import type { OrderDetail } from '@/types/order'
import StudentOrderReviewView from './StudentOrderReviewView.vue'

vi.mock('@/api/order', () => ({
  studentOrderDetail: vi.fn(),
}))

const mockedDetail = vi.mocked(studentOrderDetail)

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
  })

  it('展示最新一条提交维修结果说明', async () => {
    mockedDetail.mockResolvedValue(detail(3, [
      { id: 1, orderId: 1001, workerId: 11, recordType: 1, content: '检查中', imageUrls: null, interruptReasonType: null, recordTime: '2026-09-20 10:00:00', createTime: '2026-09-20 10:00:00' },
      { id: 2, orderId: 1001, workerId: 11, recordType: 4, content: '已更换密封圈，不再渗水。', imageUrls: null, interruptReasonType: null, recordTime: '2026-09-20 11:30:00', createTime: '2026-09-20 11:30:00' },
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

  it('待确认状态：确认与返工按钮禁用并提示接口待后端提供', async () => {
    mockedDetail.mockResolvedValue(detail(3))
    const { wrapper } = await mountView()
    await flushPromises()

    const confirmButton = wrapper.find('.inline-actions .btn.primary')
    const reworkButton = wrapper.find('.inline-actions .btn.danger')
    expect(confirmButton.exists()).toBe(true)
    expect(confirmButton.attributes('disabled')).toBeDefined()
    expect(confirmButton.attributes('title')).toBe('确认完成接口待后端提供')
    expect(reworkButton.attributes('disabled')).toBeDefined()
    expect(reworkButton.attributes('title')).toBe('返工申请接口待后端提供')
    expect(wrapper.find('.stars').exists()).toBe(false)
  })

  it('已完成状态展示评价区，星标可选，提交评价禁用', async () => {
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
    expect(submitRating.text()).toBe('提交评价')
    expect(submitRating.attributes('disabled')).toBeDefined()
    expect(submitRating.attributes('title')).toBe('评价接口待后端提供')
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
