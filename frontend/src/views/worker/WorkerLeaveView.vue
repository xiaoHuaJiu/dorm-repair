<script setup lang="ts">
import MobileAppShell from '@/layouts/MobileAppShell.vue'

const navItems = [
  { label: '工作台', to: '/worker/home' },
  { label: '我的工单', to: '/worker/orders' },
  { label: '请假', to: '/worker/leave' },
]
</script>

<template>
  <MobileAppShell :role="2" title="我的请假" subtitle="审批通过后，到开始时间才会停止派单" :nav-items="navItems">
    <template #head-action>
      <button
        class="btn primary"
        type="button"
        disabled
        title="后端请假申请接口待提供"
        @click.stop
      >
        发起请假申请
      </button>
    </template>

    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>暂无请假申请</p>
      <p class="muted">请假接口待提供，待后端完成后即可发起申请并查看审批状态。</p>
    </div>
  </MobileAppShell>
</template>
