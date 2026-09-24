<script setup lang="ts">
import AdminAppShell from '@/layouts/AdminAppShell.vue'
</script>

<template>
  <AdminAppShell title="转派审批" subtitle="审核维修人员的转派申请，同意后工单转移给新负责人">
    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>暂无转派申请</p>
      <p class="muted">转派审批接口待提供，待后端完成后即可查看申请并审批。</p>
    </div>
  </AdminAppShell>
</template>
