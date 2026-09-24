<script setup lang="ts">
export interface FilterOption<T extends string | number> {
  label: string
  value: T
}

defineProps<{
  /** 选中值。 */
  modelValue: string | number
  options: FilterOption<string | number>[]
  /** 筛选栏的可访问名称。 */
  label?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()
</script>

<template>
  <div class="filters" role="group" :aria-label="label ?? '筛选'">
    <button
      v-for="option in options"
      :key="String(option.value)"
      class="filter"
      :class="{ active: modelValue === option.value }"
      type="button"
      :aria-pressed="modelValue === option.value"
      @click="emit('update:modelValue', option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>
