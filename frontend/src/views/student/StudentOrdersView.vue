<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import OrderCard from '@/components/common/OrderCard.vue'
import PageState from '@/components/common/PageState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import { pageStudentOrders } from '@/api/order'
import { ORDER_STATUS, ORDER_STATUS_LABEL, isStudentCancelable } from '@/constants/order'
import type { OrderListItem } from '@/types/order'

const navItems = [
  { label: '首页', to: '/student/home' },
  { label: '我的工单', to: '/student/orders' },
]

const router = useRouter()

/** 筛选值：-1 表示全部。 */
const statusFilter = ref<number>(-1)

const statusOptions = [
  { label: '全部', value: -1 },
  ...[
    ORDER_STATUS.PENDING_DISPATCH,
    ORDER_STATUS.PENDING_ACCEPTANCE,
    ORDER_STATUS.REPAIRING,
    ORDER_STATUS.PENDING_CONFIRMATION,
    ORDER_STATUS.REWORKING,
    ORDER_STATUS.INTERRUPTED,
    ORDER_STATUS.COMPLETED,
    ORDER_STATUS.CANCELLED,
  ].map((status) => ({ label: ORDER_STATUS_LABEL[status], value: status })),
]

const loading = ref(true)
const error = ref<string | null>(null)
const records = ref<OrderListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

async function load() {
  loading.value = true
  error.value = null
  try {
    const query = statusFilter.value === -1 ? {} : { statusList: [statusFilter.value] }
    const page = await pageStudentOrders({ pageNum: pageNum.value, pageSize, ...query })
    records.value = page.records
    total.value = page.total
  } catch (cause) {
    records.value = []
    total.value = 0
    error.value = cause instanceof Error ? cause.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function onFilterChange(value: string | number) {
  statusFilter.value = value as number
  pageNum.value = 1
  void load()
}

function onPageChange(value: number) {
  pageNum.value = value
  void load()
}

function openDetail(order: OrderListItem) {
  void router.push({ name: 'student-order-detail', params: { id: order.orderId } })
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="1" title="我的工单" subtitle="查看本人发起的全部报修" :nav-items="navItems">
    <FilterBar v-model="statusFilter" :options="statusOptions" label="按处理状态筛选" @update:model-value="onFilterChange" />

    <div class="order-list-state">
      <PageState
        :loading="loading"
        :error="error"
        :empty="records.length === 0"
        empty-text="当前状态下没有工单"
        @retry="load"
      >
        <div class="grid">
          <OrderCard v-for="order in records" :key="order.orderId" :order="order" @open="openDetail">
            <template #actions>
              <button
                v-if="isStudentCancelable(order.status)"
                class="btn danger small"
                type="button"
                disabled
                title="后端取消工单接口待提供"
                @click.stop
              >
                取消工单
              </button>
            </template>
          </OrderCard>
        </div>
        <PaginationBar :total="total" :page-num="pageNum" :page-size="pageSize" @update:page-num="onPageChange" />
      </PageState>
    </div>
  </MobileAppShell>
</template>

<style scoped>
.order-list-state {
  margin-top: 16px;
}
</style>
