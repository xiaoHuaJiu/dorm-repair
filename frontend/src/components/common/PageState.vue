<script setup lang="ts">
defineProps<{
  /** 首次加载中，展示骨架屏。 */
  loading?: boolean
  /** 加载失败的错误信息；有值时展示错误状态与重试。 */
  error?: string | null
  /** 数据为空时展示空状态。 */
  empty?: boolean
  emptyText?: string
  emptyMark?: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div v-if="loading" class="page-state" aria-busy="true">
    <div class="skeleton-block" style="height: 84px" />
    <div class="skeleton-block" style="height: 84px" />
    <div class="skeleton-block" style="height: 84px" />
  </div>

  <div v-else-if="error" class="empty" role="alert">
    <div class="empty-mark" aria-hidden="true">!</div>
    <p>{{ error }}</p>
    <button class="btn outline small" type="button" @click="emit('retry')">重试</button>
  </div>

  <div v-else-if="empty" class="empty">
    <div class="empty-mark" aria-hidden="true">{{ emptyMark ?? '○' }}</div>
    <p>{{ emptyText ?? '暂无数据' }}</p>
  </div>

  <slot v-else />
</template>

<style scoped>
.page-state {
  display: grid;
  gap: 12px;
}
</style>
