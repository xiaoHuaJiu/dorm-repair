<script setup lang="ts">
import { computed } from 'vue'
import type { OrderFlowItem } from '@/types/order'
import { orderOperationLabel, orderStatusLabel } from '@/constants/order'

const props = withDefaults(defineProps<{
  /** 工单生命周期流转记录；按接口返回顺序传入，组件按最新在前倒序展示。 */
  flows: OrderFlowItem[]
  /** 负责人姓名映射；未提供时回退展示编号。 */
  assigneeNameOf?: (id: number | null) => string
  /** 操作人姓名映射；未提供时仅展示操作类型，不伪造操作人。 */
  operatorNameOf?: (item: OrderFlowItem) => string | null
  emptyText?: string
}>(), {
  assigneeNameOf: undefined,
  operatorNameOf: undefined,
  emptyText: '暂无流转记录',
})

const items = computed(() => [...props.flows].reverse())

function statusText(status: number | null): string {
  return status === null ? '' : orderStatusLabel(status)
}

function stateChangeText(item: OrderFlowItem): string {
  const from = statusText(item.fromStatus)
  const to = statusText(item.toStatus)
  if (from && to) return `${from} → ${to}`
  return to || ''
}

function assignmentText(item: OrderFlowItem): string {
  const nameOf = props.assigneeNameOf ?? ((id: number | null) => (id === null ? '待分配' : String(id)))
  return `${nameOf(item.originalAssigneeId)} → ${nameOf(item.newAssigneeId)}`
}

function titleText(item: OrderFlowItem): string {
  const operator = props.operatorNameOf?.(item)
  return operator ? `${orderOperationLabel(item.operationType)} · ${operator}` : orderOperationLabel(item.operationType)
}
</script>

<template>
  <div v-if="items.length" class="timeline lifecycle-timeline">
    <div v-for="item in items" :key="item.id" class="timeline-item">
      <i class="timeline-dot" aria-hidden="true"></i>
      <div class="timeline-time">{{ item.operationTime }}</div>
      <div class="timeline-title">{{ titleText(item) }}</div>
      <div v-if="stateChangeText(item)" class="timeline-state">{{ stateChangeText(item) }}</div>
      <div v-if="item.reason" class="muted">{{ item.reason }}</div>
      <div
        v-if="item.originalAssigneeId !== null && item.newAssigneeId !== null && item.originalAssigneeId !== item.newAssigneeId"
        class="timeline-assignment"
      >
        负责人：{{ assignmentText(item) }}
      </div>
    </div>
  </div>

  <div v-else class="empty">
    <div class="empty-mark" aria-hidden="true">○</div>
    <p>{{ emptyText }}</p>
  </div>
</template>

<style scoped>
.timeline-state {
  display: inline-flex;
  margin: 5px 0;
  padding: 2px 8px;
  border-radius: 7px;
  background: #f5f6f7;
  color: #4c5158;
  font-size: 12px;
  font-weight: 700;
}

.timeline-assignment {
  margin-top: 4px;
  color: var(--dr-color-primary-active);
  font-size: 12px;
}
</style>
