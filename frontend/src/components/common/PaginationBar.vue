<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** 总记录数。 */
  total: number
  /** 当前页码，从 1 开始。 */
  pageNum: number
  /** 每页条数。 */
  pageSize: number
}>()

const emit = defineEmits<{
  'update:pageNum': [value: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

function go(page: number) {
  if (page < 1 || page > totalPages.value || page === props.pageNum) return
  emit('update:pageNum', page)
}
</script>

<template>
  <div class="pagination-bar">
    <span class="muted">共 {{ total }} 条</span>
    <div class="inline-actions">
      <button class="btn small" type="button" :disabled="pageNum <= 1" @click="go(pageNum - 1)">上一页</button>
      <span class="muted">第 {{ pageNum }} / {{ totalPages }} 页</span>
      <button class="btn small" type="button" :disabled="pageNum >= totalPages" @click="go(pageNum + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 16px;
}

.muted {
  color: var(--dr-color-text-muted);
  font-size: 13px;
}
</style>
