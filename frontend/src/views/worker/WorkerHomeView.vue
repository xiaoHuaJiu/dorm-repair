<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import OrderCard from '@/components/common/OrderCard.vue'
import PageState from '@/components/common/PageState.vue'
import { pageWorkerOrders } from '@/api/order'
import { ORDER_STATUS } from '@/constants/order'
import type { OrderQuery } from '@/types/order'
import type { OrderListItem } from '@/types/order'

const navItems = [
  { label: '工作台', to: '/worker/home' },
  { label: '我的工单', to: '/worker/orders' },
  { label: '请假', to: '/worker/leave' },
]

const router = useRouter()

interface LaneState {
  loading: boolean
  error: string | null
  orders: OrderListItem[]
}

const activeLane = ref<LaneState>({ loading: true, error: null, orders: [] })
const pendingLane = ref<LaneState>({ loading: true, error: null, orders: [] })
const alertLane = ref<LaneState>({ loading: true, error: null, orders: [] })

/** 工作台每个区域独立加载，最多展示 5 条。 */
const PAGE_SIZE = 5

async function loadLane(lane: LaneState, query: OrderQuery) {
  lane.loading = true
  lane.error = null
  try {
    const page = await pageWorkerOrders(query)
    lane.orders = page.records
  } catch (error) {
    lane.error = error instanceof Error ? error.message : '加载失败，请稍后重试'
  } finally {
    lane.loading = false
  }
}

function loadAll() {
  void loadLane(activeLane.value, { pageNum: 1, pageSize: PAGE_SIZE, statusList: [ORDER_STATUS.REPAIRING] })
  void loadLane(pendingLane.value, { pageNum: 1, pageSize: PAGE_SIZE, statusList: [ORDER_STATUS.PENDING_ACCEPTANCE] })
  void loadLane(alertLane.value, { pageNum: 1, pageSize: PAGE_SIZE, exceptionFlag: true })
}

onMounted(loadAll)

function openOrder(order: OrderListItem) {
  void router.push({ name: 'worker-order-detail', params: { id: order.orderId } })
}
</script>

<template>
  <MobileAppShell :role="2" title="今日工作台" subtitle="优先处理待接单与异常任务" :nav-items="navItems">
    <div class="worker-board">
      <div class="worker-main-lanes">
        <section class="worker-lane lane-active">
          <div class="lane-head">
            <div>
              <span class="lane-kicker">正在处理</span>
              <h2>我负责的维修工单</h2>
            </div>
            <strong>{{ activeLane.loading ? '…' : activeLane.orders.length }}</strong>
          </div>
          <PageState
            :loading="activeLane.loading"
            :error="activeLane.error"
            :empty="!activeLane.orders.length"
            empty-text="当前没有维修中的工单"
            @retry="loadLane(activeLane, { pageNum: 1, pageSize: PAGE_SIZE, statusList: [ORDER_STATUS.REPAIRING] })"
          >
            <div class="lane-list">
              <OrderCard v-for="order in activeLane.orders" :key="order.orderId" :order="order" @open="openOrder" />
            </div>
          </PageState>
        </section>

        <section class="worker-lane lane-all">
          <div class="lane-head">
            <div>
              <span class="lane-kicker">全校动态</span>
              <h2>全部工单</h2>
            </div>
            <strong>—</strong>
          </div>
          <p class="lane-intro">查看当前学校全部报修任务；非本人负责的工单仅供查看。</p>
          <div class="empty">
            <div class="empty-mark" aria-hidden="true">○</div>
            <p>全校工单查询接口待提供，暂无法展示。</p>
          </div>
        </section>
      </div>

      <div class="worker-side">
        <section class="worker-lane lane-pending">
          <div class="lane-head">
            <div>
              <span class="lane-kicker">等待响应</span>
              <h2>系统派单</h2>
            </div>
            <strong>{{ pendingLane.loading ? '…' : pendingLane.orders.length }}</strong>
          </div>
          <PageState
            :loading="pendingLane.loading"
            :error="pendingLane.error"
            :empty="!pendingLane.orders.length"
            empty-text="暂无待接单任务"
            @retry="loadLane(pendingLane, { pageNum: 1, pageSize: PAGE_SIZE, statusList: [ORDER_STATUS.PENDING_ACCEPTANCE] })"
          >
            <div class="lane-list">
              <OrderCard v-for="order in pendingLane.orders" :key="order.orderId" :order="order" @open="openOrder" />
            </div>
          </PageState>
        </section>

        <section class="worker-lane lane-alert">
          <div class="lane-head">
            <div>
              <span class="lane-kicker">需要关注</span>
              <h2>异常工单</h2>
            </div>
            <strong>{{ alertLane.loading ? '…' : alertLane.orders.length }}</strong>
          </div>
          <PageState
            :loading="alertLane.loading"
            :error="alertLane.error"
            :empty="!alertLane.orders.length"
            empty-text="暂无返工或中断工单"
            @retry="loadLane(alertLane, { pageNum: 1, pageSize: PAGE_SIZE, exceptionFlag: true })"
          >
            <div class="lane-list">
              <OrderCard v-for="order in alertLane.orders" :key="order.orderId" :order="order" @open="openOrder" />
            </div>
          </PageState>
        </section>
      </div>
    </div>
  </MobileAppShell>
</template>

<style scoped>
.worker-board {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(330px, 1fr);
  gap: 18px;
  align-items: start;
}

.worker-main-lanes,
.worker-side {
  display: grid;
  gap: 18px;
}

.worker-lane {
  padding: 20px;
  border: 1px solid var(--dr-color-border);
  border-radius: 18px;
  background: var(--dr-color-surface);
  box-shadow: 0 10px 34px rgb(24 25 28 / 4.5%);
}

.lane-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}

.lane-head h2 {
  margin: 2px 0 0;
  font-size: 17px;
}

.lane-head strong {
  font-size: 22px;
  color: var(--dr-color-primary);
}

.lane-kicker {
  font-size: 12px;
  color: var(--dr-color-text-muted);
}

.lane-intro {
  margin: -5px 0 14px;
  color: var(--dr-color-text-muted);
  font-size: 13px;
}

.lane-list {
  display: grid;
  gap: 12px;
}

@media (max-width: 900px) {
  .worker-board {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
