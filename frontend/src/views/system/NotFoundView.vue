<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="status-page">
    <section class="status-card">
      <p class="eyebrow">404 · Not Found</p>
      <h1>页面不存在</h1>
      <p class="status-note">访问的地址不存在或已被移除，请检查地址后重试。</p>
      <button class="status-action" type="button" @click="router.push('/')">返回首页</button>
    </section>
  </main>
</template>

<style scoped>
.status-action {
  margin-top: 28px;
  padding: 10px 22px;
  border: 0;
  border-radius: var(--dr-radius-sm);
  background: var(--dr-color-primary);
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}
</style>
