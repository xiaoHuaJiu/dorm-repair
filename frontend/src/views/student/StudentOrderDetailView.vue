<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import { studentOrderDetail } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { isStudentCancelable } from '@/constants/order'
import { computeOrderPermissions } from '@/utils/orderPermission'
import { ROLE } from '@/constants/role'
import type { AreaTreeNode, FaultTypeItem } from '@/types/config'
import type { OrderDetail } from '@/types/order'

const navItems = [
  { label: '首页', to: '/student/home' },
  { label: '我的工单', to: '/student/orders' },
]

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<OrderDetail | null>(null)
/** 区域树中 ID 到名称的映射，用于还原故障位置。 */
const areaNameOf = ref(new Map<number, string>())
/** 故障类型 ID 到名称的映射。 */
const faultTypeNameOf = ref(new Map<number, string>())

function collectAreaNames(nodes: AreaTreeNode[]) {
  for (const node of nodes) {
    areaNameOf.value.set(node.id, node.areaName)
    collectAreaNames(node.children)
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [order, tree, faultTypes] = await Promise.all([
      studentOrderDetail(orderId),
      enabledAreaTree(),
      listEnabledFaultTypes(),
    ])
    detail.value = order
    areaNameOf.value = new Map()
    collectAreaNames(tree)
    faultTypeNameOf.value = new Map(faultTypes.map((item: FaultTypeItem) => [item.id, item.typeName]))
  } catch (cause) {
    detail.value = null
    error.value = cause instanceof Error ? cause.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function backHome() {
  void router.push({ name: 'student-home' })
}

function openReview() {
  void router.push({ name: 'student-order-review', params: { id: orderId } })
}

const faultTypeName = computed(() => {
  const base = detail.value?.baseInfo
  if (!base) return ''
  return faultTypeNameOf.value.get(base.faultTypeId) ?? `故障类型 #${base.faultTypeId}`
})

const locationText = computed(() => {
  const base = detail.value?.baseInfo
  if (!base) return ''
  const names = [base.campusId, base.areaId, base.buildingId, base.roomId]
    .map((id) => areaNameOf.value.get(id))
    .filter(Boolean)
    .join(' ')
  return base.locationDetail ? `${names} · ${base.locationDetail}` : names
})

const assigneeText = computed(() => {
  const assigneeId = detail.value?.baseInfo.currentAssigneeId
  return assigneeId === null || assigneeId === undefined ? '待分配' : `维修人员 #${assigneeId}`
})

const canConfirm = computed(() =>
  detail.value !== null && computeOrderPermissions(ROLE.STUDENT, detail.value.baseInfo.status).confirm,
)

function completeDeadlineText(): string {
  const base = detail.value?.baseInfo
  return base?.completeDeadline ?? base?.acceptDeadline ?? '接单后生成'
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="1" title="工单详情" :subtitle="detail?.baseInfo.orderNo" :nav-items="navItems">
    <PageState :loading="loading" :error="error" @retry="load">
      <template v-if="detail">
        <section class="card">
          <div class="section-title">
            <div>
              <h2>{{ faultTypeName }}</h2>
              <div class="detail-status-tags">
                <OrderStatusTag :status="detail.baseInfo.status" />
              </div>
            </div>
            <button
              v-if="isStudentCancelable(detail.baseInfo.status)"
              class="btn danger"
              type="button"
              disabled
              title="后端取消工单接口待提供"
              @click.stop
            >
              取消工单
            </button>
          </div>
          <div class="detail-list">
            <div class="detail-item">
              <small>故障位置</small>
              <strong>{{ locationText }}</strong>
            </div>
            <div class="detail-item">
              <small>报修时间</small>
              <strong>{{ detail.baseInfo.reportTime }}</strong>
            </div>
            <div class="detail-item">
              <small>完成期限</small>
              <strong>{{ completeDeadlineText() }}</strong>
            </div>
            <div class="detail-item">
              <small>当前负责人</small>
              <strong>{{ assigneeText }}</strong>
            </div>
          </div>
          <p class="order-description">{{ detail.baseInfo.problemDescription }}</p>
        </section>

        <section class="card section">
          <div class="section-title">
            <h2>维修轨迹</h2>
            <span class="muted">共 {{ detail.flows.length }} 个节点</span>
          </div>
          <OrderTimeline :flows="detail.flows" />
        </section>

        <section v-if="canConfirm" class="card section">
          <h2>等待你的确认</h2>
          <p class="muted">请检查现场后确认完成或申请返工。</p>
          <button class="btn primary" type="button" @click="openReview">验收维修结果</button>
        </section>
      </template>

      <div v-else-if="!loading" class="empty">
        <div class="empty-mark" aria-hidden="true">!</div>
        <p>工单不存在或已被删除</p>
        <button class="btn outline small" type="button" @click="backHome">返回首页</button>
      </div>
    </PageState>
  </MobileAppShell>
</template>

<style scoped>
.detail-status-tags {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.order-description {
  margin: 14px 0 0;
  color: var(--dr-color-text-secondary);
  white-space: pre-wrap;
}
</style>
