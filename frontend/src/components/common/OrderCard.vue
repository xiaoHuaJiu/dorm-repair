<script setup lang="ts">
import type { OrderListItem } from '@/types/order'
import OrderStatusTag from './OrderStatusTag.vue'

defineProps<{
  order: OrderListItem
  /** 列表用于维修端全校工单时展示负责人。 */
  showOwner?: boolean
}>()

const emit = defineEmits<{
  open: [order: OrderListItem]
}>()

function formatDeadline(order: OrderListItem): string {
  return order.completeDeadline ?? order.acceptDeadline ?? '待确定期限'
}
</script>

<template>
  <article
    class="card hover order-card student-order-card clickable-order"
    tabindex="0"
    role="link"
    :aria-label="`查看工单 ${order.orderNo} ${order.faultTypeName}`"
    @click="emit('open', order)"
    @keydown.enter.prevent="emit('open', order)"
    @keydown.space.prevent="emit('open', order)"
  >
    <div>
      <div class="order-id">{{ order.orderNo }}</div>
      <div class="order-title">{{ order.faultTypeName }}</div>
      <div class="meta">
        <span>{{ order.locationText }}</span>
        <span v-if="showOwner">负责人：{{ order.workerName || '待分配' }}</span>
        <span>{{ formatDeadline(order) }}</span>
      </div>
      <div class="order-tags">
        <OrderStatusTag :status="order.status" :status-name="order.statusName" />
      </div>
    </div>
    <div class="order-actions">
      <slot name="actions" />
    </div>
  </article>
</template>

<style scoped>
.order-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.order-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 8px;
}

@media (max-width: 600px) {
  .order-actions {
    flex-direction: row;
    align-items: flex-start;
  }
}
</style>
