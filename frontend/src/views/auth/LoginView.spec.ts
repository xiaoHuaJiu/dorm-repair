import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory } from 'vue-router'
import { ApiError } from '@/api/ApiError'
import { login, registerStudent } from '@/api/auth'
import { createAppRouter } from '@/router'
import { useAppStore } from '@/stores/app'
import LoginView from './LoginView.vue'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  registerStudent: vi.fn(),
}))

const mockedLogin = vi.mocked(login)
const mockedRegister = vi.mocked(registerStudent)

async function mountView() {
  setActivePinia(createPinia())
  const router = createAppRouter(createMemoryHistory())
  const wrapper = mount(LoginView, {
    global: { plugins: [router] },
  })
  // 等待安装触发的初始导航完成，避免与登录后的跳转产生竞争。
  await router.isReady()
  return { router, wrapper }
}

async function fillLogin(wrapper: VueWrapper, username: string, password: string) {
  await wrapper.find('#username').setValue(username)
  await wrapper.find('#password').setValue(password)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('登录页', () => {
  beforeEach(() => {
    localStorage.clear()
    mockedLogin.mockReset()
    mockedRegister.mockReset()
  })

  it('渲染登录表单和注册标签', async () => {
    const { wrapper } = await mountView()
    expect(wrapper.find('#login-tab').text()).toBe('登录')
    expect(wrapper.find('#register-tab').text()).toBe('学生注册')
    expect(wrapper.find('#login-view').isVisible()).toBe(true)
    expect(wrapper.find('#register-view').isVisible()).toBe(false)
  })

  it('空账号或密码提交时显示校验错误且不调用接口', async () => {
    const { wrapper } = await mountView()
    await fillLogin(wrapper, '', '')

    expect(wrapper.find('#login-error').text()).toBe('请输入账号')
    expect(mockedLogin).not.toHaveBeenCalled()
  })

  it('登录成功保存身份并跳转到角色首页', async () => {
    mockedLogin.mockResolvedValue({
      token: 'jwt-token',
      user: { userId: 1, username: 'student1', realName: '林同学', roleType: 1 },
    })
    const { router, wrapper } = await mountView()

    await fillLogin(wrapper, 'student1', 'secret123')

    expect(mockedLogin).toHaveBeenCalledWith({ username: 'student1', password: 'secret123' })
    expect(useAppStore().currentUser?.username).toBe('student1')
    // 角色首页首次懒加载耗时较长，放宽轮询等待时间。
    await vi.waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/student/home')
    }, { timeout: 10000 })
  })

  it('登录失败时在表单内显示错误', async () => {
    mockedLogin.mockRejectedValue(new ApiError('账号或密码错误', 401))
    const { wrapper } = await mountView()

    await fillLogin(wrapper, 'student1', 'wrong-password')

    expect(wrapper.find('#login-error').text()).toBe('账号或密码错误')
    expect(useAppStore().isLoggedIn).toBe(false)
  })

  it('登录角色未知时提示联系管理员', async () => {
    mockedLogin.mockResolvedValue({
      token: 'jwt-token',
      user: { userId: 9, username: 'ghost', realName: '未知', roleType: 99 },
    })
    const { wrapper } = await mountView()

    await fillLogin(wrapper, 'ghost', 'secret123')

    expect(wrapper.find('#login-error').text()).toBe('账号角色未知，请联系管理员')
  })

  it('可以切换到学生注册表单', async () => {
    const { wrapper } = await mountView()
    await wrapper.find('#register-tab').trigger('click')

    expect(wrapper.find('#register-view').isVisible()).toBe(true)
    expect(wrapper.find('#login-view').isVisible()).toBe(false)
  })

  it('注册密码与确认密码不一致时提示校验错误', async () => {
    const { wrapper } = await mountView()
    await wrapper.find('#register-tab').trigger('click')

    await wrapper.find('#register-username').setValue('student2')
    await wrapper.find('#register-password').setValue('password1')
    await wrapper.find('#register-confirm-password').setValue('password2')
    await wrapper.find('#register-real-name').setValue('李同学')
    await wrapper.find('#register-phone').setValue('13800138000')
    await wrapper.find('#register-view form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('#register-error').text()).toBe('两次输入的密码不一致')
    expect(mockedRegister).not.toHaveBeenCalled()
  })

  it('注册成功切回登录表单并显示成功提示', async () => {
    mockedRegister.mockResolvedValue(null)
    const { wrapper } = await mountView()
    await wrapper.find('#register-tab').trigger('click')

    await wrapper.find('#register-username').setValue('student2')
    await wrapper.find('#register-password').setValue('password1')
    await wrapper.find('#register-confirm-password').setValue('password1')
    await wrapper.find('#register-real-name').setValue('李同学')
    await wrapper.find('#register-phone').setValue('13800138000')
    await wrapper.find('#register-view form').trigger('submit')
    await flushPromises()

    expect(mockedRegister).toHaveBeenCalledWith({
      username: 'student2',
      password: 'password1',
      confirmPassword: 'password1',
      realName: '李同学',
      phone: '13800138000',
    })
    expect(wrapper.find('#login-view').isVisible()).toBe(true)
    expect(wrapper.find('.notice.green').text()).toBe('注册成功，请使用新账号登录')
    // 注册密码不保留。
    expect((wrapper.find('#register-password').element as HTMLInputElement).value).toBe('')
  })

  it('注册失败时在表单内显示错误', async () => {
    mockedRegister.mockRejectedValue(new ApiError('用户名已存在', 409))
    const { wrapper } = await mountView()
    await wrapper.find('#register-tab').trigger('click')

    await wrapper.find('#register-username').setValue('student2')
    await wrapper.find('#register-password').setValue('password1')
    await wrapper.find('#register-confirm-password').setValue('password1')
    await wrapper.find('#register-real-name').setValue('李同学')
    await wrapper.find('#register-phone').setValue('13800138000')
    await wrapper.find('#register-view form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('#register-error').text()).toBe('用户名已存在')
  })
})
