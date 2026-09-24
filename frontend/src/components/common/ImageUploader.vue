<script setup lang="ts">
import { ref } from 'vue'
import type { OrderFile } from '@/types/file'

const props = withDefaults(defineProps<{
  /** 已上传的文件列表（fileId + previewUrl）。 */
  modelValue: OrderFile[]
  /** 最多可上传数量。 */
  max?: number
  /** 上传接口不可用或提交中时禁用选择。 */
  disabled?: boolean
}>(), {
  max: 3,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: OrderFile[]]
  /** 用户选择新文件；页面层上传成功后把结果追加到 modelValue。 */
  'add-files': [files: File[]]
  /** 用户移除文件；页面层调用删除接口成功后更新 modelValue。 */
  'remove-file': [file: OrderFile]
}>()

const input = ref<HTMLInputElement | null>(null)

function onPick() {
  if (!props.disabled) input.value?.click()
}

function onInputChange(event: Event) {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files ?? [])
  target.value = ''
  if (!files.length) return
  const remain = props.max - props.modelValue.length
  if (remain <= 0) return
  emit('add-files', files.slice(0, remain))
}

function onRemove(index: number) {
  if (props.disabled) return
  emit('remove-file', props.modelValue[index])
}
</script>

<template>
  <div class="image-uploader">
    <div v-for="(file, index) in modelValue" :key="file.fileId" class="thumb">
      <img :src="file.previewUrl" alt="" />
      <button
        class="thumb-remove"
        type="button"
        :disabled="disabled"
        :aria-label="`移除第 ${index + 1} 张图片`"
        @click="onRemove(index)"
      >
        ×
      </button>
    </div>

    <button
      v-if="modelValue.length < max"
      class="thumb add"
      type="button"
      :disabled="disabled"
      :aria-label="disabled ? '图片上传暂不可用' : '添加图片'"
      @click="onPick"
    >
      +
    </button>

    <input ref="input" type="file" accept="image/*" multiple hidden @change="onInputChange" />

    <p class="hint">
      <slot name="hint">
        最多 {{ max }} 张，单张不超过 5MB。
      </slot>
    </p>
  </div>
</template>

<style scoped>
.image-uploader {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.thumb {
  position: relative;
  width: 64px;
  height: 64px;
  border: 1px solid var(--dr-color-border);
  border-radius: 10px;
  overflow: hidden;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb.add {
  display: grid;
  border: 1px dashed var(--dr-color-border);
  background: none;
  color: var(--dr-color-text-muted);
  font-size: 24px;
  cursor: pointer;
  place-items: center;
}

.thumb.add:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.thumb-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  display: grid;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgb(0 0 0 / 55%);
  color: #ffffff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  place-items: center;
}

.hint {
  width: 100%;
  margin: 2px 0 0;
  color: var(--dr-color-text-muted);
  font-size: 12px;
}
</style>
