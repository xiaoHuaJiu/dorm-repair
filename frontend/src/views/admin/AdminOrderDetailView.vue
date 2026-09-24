<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import FileGallery from '@/components/common/FileGallery.vue'
import { adminOrderDetail } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { pageWorkers } from '@/api/worker'
import type { AreaTreeNode, FaultTypeItem } from '@/types/config'
import type { OrderDetail } from '@/types/order'

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
/** 维修人员 ID 到姓名映射，用于展示负责人与转派轨迹。 */
const workerNameOf = ref(new Map<number, string>())

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
      adminOrderDetail(orderId),
      enabledAreaTree(),
      listEnabledFaultTypes(),
    ])
    detail.value = order
    areaNameOf.value = new Map()
    collectAreaNames(tree)
    faultTypeNameOf.value = new Map(faultTypes.map((item: FaultTypeItem) => [item.id, item.typeName]))
    await loadWorkerNames()
  } catch (cause) {
    detail.value = null
    error.value = cause instanceof Error ? cause.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 负责人姓名映射加载失败不阻塞详情展示。 */
async function loadWorkerNames() {
  try {
    const workers = await pageWorkers({ pageNum: 1, pageSize: 500 })
    workerNameOf.value = new Map(
      workers.records.map((worker) => [worker.workerId, worker.realName ?? `维修人员 #${worker.workerId}`]),
    )
  } catch {
    workerNameOf.value = new Map()
  }
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
  const id = detail.value?.baseInfo.currentAssigneeId
  if (id == null) return '待分配'
  return workerNameOf.value.get(id) ?? `维修人员 #${id}`
})

function assigneeNameOf(id: number | null): string {
  if (id == null) return '待分配'
  return workerNameOf.value.get(id) ?? `维修人员 #${id}`
}

function backToOrders() {
  void router.push({ name: 'admin-orders' })
}

onMounted(load)
</script>

<template>
  <AdminAppShell title="工单详情" :subtitle="detail?.baseInfo.orderNo">
    <PageState :loading="loading" :error="error" @retry="load">
      <template v-if="detail">
        <div class="split">
          <div class="split-main">
            <section class="card">
              <div class="section-title">
                <div>
                  <h2>{{ faultTypeName }}</h2>
                  <div class="detail-status-tags">
                    <OrderStatusTag :status="detail.baseInfo.status" />
                  </div>
                </div>
              </div>
              <div class="detail-list">
                <div class="detail-item">
                  <small>位置</small>
                  <strong>{{ locationText }}</strong>
                </div>
                <div class="detail-item">
                  <small>当前负责人</small>
                  <strong>{{ assigneeText }}</strong>
                </div>
                <div class="detail-item">
                  <small>联系人</small>
                  <strong>{{ detail.baseInfo.contactName }} {{ detail.baseInfo.contactPhone }}</strong>
                </div>
                <div class="detail-item">
                  <small>报修时间</small>
                  <strong>{{ detail.baseInfo.reportTime }}</strong>
                </div>
                <div class="detail-item">
                  <small>接单截止</small>
                  <strong>{{ detail.baseInfo.acceptDeadline ?? '—' }}</strong>
                </div>
                <div class="detail-item">
                  <small>完成截止</small>
                  <strong>{{ detail.baseInfo.completeDeadline ?? detail.baseInfo.expectedCompleteTime ?? '—' }}</strong>
                </div>
              </div>
              <p class="order-description">{{ detail.baseInfo.problemDescription }}</p>
              <FileGallery :files="detail.baseInfo.files" />
            </section>

            <section class="card section">
              <div class="section-title">
                <h2>维修轨迹</h2>
                <span class="muted">共 {{ detail.flows.length }} 个节点</span>
              </div>
              <OrderTimeline :flows="detail.flows" :assignee-name-of="assigneeNameOf" />
            </section>
          </div>

          <aside class="card record-actions">
            <h2>维修记录</h2>
            <div class="aside-list">
              <p v-if="detail.processRecords.length === 0 && detail.materialRecords.length === 0" class="muted">
                暂无过程记录与材料记录
              </p>
              <div v-for="record in detail.processRecords" :key="record.id" class="aside-item">
                <small>过程记录 · {{ record.recordTime }}</small>
                <span>{{ record.content }}</span>
                <FileGallery :files="record.files" />
              </div>
              <div v-for="record in detail.reworkRecords" :key="record.id" class="aside-item">
                <small>第 {{ record.reworkNo }} 次返工 · {{ record.createTime }}</small>
                <span>{{ record.reason }}</span>
                <FileGallery :files="record.files" />
              </div>
              <div v-for="material in detail.materialRecords" :key="material.id" class="aside-item">
                <small>材料使用 · {{ material.useTime ?? material.createTime }}</small>
                <span>{{ material.materialName }} × {{ material.quantity }} {{ material.unit }}</span>
              </div>
            </div>
          </aside>
        </div>
      </template>

      <div v-else-if="!loading" class="empty">
        <div class="empty-mark" aria-hidden="true">!</div>
        <p>工单不存在</p>
        <button class="btn outline small" type="button" @click="backToOrders">返回全部工单</button>
      </div>
    </PageState>
  </AdminAppShell>
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

.split-main {
  display: grid;
  gap: 18px;
}

.record-actions {
  align-self: start;
}

.aside-list {
  display: grid;
  gap: 12px;
}

.aside-item {
  display: grid;
  gap: 3px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--dr-color-border);
}

.aside-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
</style>
