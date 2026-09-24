<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ROLE_HOME, type RoleType } from '@/constants/role'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const app = useAppStore()

const backTarget = computed(() => {
  if (!app.currentUser) return { name: 'login' }
  const roleType = app.currentUser.roleType as RoleType
  return { path: ROLE_HOME[roleType] }
})

const backLabel = computed(() => (app.currentUser ? '返回首页' : '返回登录'))
</script>

<template>
  <main class="status-page">
    <section class="status-card">
      <p class="eyebrow">403 · Forbidden</p>
      <h1>没有访问权限</h1>
      <p class="status-text">当前账号无权访问该页面</p>
      <p class="status-note">学生、维修人员和管理员只能访问各自角色入口。如认为账号权限有误，请联系管理员。</p>
      <button class="status-action" type="button" @click="router.push(backTarget)">{{ backLabel }}</button>
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
