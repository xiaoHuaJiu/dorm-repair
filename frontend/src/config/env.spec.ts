import { describe, expect, it } from 'vitest'
import { parseRuntimeConfig } from './env'

describe('运行环境配置', () => {
  it('清理接口地址末尾斜杠并读取正整数超时', () => {
    expect(parseRuntimeConfig({
      VITE_API_BASE_URL: ' https://example.test/api/ ',
      VITE_REQUEST_TIMEOUT_MS: '15000',
    })).toEqual({ apiBaseUrl: 'https://example.test/api', requestTimeoutMs: 15_000 })
  })

  it('空值和非法超时使用安全默认值', () => {
    expect(parseRuntimeConfig({
      VITE_API_BASE_URL: '',
      VITE_REQUEST_TIMEOUT_MS: '-1',
    })).toEqual({ apiBaseUrl: '/api', requestTimeoutMs: 10_000 })
  })
})
