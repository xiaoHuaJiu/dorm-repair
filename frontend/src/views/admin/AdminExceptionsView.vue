<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import PageState from '@/components/common/PageState.vue'
import OrderTableRow from '@/components/common/OrderTableRow.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import { pageAdminOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'

const route = useRoute()
const router = useRouter()

/** 异常类型筛选：'all' 全部异常、'timeout' 即将超时。 */
const typeFilter = ref<'all' | 'timeout'>(route.query.type === 'timeout' ? 'timeout' : 'all')

const typeOptions = [
  { label: '全部异常', value: 'all' },
  { label: '即将超时', value: 'timeout' },
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
    // 全部异常按 exceptionFlag 查询；即将超时按 acceptTimeout 查询。
    const query =
      typeFilter.value === 'timeout'
        ? { acceptTimeout: true }
        : { exceptionFlag: true }
    const page = await pageAdminOrders({ pageNum: pageNum.value, pageSize, ...query })
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
  typeFilter.value = value as 'all' | 'timeout'
  pageNum.value = 1
  void load()
}

function onPageChange(value: number) {
  pageNum.value = value
  void load()
}

function openDetail(order: OrderListItem) {
  void router.push({ name: 'admin-order-detail', params: { id: order.orderId } })
}

onMounted(load)
</script>

<template>
  <AdminAppShell title="异常工单" subtitle="集中查看超时、派单失败、转派失败和多次返工">
    <FilterBar v-model="typeFilter" :options="typeOptions" label="按异常类型筛选" @update:model-value="onFilterChange" />

    <div class="exception-state">
      <PageState :loading="loading" :error="error" :empty="records.length === 0" empty-text="当前没有异常工单" @retry="load">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>工单</th>
                <th>区域/位置</th>
                <th>状态</th>
                <th>负责人</th>
                <th>异常</th>
              </tr>
            </thead>
            <tbody>
              <OrderTableRow v-for="order in records" :key="order.orderId" :order="order" @open="openDetail" />
            </tbody>
          </table>
        </div>
        <PaginationBar :total="total" :page-num="pageNum" :page-size="pageSize" @update:page-num="onPageChange" />
      </PageState>
    </div>
  </AdminAppShell>
</template>

<style scoped>
.exception-state {
  margin-top: 16px;
}
</style>
