/// <reference types="vitest" />

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 8810,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8811',
    },
  },
  test: {
    environment: 'jsdom',
    // 全量并行运行时的路由跳转类测试可能超过默认 5s，提高阈值避免环境负载导致的偶发超时。
    testTimeout: 15000,
  },
})
