<script setup lang="ts">
import MobileAppShell from '@/layouts/MobileAppShell.vue'

const navItems = [
  { label: '工作台', to: '/worker/home' },
  { label: '我的工单', to: '/worker/orders' },
  { label: '请假', to: '/worker/leave' },
]
</script>

<template>
  <MobileAppShell :role="2" title="今日工作台" subtitle="优先处理待接单与异常任务" :nav-items="navItems">
    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>维修人员工作台将在 F7 阶段实现：负责工单、系统派单、异常工单和全校工单。</p>
    </div>
  </MobileAppShell>
</template>
