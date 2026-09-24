<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderTableRow from '@/components/common/OrderTableRow.vue'
import { pageAdminOrders } from '@/api/order'
import type { OrderListItem } from '@/types/order'

const router = useRouter()

/** 待审批抽屉是否展开。 */
const approvalExpanded = ref(false)

/** 统计接口未提供，四张卡片数字统一占位。 */
const METRIC_PLACEHOLDER = '—'

/** 今日优先事项：加载异常工单前 5 条。 */
const loading = ref(true)
const error = ref<string | null>(null)
const priorityOrders = ref<OrderListItem[]>([])

async function loadPriority() {
  loading.value = true
  error.value = null
  try {
    const page = await pageAdminOrders({ pageNum: 1, pageSize: 5, exceptionFlag: true })
    priorityOrders.value = page.records
  } catch (cause) {
    priorityOrders.value = []
    error.value = cause instanceof Error ? cause.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function toggleApproval() {
  approvalExpanded.value = !approvalExpanded.value
}

function openDetail(order: OrderListItem) {
  void router.push({ name: 'admin-order-detail', params: { id: order.orderId } })
}

onMounted(loadPriority)
</script>

<template>
  <AdminAppShell title="管理工作台" subtitle="审批、派单与异常事项集中处理">
    <div class="admin-metrics" :class="{ 'approval-open': approvalExpanded }">
      <button
        class="metric-card approval"
        type="button"
        :aria-expanded="approvalExpanded"
        @click="toggleApproval"
      >
        <span>待审批</span>
        <strong>{{ METRIC_PLACEHOLDER }}</strong>
        <small>点击展开审批列表</small>
      </button>
      <RouterLink class="metric-card" to="/admin/orders/manual-dispatch">
        <span>待人工派单</span>
        <strong>{{ METRIC_PLACEHOLDER }}</strong>
        <small>进入派单队列</small>
      </RouterLink>
      <RouterLink class="metric-card" to="/admin/orders/exceptions?type=timeout">
        <span>超时风险</span>
        <strong>{{ METRIC_PLACEHOLDER }}</strong>
        <small>查看风险工单</small>
      </RouterLink>
      <RouterLink class="metric-card" to="/admin/orders/exceptions">
        <span>异常数量</span>
        <strong>{{ METRIC_PLACEHOLDER }}</strong>
        <small>查看全部异常</small>
      </RouterLink>
    </div>

    <section v-if="approvalExpanded" class="approval-drawer" aria-label="待审批事项">
      <div>
        <div class="section-title">
          <h2>转派审批</h2>
          <RouterLink to="/admin/approvals/transfers">查看全部</RouterLink>
        </div>
        <p class="muted">转派审批接口待提供，暂无法展示待审批申请。</p>
      </div>
      <div>
        <div class="section-title">
          <h2>请假审批</h2>
          <RouterLink to="/admin/approvals/leaves">查看全部</RouterLink>
        </div>
        <p class="muted">请假审批接口待提供，暂无法展示待审批申请。</p>
      </div>
    </section>

    <section class="section">
      <div class="section-title">
        <h2>今日优先事项</h2>
        <RouterLink to="/admin/orders/exceptions">查看异常工单</RouterLink>
      </div>
      <PageState :loading="loading" :error="error" :empty="priorityOrders.length === 0" empty-text="当前没有异常工单" @retry="loadPriority">
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
              <OrderTableRow v-for="order in priorityOrders" :key="order.orderId" :order="order" @open="openDetail" />
            </tbody>
          </table>
        </div>
      </PageState>
    </section>
  </AdminAppShell>
</template>

<style scoped>
.admin-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  transition: grid-template-columns 0.28s ease;
}

.metric-card {
  text-align: left;
  border: 1px solid var(--dr-color-border);
  background: var(--dr-color-surface);
  border-radius: 16px;
  padding: 18px;
  display: grid;
  gap: 5px;
  color: var(--dr-color-text);
  cursor: pointer;
}

.metric-card strong {
  font-size: 26px;
  color: var(--dr-color-primary);
}

.metric-card small {
  color: var(--dr-color-text-muted);
}

.approval-drawer {
  margin-top: 14px;
  background: var(--dr-color-surface);
  border: 1px solid #efaaa7;
  border-radius: 16px;
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

/* 展开审批抽屉时，待审批卡片变宽，其余卡片收缩。 */
.approval-open {
  grid-template-columns: 2.6fr 0.7fr 0.7fr 0.7fr;
}

@media (max-width: 900px) {
  .admin-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .approval-open {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .approval-drawer {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
