import { describe, expect, it } from 'vitest'
import { ROLE } from '@/constants/role'
import { ORDER_STATUS } from '@/constants/order'
import { computeOrderPermissions } from './orderPermission'

describe('工单操作权限计算', () => {
  describe('学生', () => {
    it('待派单、待接单、维修中、已中断、返工中允许取消', () => {
      for (const status of [
        ORDER_STATUS.PENDING_DISPATCH,
        ORDER_STATUS.PENDING_ACCEPTANCE,
        ORDER_STATUS.REPAIRING,
        ORDER_STATUS.INTERRUPTED,
        ORDER_STATUS.REWORKING,
      ]) {
        const permissions = computeOrderPermissions(ROLE.STUDENT, status)
        expect(permissions.cancel).toBe(true)
        expect(permissions.readonly).toBe(false)
      }
    })

    it('待确认允许确认与申请返工，不允许取消', () => {
      const permissions = computeOrderPermissions(ROLE.STUDENT, ORDER_STATUS.PENDING_CONFIRMATION)
      expect(permissions.confirm).toBe(true)
      expect(permissions.requestRework).toBe(true)
      expect(permissions.cancel).toBe(false)
      expect(permissions.readonly).toBe(false)
    })

    it('已完成、已取消只读', () => {
      for (const status of [ORDER_STATUS.COMPLETED, ORDER_STATUS.CANCELLED]) {
        const permissions = computeOrderPermissions(ROLE.STUDENT, status)
        expect(permissions).toMatchObject({
          cancel: false,
          confirm: false,
          requestRework: false,
          readonly: true,
        })
      }
    })
  })

  describe('维修人员', () => {
    it('非负责人一律只读', () => {
      for (let status = 0; status <= 7; status += 1) {
        expect(computeOrderPermissions(ROLE.WORKER, status, false).readonly).toBe(true)
      }
    })

    it('待接单允许接单与转派', () => {
      const permissions = computeOrderPermissions(ROLE.WORKER, ORDER_STATUS.PENDING_ACCEPTANCE, true)
      expect(permissions.accept).toBe(true)
      expect(permissions.transfer).toBe(true)
      expect(permissions.addProcessRecord).toBe(false)
      expect(permissions.readonly).toBe(false)
    })

    it('维修中允许过程记录、中断、转派', () => {
      const permissions = computeOrderPermissions(ROLE.WORKER, ORDER_STATUS.REPAIRING, true)
      expect(permissions.addProcessRecord).toBe(true)
      expect(permissions.interrupt).toBe(true)
      expect(permissions.transfer).toBe(true)
      expect(permissions.accept).toBe(false)
    })

    it('已中断允许恢复与转派', () => {
      const permissions = computeOrderPermissions(ROLE.WORKER, ORDER_STATUS.INTERRUPTED, true)
      expect(permissions.resume).toBe(true)
      expect(permissions.transfer).toBe(true)
    })

    it('返工中允许提交维修结果', () => {
      const permissions = computeOrderPermissions(ROLE.WORKER, ORDER_STATUS.REWORKING, true)
      expect(permissions.submitResult).toBe(true)
    })

    it('已完成、已取消只读', () => {
      for (const status of [ORDER_STATUS.COMPLETED, ORDER_STATUS.CANCELLED]) {
        expect(computeOrderPermissions(ROLE.WORKER, status, true).readonly).toBe(true)
      }
    })
  })

  describe('管理员与未知角色', () => {
    it('管理员一律只读（审批与派单操作在 F8 阶段扩展）', () => {
      for (let status = 0; status <= 7; status += 1) {
        const permissions = computeOrderPermissions(ROLE.ADMIN, status)
        expect(permissions.readonly).toBe(true)
        expect(Object.values(permissions).filter(Boolean)).toHaveLength(1)
      }
    })

    it('未知角色只读', () => {
      expect(computeOrderPermissions(99, ORDER_STATUS.REPAIRING).readonly).toBe(true)
    })
  })
})
