<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  title: string
  content?: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
  /** 确认请求进行中，按钮进入禁用状态。 */
  loading?: boolean
}>(), {
  content: '',
  confirmText: '确认',
  cancelText: '取消',
  danger: false,
  loading: false,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: []
  cancel: []
}>()

function close() {
  if (!props.loading) emit('update:visible', false)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.visible) close()
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) window.addEventListener('keydown', onKeydown)
    else window.removeEventListener('keydown', onKeydown)
  },
  { immediate: true },
)

onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="close">
    <section class="modal" role="dialog" aria-modal="true" :aria-label="title">
      <h2>{{ title }}</h2>
      <div v-if="content">
        <p>{{ content }}</p>
      </div>
      <slot />
      <div class="inline-actions modal-actions">
        <button class="btn" type="button" :disabled="loading" @click="emit('cancel')">{{ cancelText }}</button>
        <button
          class="btn"
          :class="danger ? 'danger' : 'primary'"
          type="button"
          :disabled="loading"
          @click="emit('confirm')"
        >
          {{ loading ? '处理中…' : confirmText }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.modal-actions {
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
