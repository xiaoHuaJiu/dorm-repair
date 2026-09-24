<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import { workerOrderDetail, addMaterialUsage } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { notifySuccess } from '@/api/successNotifier'
import { ORDER_STATUS } from '@/constants/order'
import type { AreaTreeNode, FaultTypeItem } from '@/types/config'
import type { OrderDetail } from '@/types/order'

const navItems = [
  { label: '工作台', to: '/worker/home' },
  { label: '我的工单', to: '/worker/orders' },
  { label: '请假', to: '/worker/leave' },
]

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<OrderDetail | null>(null)
const areaNameOf = ref(new Map<number, string>())
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
      workerOrderDetail(orderId),
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

/** 后端登记材料允许 2-维修中、4-返工中。 */
const canSubmit = computed(() => {
  const status = detail.value?.baseInfo.status
  return status === ORDER_STATUS.REPAIRING || status === ORDER_STATUS.REWORKING
})

const materialName = ref('')
const specification = ref('')
const quantity = ref('1')
const unit = ref('个')
const remark = ref('')
const submitting = ref(false)
const submitError = ref('')

async function submit() {
  if (!materialName.value.trim()) {
    submitError.value = '请填写材料名称'
    return
  }
  const quantityValue = Number(quantity.value)
  if (!Number.isFinite(quantityValue) || quantityValue <= 0) {
    submitError.value = '请填写有效数量'
    return
  }
  if (!unit.value.trim()) {
    submitError.value = '请填写单位'
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    await addMaterialUsage(orderId, {
      materialName: materialName.value.trim(),
      specification: specification.value.trim() || undefined,
      quantity: quantityValue,
      unit: unit.value.trim(),
      remark: remark.value.trim() || undefined,
    })
    notifySuccess('材料已登记')
    void router.push({ name: 'worker-order-detail', params: { id: orderId } })
  } catch (cause) {
    submitError.value = cause instanceof Error ? cause.message : '登记失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function backToDetail() {
  void router.push({ name: 'worker-order-detail', params: { id: orderId } })
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="2" title="维修材料" :subtitle="detail?.baseInfo.orderNo" :nav-items="navItems">
    <PageState :loading="loading" :error="error" @retry="load">
      <template v-if="detail">
        <div class="split">
          <section class="card">
            <div class="section-title">
              <h2>{{ faultTypeName }}</h2>
              <OrderStatusTag :status="detail.baseInfo.status" />
            </div>
            <p class="muted">{{ locationText }}</p>

            <form class="form" @submit.prevent="submit">
              <div class="grid two">
                <div class="field">
                  <label class="required" for="material-name">材料名称</label>
                  <input id="material-name" v-model="materialName" class="input" />
                </div>
                <div class="field">
                  <label for="material-spec">规格</label>
                  <input id="material-spec" v-model="specification" class="input" />
                </div>
                <div class="field">
                  <label class="required" for="material-quantity">数量</label>
                  <input id="material-quantity" v-model="quantity" class="input" type="number" min="0.01" step="0.01" />
                </div>
                <div class="field">
                  <label class="required" for="material-unit">单位</label>
                  <input id="material-unit" v-model="unit" class="input" />
                </div>
              </div>
              <div class="field">
                <label for="material-remark">备注</label>
                <input id="material-remark" v-model="remark" class="input" />
              </div>
              <div v-if="submitError" class="error" role="alert">{{ submitError }}</div>
              <div class="inline-actions">
                <button
                  class="btn primary"
                  type="submit"
                  :disabled="!canSubmit || submitting"
                  :title="canSubmit ? '' : '当前状态不能登记材料'"
                >
                  {{ submitting ? '登记中…' : '登记材料' }}
                </button>
                <button class="btn outline" type="button" @click="backToDetail">返回详情</button>
              </div>
            </form>
          </section>

          <aside class="card">
            <h2>已用材料</h2>
            <template v-if="detail.materialRecords.length">
              <p v-for="record in detail.materialRecords" :key="record.id" class="material-line">
                <strong>{{ record.materialName }}</strong>
                {{ record.specification ?? '' }} · {{ record.quantity }}{{ record.unit ?? '' }}
              </p>
            </template>
            <div v-else class="empty">
              <div class="empty-mark" aria-hidden="true">○</div>
              <p>尚未登记材料</p>
            </div>
          </aside>
        </div>
      </template>
    </PageState>
  </MobileAppShell>
</template>

<style scoped>
.material-line {
  padding: 8px 0;
  border-bottom: 1px solid var(--dr-color-border);
}

.material-line:last-child {
  border-bottom: none;
}

@media (max-width: 900px) {
  .split {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
