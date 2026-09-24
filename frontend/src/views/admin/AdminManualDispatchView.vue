<script setup lang="ts">
import AdminAppShell from '@/layouts/AdminAppShell.vue'
</script>

<template>
  <AdminAppShell title="待人工派单" subtitle="自动派单失败后由管理员人工指定维修人员">
    <div class="empty">
      <div class="empty-mark" aria-hidden="true">○</div>
      <p>暂无待人工派单工单</p>
      <p class="muted">人工派单接口待提供，待后端完成后即可查看候选人员并派单。</p>
    </div>
  </AdminAppShell>
</template>
