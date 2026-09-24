<script setup lang="ts">
import { computed } from 'vue'
import type { OrderListItem } from '@/types/order'
import OrderStatusTag from './OrderStatusTag.vue'

const props = defineProps<{
  order: OrderListItem
}>()

const emit = defineEmits<{
  open: [order: OrderListItem]
}>()

/** 异常与超时标签，与原型 flags 对应。 */
const flags = computed(() => {
  const list: string[] = []
  if (props.order.acceptTimeout) list.push('接单超时')
  if (props.order.completeTimeout) list.push('完成超时')
  if (props.order.duplicateFlag) list.push('疑似重复')
  if (props.order.exceptionFlag) list.push('异常')
  return list
})

function onRowClick(event: MouseEvent) {
  const target = event.target as Element | null
  // 行内交互元素不触发整行跳转，避免与操作按钮冲突。
  if (target?.closest('button, a, input, select, textarea, label')) return
  emit('open', props.order)
}

function onRowKeydown(event: KeyboardEvent) {
  const target = event.target as Element | null
  if (target?.closest('button, a, input, select, textarea, label')) return
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    emit('open', props.order)
  }
}
</script>

<template>
  <tr
    class="clickable-order"
    tabindex="0"
    role="link"
    :aria-label="`查看工单 ${order.orderNo} ${order.faultTypeName}`"
    @click="onRowClick"
    @keydown="onRowKeydown"
  >
    <td>
      <strong>{{ order.orderNo }}</strong>
      <br />
      {{ order.faultTypeName }}
    </td>
    <td>{{ order.locationText }}</td>
    <td><OrderStatusTag :status="order.status" :status-name="order.statusName" /></td>
    <td>{{ order.workerName || '待分配' }}</td>
    <td>
      <template v-if="flags.length">
        <span v-for="flag in flags" :key="flag" class="tag red">{{ flag }}</span>
      </template>
      <template v-else>—</template>
    </td>
    <td v-if="$slots.actions">
      <div class="row-actions"><slot name="actions" /></div>
    </td>
  </tr>
</template>

<style scoped>
.row-actions {
  display: inline-flex;
  gap: 8px;
}
</style>
