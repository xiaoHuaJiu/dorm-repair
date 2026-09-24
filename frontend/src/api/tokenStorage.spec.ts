import { beforeEach, describe, expect, it } from 'vitest'
import { clearToken, getToken, setToken } from './tokenStorage'

describe('令牌存储', () => {
  beforeEach(() => localStorage.clear())
  it('集中保存、读取和清除令牌', () => {
    expect(getToken()).toBeNull()
    setToken('jwt'); expect(getToken()).toBe('jwt')
    clearToken(); expect(getToken()).toBeNull()
  })
})
