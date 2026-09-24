<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  title: string
  confirmText?: string
  cancelText?: string
  /** 提交请求进行中。 */
  loading?: boolean
  /** 表单内错误文案，例如校验失败或接口错误。 */
  error?: string
}>(), {
  confirmText: '保存',
  cancelText: '取消',
  loading: false,
  error: '',
})

defineSlots<{
  default(): unknown
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  submit: []
  cancel: []
}>()

function close() {
  if (!props.loading) emit('update:visible', false)
}

/** 取消按钮：既通知父组件，也负责关闭弹窗。 */
function cancel() {
  emit('cancel')
  close()
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
      <slot />
      <div v-if="error" class="error" role="alert">{{ error }}</div>
      <div class="inline-actions modal-actions">
        <button class="btn" type="button" :disabled="loading" @click="cancel">{{ cancelText }}</button>
        <button class="btn primary" type="button" :disabled="loading" @click="emit('submit')">
          {{ loading ? '提交中…' : confirmText }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.error {
  margin-top: 12px;
}

.modal-actions {
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
