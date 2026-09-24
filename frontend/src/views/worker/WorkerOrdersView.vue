<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import OrderCard from '@/components/common/OrderCard.vue'
import PageState from '@/components/common/PageState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import { pageWorkerOrders } from '@/api/order'
import { WORKER_FILTER_STATUSES } from '@/constants/order'
import type { OrderListItem } from '@/types/order'

const navItems = [
  { label: '工作台', to: '/worker/home' },
  { label: '我的工单', to: '/worker/orders' },
  { label: '请假', to: '/worker/leave' },
]

const router = useRouter()

/** 筛选值：-1 表示全部。 */
const statusFilter = ref<number>(-1)

const statusOptions = [
  { label: '全部', value: -1 },
  ...WORKER_FILTER_STATUSES.map((item) => ({ label: item.label, value: item.value })),
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
    const page = await pageWorkerOrders({ pageNum: pageNum.value, pageSize, ...query })
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
  void router.push({ name: 'worker-order-detail', params: { id: order.orderId } })
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="2" title="我的工单" subtitle="查看当前及历史负责工单" :nav-items="navItems">
    <FilterBar v-model="statusFilter" :options="statusOptions" label="按处理状态筛选" @update:model-value="onFilterChange" />

    <div class="order-list-state">
      <PageState
        :loading="loading"
        :error="error"
        :empty="records.length === 0"
        empty-text="当前状态下没有负责工单"
        @retry="load"
      >
        <div class="grid">
          <OrderCard v-for="order in records" :key="order.orderId" :order="order" @open="openDetail" />
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
