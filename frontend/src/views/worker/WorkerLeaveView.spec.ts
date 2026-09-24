import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import WorkerLeaveView from './WorkerLeaveView.vue'

async function mountView() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 2, username: 'worker01', realName: '张师傅', roleType: 2 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/worker/leave')
  const wrapper = mount(WorkerLeaveView, { global: { plugins: [router] } })
  return { wrapper }
}

describe('维修人员请假页', () => {
  it('请假申请按钮因接口缺失禁用并提示', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('我的请假')
    const button = wrapper.find('button[title="后端请假申请接口待提供"]')
    expect(button.exists()).toBe(true)
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toBe('发起请假申请')
  })

  it('展示接口待提供的空态说明', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('暂无请假申请')
    expect(wrapper.text()).toContain('请假接口待提供')
  })
})
