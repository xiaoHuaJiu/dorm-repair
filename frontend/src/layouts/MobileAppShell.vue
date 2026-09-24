<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ROLE_HOME, roleLabelOf } from '@/constants/role'
import { useAppStore } from '@/stores/app'

export interface MobileNavItem {
  label: string
  to: string
}

const props = defineProps<{
  /** 当前端：学生或维修人员。 */
  role: 1 | 2
  title: string
  subtitle?: string
  /** 底部导航条目；未注册的路由自动隐藏，页面接入后自动出现。 */
  navItems: MobileNavItem[]
}>()

defineSlots<{
  default(): unknown
  /** 顶栏右侧操作区之前的内容，例如维修端消息入口。 */
  'before-actions'(): unknown
  /** 页面标题右侧操作，例如学生端“我要报修”。 */
  'head-action'(): unknown
}>()

const router = useRouter()
const route = useRoute()
const app = useAppStore()

const homePath = computed(() => ROLE_HOME[props.role])

const visibleNavItems = computed(() =>
  props.navItems.filter((item) => {
    const resolved = router.resolve(item.to)
    return resolved.name !== 'not-found'
  }),
)

function isActive(to: string): boolean {
  return route.path === to || route.path.startsWith(`${to}/`)
}

function logout() {
  app.clearLoginState()
  void router.push({ name: 'login' })
}
</script>

<template>
  <div class="app">
    <header class="topbar">
      <a class="brand" :href="homePath">
        <span class="brand-mark" aria-hidden="true">修</span>
        <span>宿修通</span>
      </a>
      <div class="top-actions">
        <slot name="before-actions" />
        <span class="avatar" aria-hidden="true">{{ app.currentUser?.realName?.slice(0, 1) || '用' }}</span>
        <span class="user-copy">
          <strong>{{ app.currentUser?.realName || '访客' }}</strong>· {{ roleLabelOf(role) }}
        </span>
        <button class="btn small" type="button" @click="logout">退出</button>
      </div>
    </header>

    <div class="mobile-shell">
      <main class="mobile-main">
        <div class="page-head">
          <div>
            <h1>{{ title }}</h1>
            <p v-if="subtitle">{{ subtitle }}</p>
          </div>
          <slot name="head-action" />
        </div>
        <slot />
      </main>
    </div>

    <nav class="bottom-nav" :aria-label="role === 1 ? '学生端导航' : '维修端导航'">
      <RouterLink
        v-for="item in visibleNavItems"
        :key="item.to"
        :to="item.to"
        :class="{ active: isActive(item.to) }"
      >
        {{ item.label }}
      </RouterLink>
    </nav>
  </div>
</template>
