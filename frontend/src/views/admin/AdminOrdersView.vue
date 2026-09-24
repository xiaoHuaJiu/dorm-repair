<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderTableRow from '@/components/common/OrderTableRow.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import { pageAdminOrders } from '@/api/order'
import { pageWorkers } from '@/api/worker'
import { enabledAreaTree } from '@/api/area'
import { ORDER_STATUS, ORDER_STATUS_LABEL } from '@/constants/order'
import type { AreaTreeNode, WorkerDetail } from '@/types/config'
import type { OrderListItem } from '@/types/order'

const router = useRouter()

/** 校区筛选（区域树顶层）。 */
const campusId = ref<number | null>(null)
const campusOptions = ref<{ value: number; label: string }[]>([])
/** 负责人筛选。 */
const workerId = ref<number | null>(null)
const workerOptions = ref<{ value: number | null; label: string }[]>([])
/** 状态筛选：-1 表示全部。 */
const statusFilter = ref<number>(-1)

const statusOptions = [
  { label: '全部状态', value: -1 },
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
    const page = await pageAdminOrders({
      pageNum: pageNum.value,
      pageSize,
      campusId: campusId.value ?? undefined,
      workerId: workerId.value ?? undefined,
      statusList: statusFilter.value === -1 ? undefined : [statusFilter.value],
    })
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

/** 并行加载筛选选项：校区（区域树顶层）与维修人员列表。 */
async function loadOptions() {
  const [tree, workers] = await Promise.all([
    enabledAreaTree(),
    pageWorkers({ pageNum: 1, pageSize: 500 }),
  ])
  campusOptions.value = tree.map((node: AreaTreeNode) => ({ value: node.id, label: node.areaName }))
  workerOptions.value = workers.records.map((worker: WorkerDetail) => ({
    value: worker.workerId,
    label: worker.realName ?? worker.username ?? `维修人员 #${worker.workerId}`,
  }))
}

async function loadAll() {
  try {
    await loadOptions()
  } catch {
    // 筛选选项加载失败不阻塞列表查询，保持选项为空即可。
  }
  await load()
}

function onFilterChange() {
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

onMounted(loadAll)
</script>

<template>
  <AdminAppShell title="全部工单" subtitle="按区域、负责人和状态组合筛选">
    <div class="admin-filter-row">
      <div class="field">
        <label for="filter-campus">区域</label>
        <select id="filter-campus" v-model="campusId" class="select" @change="onFilterChange">
          <option :value="null">全部区域</option>
          <option v-for="option in campusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
      </div>
      <div class="field">
        <label for="filter-worker">负责人</label>
        <select id="filter-worker" v-model="workerId" class="select" @change="onFilterChange">
          <option :value="null">全部负责人</option>
          <option v-for="option in workerOptions" :key="option.value ?? 'all'" :value="option.value">{{ option.label }}</option>
        </select>
      </div>
      <div class="field">
        <label for="filter-status">状态</label>
        <select id="filter-status" v-model="statusFilter" class="select" @change="onFilterChange">
          <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
      </div>
    </div>

    <PageState :loading="loading" :error="error" :empty="records.length === 0" empty-text="当前筛选条件下没有工单" @retry="load">
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
  </AdminAppShell>
</template>

<style scoped>
.admin-filter-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 240px));
  gap: 12px;
  margin-bottom: 15px;
}

@media (max-width: 700px) {
  .admin-filter-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
