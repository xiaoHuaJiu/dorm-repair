import { describe, expect, it } from 'vitest'
import {
  ORDER_OPERATION,
  ORDER_OPERATION_LABEL,
  ORDER_STATUS,
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TONE,
  isOrderStatus,
  orderOperationLabel,
  orderStatusLabel,
  orderStatusTone,
} from './order'

describe('工单状态枚举', () => {
  it('状态数值与后端 RepairOrderStatusEnum 一致', () => {
    expect(ORDER_STATUS).toEqual({
      PENDING_DISPATCH: 0,
      PENDING_ACCEPTANCE: 1,
      REPAIRING: 2,
      PENDING_CONFIRMATION: 3,
      REWORKING: 4,
      INTERRUPTED: 5,
      COMPLETED: 6,
      CANCELLED: 7,
    })
  })

  it('八个状态都有文案和色调', () => {
    expect(Object.keys(ORDER_STATUS_LABEL)).toHaveLength(8)
    expect(Object.keys(ORDER_STATUS_TONE)).toHaveLength(8)
  })

  it('未知状态数值回退默认文案和灰色调', () => {
    expect(orderStatusLabel(99)).toBe('未知状态')
    expect(orderStatusLabel(99, '其他')).toBe('其他')
    expect(orderStatusTone(99)).toBe('gray')
    expect(isOrderStatus(99)).toBe(false)
    expect(isOrderStatus(3)).toBe(true)
  })
})

describe('工单操作类型', () => {
  it('操作类型数值与后端 RepairOrderOperationTypeEnum 一致', () => {
    expect(ORDER_OPERATION).toEqual({
      CREATE: 1,
      AUTO_DISPATCH: 2,
      ACCEPT: 3,
      INTERRUPT: 4,
      RESUME: 5,
      SUBMIT_RESULT: 6,
      STUDENT_CONFIRM: 7,
      REWORK: 8,
    })
  })

  it('每个操作类型都有文案，未知操作类型回退', () => {
    expect(ORDER_OPERATION_LABEL[ORDER_OPERATION.ACCEPT]).toBe('接单')
    expect(orderOperationLabel(ORDER_OPERATION.SUBMIT_RESULT)).toBe('提交维修结果')
    expect(orderOperationLabel(ORDER_OPERATION.STUDENT_CONFIRM)).toBe('确认维修完成')
    expect(orderOperationLabel(ORDER_OPERATION.REWORK)).toBe('申请返工')
    expect(orderOperationLabel(99)).toBe('状态更新')
  })
})
