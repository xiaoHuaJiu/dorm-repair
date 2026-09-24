<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ROLE_HOME, roleLabelOf } from '@/constants/role'
import { useAppStore } from '@/stores/app'

export interface AdminNavItem {
  label: string
  to: string
}

export interface AdminNavGroup {
  label: string
  items: AdminNavItem[]
}

const props = defineProps<{
  title: string
  subtitle?: string
}>()

defineSlots<{
  default(): unknown
  /** 页面标题右侧操作，例如“新增维修人员”。 */
  'head-action'(): unknown
}>()

/** 管理端导航基线，对应原型的侧栏分组；未注册路由自动隐藏。 */
const NAV_GROUPS: AdminNavGroup[] = [
  {
    label: '工作台',
    items: [{ label: '工作台', to: '/admin/dashboard' }],
  },
  {
    label: '基础维护',
    items: [
      { label: '维修人员', to: '/admin/config/workers' },
      { label: '区域配置', to: '/admin/config/areas' },
      { label: '工作时间配置', to: '/admin/config/schedules' },
      { label: '故障类型管理', to: '/admin/config/fault-types' },
    ],
  },
  {
    label: '工单管理',
    items: [
      { label: '全部工单', to: '/admin/orders' },
      { label: '异常工单', to: '/admin/orders/exceptions' },
      { label: '转派审批', to: '/admin/approvals/transfers' },
      { label: '待人工派单', to: '/admin/orders/manual-dispatch' },
      { label: '请假审批', to: '/admin/approvals/leaves' },
    ],
  },
]

const router = useRouter()
const route = useRoute()
const app = useAppStore()

const visibleGroups = computed(() =>
  NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => router.resolve(item.to).name !== 'not-found'),
  })).filter((group) => group.items.length > 0),
)

function isActive(to: string): boolean {
  return route.path === to || route.path.startsWith(`${to}/`)
}

/** 面包屑：当前路由所属导航组 + 页面标题，相邻重复时只保留一个。 */
const breadcrumbs = computed(() => {
  const group = NAV_GROUPS.find((item) => item.items.some((link) => isActive(link.to)))
  const parts = [group?.label, props.title].filter((part): part is string => Boolean(part))
  return parts.filter((part, index) => index === 0 || part !== parts[index - 1])
})

function logout() {
  app.clearLoginState()
  void router.push({ name: 'login' })
}
</script>

<template>
  <div class="app">
    <header class="topbar">
      <a class="brand" :href="ROLE_HOME[3]">
        <span class="brand-mark" aria-hidden="true">修</span>
        <span>宿修通</span>
      </a>
      <div class="top-actions">
        <span class="avatar" aria-hidden="true">{{ app.currentUser?.realName?.slice(0, 1) || '用' }}</span>
        <span class="user-copy">
          <strong>{{ app.currentUser?.realName || '访客' }}</strong>· {{ roleLabelOf(3) }}
        </span>
        <button class="btn small" type="button" @click="logout">退出</button>
      </div>
    </header>

    <div class="admin-shell">
      <aside class="sidebar admin-sidebar">
        <nav aria-label="管理端导航">
          <section v-for="group in visibleGroups" :key="group.label" class="nav-section">
            <h2>{{ group.label }}</h2>
            <RouterLink
              v-for="item in group.items"
              :key="item.to"
              :to="item.to"
              class="nav-link"
              :class="{ active: isActive(item.to) }"
            >
              {{ item.label }}
            </RouterLink>
          </section>
        </nav>
      </aside>

      <main class="admin-main">
        <nav v-if="breadcrumbs.length" class="breadcrumb" aria-label="面包屑">
          <template v-for="(crumb, index) in breadcrumbs" :key="crumb">
            <span v-if="index > 0" class="breadcrumb-sep" aria-hidden="true">/</span>
            <span :class="{ 'breadcrumb-current': index === breadcrumbs.length - 1 }">{{ crumb }}</span>
          </template>
        </nav>
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
  </div>
</template>
