<script setup lang="ts">
import MobileAppShell from '@/layouts/MobileAppShell.vue'

const navItems = [
  { label: '首页', to: '/student/home' },
  { label: '我的工单', to: '/student/orders' },
]
</script>

<template>
  <MobileAppShell :role="1" title="首页" subtitle="查看本人报修进度与全校维修动态" :nav-items="navItems">
    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>学生端首页将在 F6 阶段实现：本人进行中工单、我要报修和全校工单列表。</p>
    </div>
  </MobileAppShell>
</template>
