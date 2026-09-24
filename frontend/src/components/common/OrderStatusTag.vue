<script setup lang="ts">
import { computed } from 'vue'
import { orderStatusLabel, orderStatusTone } from '@/constants/order'

const props = defineProps<{
  /** 工单状态数值，与后端 `RepairOrderStatusEnum` 一致。 */
  status: number
  /** 后端返回的状态文案；未提供时按状态数值映射。 */
  statusName?: string | null
}>()

const label = computed(() => props.statusName ?? orderStatusLabel(props.status))
const tone = computed(() => orderStatusTone(props.status))
</script>

<template>
  <span class="tag" :class="tone">{{ label }}</span>
</template>
