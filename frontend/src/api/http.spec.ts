import MockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './ApiError'
import { setErrorNotifier } from './errorNotifier'
import { http, request } from './http'
import { setUnauthorizedHandler } from './unauthorizedHandler'

describe('统一请求', () => {
  let mock: MockAdapter
  const notify = vi.fn()
  const unauthorized = vi.fn()

  beforeEach(() => {
    mock = new MockAdapter(http)
    setErrorNotifier(notify)
    setUnauthorizedHandler(unauthorized)
  })
  afterEach(() => {
    mock.restore()
    notify.mockReset()
    unauthorized.mockReset()
    setErrorNotifier(() => undefined)
    setUnauthorizedHandler(() => undefined)
  })

  it('直接返回成功数据', async () => {
    mock.onGet('/user').reply(200, { code: 0, message: 'success', data: { id: 1 } })
    await expect(request<{ id: number }>({ url: '/user' })).resolves.toEqual({ id: 1 })
    expect(notify).not.toHaveBeenCalled()
  })

  it('数组查询参数按重复参数序列化以匹配后端 List 绑定', () => {
    const uri = http.getUri({
      url: '/student/repair-orders',
      params: { pageNum: 1, pageSize: 50, statusList: [0, 1, 2] },
    })
    expect(uri).toBe('/api/student/repair-orders?pageNum=1&pageSize=50&statusList=0&statusList=1&statusList=2')
  })

  it('为已登录请求附加 Bearer 令牌', async () => {
    localStorage.setItem('dorm-repair-token', 'jwt-token')
    mock.onGet('/authorized').reply((config) => [200, {
      code: 0,
      message: 'success',
      data: { authorization: config.headers?.Authorization },
    }])

    await expect(request<{ authorization: string }>({ url: '/authorized' }))
      .resolves.toEqual({ authorization: 'Bearer jwt-token' })
  })

  it('将业务失败转换为一次提示的 ApiError', async () => {
    mock.onGet('/missing').reply(200, { code: 10004, message: '数据不存在', data: null })
    await expect(request({ url: '/missing' })).rejects.toMatchObject({ code: 10004, message: '数据不存在' })
    expect(notify).toHaveBeenCalledTimes(1)
  })

  it('401 先触发未登录处理再提示一次', async () => {
    mock.onGet('/private').reply(401, { code: 10002, message: '未登录或登录状态已失效', data: null })
    await expect(request({ url: '/private' })).rejects.toBeInstanceOf(ApiError)
    expect(unauthorized).toHaveBeenCalledTimes(1)
    expect(notify).toHaveBeenCalledTimes(1)
    expect(unauthorized.mock.invocationCallOrder[0]).toBeLessThan(notify.mock.invocationCallOrder[0])
  })

  it('403 保留权限错误且不清理登录状态', async () => {
    mock.onGet('/forbidden').reply(403, { code: 10003, message: '无权限访问', data: null })
    await expect(request({ url: '/forbidden' })).rejects.toMatchObject({
      httpStatus: 403,
      code: 10003,
      message: '无权限访问',
    })
    expect(unauthorized).not.toHaveBeenCalled()
  })

  it('保留主动取消请求并且不提示错误', async () => {
    const controller = new AbortController()
    controller.abort()

    await expect(request({ url: '/cancelled', signal: controller.signal }))
      .rejects.toMatchObject({ code: 'ERR_CANCELED' })
    expect(notify).not.toHaveBeenCalled()
  })

  it('使用相同防重键时拒绝仍在处理的重复提交', async () => {
    mock.onPost('/submit').reply(() => new Promise((resolve) => {
      setTimeout(() => resolve([200, { code: 0, message: 'success', data: { saved: true } }]), 10)
    }))

    const first = request<{ saved: boolean }>({ method: 'POST', url: '/submit', dedupeKey: 'repair:create' })
    await expect(request({ method: 'POST', url: '/submit', dedupeKey: 'repair:create' }))
      .rejects.toMatchObject({ message: '请求正在处理中，请勿重复提交' })
    await expect(first).resolves.toEqual({ saved: true })

    await expect(request({ method: 'POST', url: '/submit', dedupeKey: 'repair:create' }))
      .resolves.toEqual({ saved: true })
  })

  it('保留服务端通用错误消息', async () => {
    mock.onGet('/system').reply(500, { code: 50000, message: '系统异常，请稍后重试', data: null })
    await expect(request({ url: '/system' })).rejects.toMatchObject({ httpStatus: 500, code: 50000, message: '系统异常，请稍后重试' })
  })

  it('网络错误和非标准响应使用安全通用消息', async () => {
    mock.onGet('/network').networkError()
    await expect(request({ url: '/network' })).rejects.toMatchObject({ message: '网络请求失败，请稍后重试' })
    mock.onGet('/invalid').reply(500, { internal: 'secret' })
    await expect(request({ url: '/invalid' })).rejects.toMatchObject({ message: '请求失败，请稍后重试' })
    expect(notify).toHaveBeenCalledTimes(2)
  })
})
