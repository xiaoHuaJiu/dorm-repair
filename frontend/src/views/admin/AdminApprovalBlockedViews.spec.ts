import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import AdminTransferApprovalsView from './AdminTransferApprovalsView.vue'
import AdminManualDispatchView from './AdminManualDispatchView.vue'
import AdminLeaveApprovalsView from './AdminLeaveApprovalsView.vue'

function prepareRouter() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 3, username: 'admin', realName: '管理员', roleType: 3 }
  const router = createAppRouter(createMemoryHistory())
  return router
}

async function mountPage(component: unknown, path: string) {
  const router = prepareRouter()
  await router.push(path)
  const wrapper = mount(component, { global: { plugins: [router] } })
  return wrapper
}

describe('转派审批（接口缺失）', () => {
  beforeEach(() => {})

  it('展示空态与接口待提供提示', async () => {
    const wrapper = await mountPage(AdminTransferApprovalsView, '/admin/approvals/transfers')

    expect(wrapper.text()).toContain('转派审批')
    expect(wrapper.text()).toContain('暂无转派申请')
    expect(wrapper.text()).toContain('转派审批接口待提供')
  })
})

describe('待人工派单（接口缺失）', () => {
  it('展示空态与接口待提供提示', async () => {
    const wrapper = await mountPage(AdminManualDispatchView, '/admin/orders/manual-dispatch')

    expect(wrapper.text()).toContain('待人工派单')
    expect(wrapper.text()).toContain('暂无待人工派单工单')
    expect(wrapper.text()).toContain('人工派单接口待提供')
  })
})

describe('请假审批（接口缺失）', () => {
  it('展示空态与接口待提供提示', async () => {
    const wrapper = await mountPage(AdminLeaveApprovalsView, '/admin/approvals/leaves')

    expect(wrapper.text()).toContain('请假审批')
    expect(wrapper.text()).toContain('暂无请假申请')
    expect(wrapper.text()).toContain('请假审批接口待提供')
  })
})
