<script setup lang="ts">
import MobileAppShell from '@/layouts/MobileAppShell.vue'

const navItems = [
  { label: '工作台', to: '/worker/home' },
  { label: '我的工单', to: '/worker/orders' },
  { label: '请假', to: '/worker/leave' },
]
</script>

<template>
  <MobileAppShell :role="2" title="消息中心" subtitle="派单、审批和返工提醒集中在这里" :nav-items="navItems">
    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>暂无消息</p>
      <p class="muted">消息中心接口待提供，待后端完成后即可查看未读提醒并跳转对应业务页面。</p>
    </div>
  </MobileAppShell>
</template>
