interface RuntimeEnv {
  VITE_API_BASE_URL?: string
  VITE_REQUEST_TIMEOUT_MS?: string
}

export interface RuntimeConfig {
  apiBaseUrl: string
  requestTimeoutMs: number
}

const DEFAULT_API_BASE_URL = '/api'
const DEFAULT_REQUEST_TIMEOUT_MS = 10_000

export function parseRuntimeConfig(env: RuntimeEnv): RuntimeConfig {
  const rawBaseUrl = env.VITE_API_BASE_URL?.trim()
  const apiBaseUrl = rawBaseUrl ? rawBaseUrl.replace(/\/+$/, '') : DEFAULT_API_BASE_URL
  const rawTimeout = Number(env.VITE_REQUEST_TIMEOUT_MS)
  const requestTimeoutMs = Number.isInteger(rawTimeout) && rawTimeout > 0
    ? rawTimeout
    : DEFAULT_REQUEST_TIMEOUT_MS

  return { apiBaseUrl, requestTimeoutMs }
}

export const runtimeConfig = parseRuntimeConfig(import.meta.env)
