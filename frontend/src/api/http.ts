import axios, { type AxiosRequestConfig } from 'axios'
import { ApiError } from './ApiError'
import { notifyError } from './errorNotifier'
import { handleUnauthorized } from './unauthorizedHandler'
import type { Result } from './types'
import { getToken } from './tokenStorage'
import { runtimeConfig } from '../config/env'

export const http = axios.create({
  baseURL: runtimeConfig.apiBaseUrl,
  timeout: runtimeConfig.requestTimeoutMs,
})

export interface RequestConfig extends AxiosRequestConfig {
  /** 业务提交防重键。相同键的请求完成前，后续请求会被拒绝。 */
  dedupeKey?: string
}

const pendingRequestKeys = new Set<string>()

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

function isResult(value: unknown): value is Result<unknown> {
  return typeof value === 'object' && value !== null
    && typeof (value as Result<unknown>).code === 'number'
    && typeof (value as Result<unknown>).message === 'string'
    && 'data' in value
}

async function rejectWith(error: ApiError): Promise<never> {
  if (error.httpStatus === 401 || error.code === 10002) await handleUnauthorized()
  notifyError(error.message)
  throw error
}

export async function request<T>(config: RequestConfig): Promise<T> {
  const { dedupeKey, ...axiosConfig } = config
  if (dedupeKey && pendingRequestKeys.has(dedupeKey)) {
    return rejectWith(new ApiError('请求正在处理中，请勿重复提交'))
  }
  if (dedupeKey) pendingRequestKeys.add(dedupeKey)

  try {
    const response = await http.request<Result<T>>(axiosConfig)
    if (!isResult(response.data)) return rejectWith(new ApiError('请求失败，请稍后重试', response.status))
    if (response.data.code !== 0) {
      return rejectWith(new ApiError(response.data.message, response.status, response.data.code))
    }
    return response.data.data
  } catch (cause) {
    if (cause instanceof ApiError) throw cause
    if (axios.isCancel(cause)) throw cause
    if (axios.isAxiosError(cause)) {
      const status = cause.response?.status
      const body = cause.response?.data
      if (isResult(body)) return rejectWith(new ApiError(body.message, status, body.code))
      const message = cause.response ? '请求失败，请稍后重试' : '网络请求失败，请稍后重试'
      return rejectWith(new ApiError(message, status))
    }
    return rejectWith(new ApiError('请求失败，请稍后重试'))
  } finally {
    if (dedupeKey) pendingRequestKeys.delete(dedupeKey)
  }
}
