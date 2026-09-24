import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import WorkerMessagesView from './WorkerMessagesView.vue'

async function mountView() {
  setActivePinia(createPinia())
  const app = useAppStore()
  app.currentUser = { userId: 2, username: 'worker01', realName: '张师傅', roleType: 2 }
  const router = createAppRouter(createMemoryHistory())
  await router.push('/worker/messages')
  const wrapper = mount(WorkerMessagesView, { global: { plugins: [router] } })
  return { wrapper }
}

describe('维修人员消息中心', () => {
  it('展示消息中心标题与接口待提供提示', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('消息中心')
    expect(wrapper.text()).toContain('暂无消息')
    expect(wrapper.text()).toContain('消息中心接口待提供')
  })

  it('顶部消息入口可用', async () => {
    const { wrapper } = await mountView()

    const link = wrapper.find('a[aria-label="消息中心"]')
    expect(link.exists()).toBe(true)
  })
})
