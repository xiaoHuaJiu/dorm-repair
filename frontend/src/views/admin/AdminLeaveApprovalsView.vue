<script setup lang="ts">
import AdminAppShell from '@/layouts/AdminAppShell.vue'
</script>

<template>
  <AdminAppShell title="请假审批" subtitle="审核维修人员的请假申请，批准后请假期间停止派单">
    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>暂无请假申请</p>
      <p class="muted">请假审批接口待提供，待后端完成后即可查看申请并审批。</p>
    </div>
  </AdminAppShell>
</template>
