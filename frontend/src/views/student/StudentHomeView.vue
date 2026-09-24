<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import RepairCreateDialog from './RepairCreateDialog.vue'
import { pageStudentOrders } from '@/api/order'
import { ORDER_STATUS } from '@/constants/order'
import type { OrderListItem } from '@/types/order'

const navItems = [
  { label: '首页', to: '/student/home' },
  { label: '我的工单', to: '/student/orders' },
]

const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)
const activeOrders = ref<OrderListItem[]>([])
const repairVisible = ref(false)

/** 进行中的工单：待派单、待接单、维修中、返工中、已中断。 */
const OPEN_STATUSES = [
  ORDER_STATUS.PENDING_DISPATCH,
  ORDER_STATUS.PENDING_ACCEPTANCE,
  ORDER_STATUS.REPAIRING,
  ORDER_STATUS.REWORKING,
  ORDER_STATUS.INTERRUPTED,
]

async function load() {
  loading.value = true
  error.value = null
  try {
    const page = await pageStudentOrders({ pageNum: 1, pageSize: 50, statusList: [...OPEN_STATUSES] })
    activeOrders.value = page.records
  } catch (cause) {
    activeOrders.value = []
    error.value = cause instanceof Error ? cause.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function openDetail(order: OrderListItem) {
  void router.push({ name: 'student-order-detail', params: { id: order.orderId } })
}

function onCreated(orderId: number) {
  void router.push({ name: 'student-order-detail', params: { id: orderId } })
}

onMounted(load)
</script>

<template>
  <MobileAppShell
    :role="1"
    title="首页"
    subtitle="报修进度清楚，维修过程随时可查"
    :nav-items="navItems"
  >
    <template #head-action>
      <button class="btn primary student-repair-trigger" type="button" @click="repairVisible = true">我要报修</button>
    </template>

    <PageState :loading="loading" :error="error" @retry="load">
      <section v-if="activeOrders.length" class="hero">
        <div>
          <span class="tag red">当前工单 · {{ activeOrders[0].orderNo }}</span>
          <h1>{{ activeOrders[0].faultTypeName }}</h1>
          <p>
            {{ activeOrders[0].locationText }}，当前由
            {{ activeOrders[0].workerName || '待分配人员' }} 处理。
          </p>
          <div class="track" aria-hidden="true">
            <span class="done"></span>
            <span class="done"></span>
            <span class="now"></span>
            <span></span>
            <span></span>
          </div>
          <button class="btn outline" type="button" @click="openDetail(activeOrders[0])">查看进度</button>
        </div>
        <div class="hero-side">
          <div class="stat-value">{{ activeOrders.length }}</div>
          <div class="muted">本人进行中的报修</div>
        </div>
      </section>

      <section v-else class="card empty">
        <div class="empty-mark" aria-hidden="true">✓</div>
        <h2>当前没有进行中的报修</h2>
        <p>如遇故障，可点击右上角“我要报修”。</p>
      </section>
    </PageState>

    <section class="section">
      <div class="section-title">
        <div>
          <h2>全校报修动态</h2>
          <p class="muted">展示当前学校全部工单</p>
        </div>
      </div>
      <div class="empty">
        <div class="empty-mark" aria-hidden="true">○</div>
        <p>全校工单列表接口待后端提供，联调后展示。</p>
      </div>
    </section>

    <RepairCreateDialog v-model:visible="repairVisible" @created="onCreated" />
  </MobileAppShell>
</template>
