<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import FormDialog from '@/components/common/FormDialog.vue'
import FileGallery from '@/components/common/FileGallery.vue'
import { workerOrderDetail, acceptRepairOrder, interruptRepair, resumeRepair } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { notifySuccess } from '@/api/successNotifier'
import { notifyError } from '@/api/errorNotifier'
import { INTERRUPT_REASON, ORDER_STATUS } from '@/constants/order'
import { computeOrderPermissions } from '@/utils/orderPermission'
import { ROLE } from '@/constants/role'
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

/** 维修端详情接口只返回本人负责工单，因此按负责人计算操作权限。 */
const permissions = computed(() =>
  detail.value ? computeOrderPermissions(ROLE.WORKER, detail.value.baseInfo.status, true) : null,
)

/** 工单操作相关弹窗状态。 */
const acceptVisible = ref(false)
const acceptExpectedTime = ref('')
const acceptLoading = ref(false)
const acceptError = ref('')

const interruptVisible = ref(false)
const interruptReasonType = ref(1)
const interruptContent = ref('')
const interruptLoading = ref(false)
const interruptError = ref('')

const resumeLoading = ref(false)

function completeDeadlineText(): string {
  const base = detail.value?.baseInfo
  return base?.completeDeadline ?? base?.acceptDeadline ?? '接单后生成'
}

/** datetime-local 值转后端接受的 ISO 时间串。 */
function toIsoDateTime(value: string): string | undefined {
  return value ? `${value}:00` : undefined
}

async function submitAccept() {
  if (!detail.value) return
  acceptLoading.value = true
  acceptError.value = ''
  try {
    await acceptRepairOrder(orderId, { expectedCompleteTime: toIsoDateTime(acceptExpectedTime.value) })
    acceptVisible.value = false
    notifySuccess('接单成功，工单已进入维修中')
    await load()
  } catch (cause) {
    acceptError.value = cause instanceof Error ? cause.message : '接单失败，请稍后重试'
  } finally {
    acceptLoading.value = false
  }
}

function openAccept() {
  acceptExpectedTime.value = ''
  acceptError.value = ''
  acceptVisible.value = true
}

async function submitInterrupt() {
  if (!detail.value) return
  if (!interruptContent.value.trim()) {
    interruptError.value = '请填写中断说明'
    return
  }
  interruptLoading.value = true
  interruptError.value = ''
  try {
    await interruptRepair(orderId, {
      interruptReasonType: interruptReasonType.value,
      content: interruptContent.value.trim(),
    })
    interruptVisible.value = false
    notifySuccess('维修已中断')
    await load()
  } catch (cause) {
    interruptError.value = cause instanceof Error ? cause.message : '中断失败，请稍后重试'
  } finally {
    interruptLoading.value = false
  }
}

function openInterrupt() {
  interruptReasonType.value = 1
  interruptContent.value = ''
  interruptError.value = ''
  interruptVisible.value = true
}

async function onResume() {
  resumeLoading.value = true
  try {
    await resumeRepair(orderId)
    notifySuccess('维修已恢复')
    await load()
  } catch (cause) {
    notifyError(cause instanceof Error ? cause.message : '恢复失败，请稍后重试')
  } finally {
    resumeLoading.value = false
  }
}

function goTo(name: string) {
  void router.push({ name, params: { id: orderId } })
}

function backToOrders() {
  void router.push({ name: 'worker-orders' })
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="2" title="维修任务" :subtitle="detail?.baseInfo.orderNo" :nav-items="navItems">
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
                  <small>联系人</small>
                  <strong>{{ detail.baseInfo.contactName }} {{ detail.baseInfo.contactPhone }}</strong>
                </div>
                <div class="detail-item">
                  <small>接单截止</small>
                  <strong>{{ detail.baseInfo.acceptDeadline ?? '—' }}</strong>
                </div>
                <div class="detail-item">
                  <small>完成截止</small>
                  <strong>{{ completeDeadlineText() }}</strong>
                </div>
              </div>
              <p class="order-description">{{ detail.baseInfo.problemDescription }}</p>
              <FileGallery :files="detail.baseInfo.files" />

              <div class="inline-actions order-actions-row">
                <button
                  v-if="permissions?.accept"
                  class="btn primary"
                  type="button"
                  @click="openAccept"
                >
                  接单
                </button>
                <button
                  v-if="permissions?.resume"
                  class="btn primary"
                  type="button"
                  :disabled="resumeLoading"
                  @click="onResume"
                >
                  恢复维修
                </button>
                <button
                  v-if="permissions?.interrupt"
                  class="btn danger"
                  type="button"
                  @click="openInterrupt"
                >
                  中断维修
                </button>
                <button
                  v-if="permissions?.transfer"
                  class="btn outline"
                  type="button"
                  disabled
                  title="后端转派申请接口待提供"
                  @click.stop
                >
                  申请转派
                </button>
              </div>
            </section>

            <section class="card section">
              <div class="section-title">
                <h2>维修轨迹</h2>
                <span class="muted">共 {{ detail.flows.length }} 个节点</span>
              </div>
              <OrderTimeline :flows="detail.flows" />
            </section>

            <section v-if="detail.processRecords.length" class="card section">
              <div class="section-title">
                <h2>处理记录</h2>
                <span class="muted">共 {{ detail.processRecords.length }} 条</span>
              </div>
              <div v-for="record in detail.processRecords" :key="record.id" class="process-item">
                <small>{{ record.recordTime }}</small>
                <p>{{ record.content }}</p>
                <FileGallery :files="record.files" />
              </div>
            </section>

            <section v-if="detail.reworkRecords.length" class="card section">
              <div class="section-title">
                <h2>返工记录</h2>
                <span class="muted">共 {{ detail.reworkRecords.length }} 次</span>
              </div>
              <div v-for="record in detail.reworkRecords" :key="record.id" class="process-item">
                <small>第 {{ record.reworkNo }} 次返工 · {{ record.createTime }}</small>
                <p>{{ record.reason }}</p>
                <FileGallery :files="record.files" />
              </div>
            </section>
          </div>

          <aside class="card record-actions">
            <h2>记录与结果</h2>
            <div class="record-actions-grid">
              <button
                class="btn outline"
                type="button"
                :disabled="!permissions?.addProcessRecord"
                :title="permissions?.addProcessRecord ? '' : '当前状态不能添加处理记录'"
                @click="permissions?.addProcessRecord && goTo('worker-order-process')"
              >
                添加处理记录
              </button>
              <button
                class="btn outline"
                type="button"
                :disabled="detail.baseInfo.status !== ORDER_STATUS.REPAIRING && detail.baseInfo.status !== ORDER_STATUS.REWORKING"
                :title="detail.baseInfo.status === ORDER_STATUS.REPAIRING || detail.baseInfo.status === ORDER_STATUS.REWORKING ? '' : '当前状态不能登记材料'"
                @click="
                  (detail.baseInfo.status === ORDER_STATUS.REPAIRING || detail.baseInfo.status === ORDER_STATUS.REWORKING) &&
                    goTo('worker-order-materials')
                "
              >
                登记使用材料
              </button>
              <button
                class="btn primary"
                type="button"
                :disabled="!permissions?.submitResult"
                :title="permissions?.submitResult ? '' : '当前状态不能提交维修结果'"
                @click="permissions?.submitResult && goTo('worker-order-complete')"
              >
                提交维修结果
              </button>
            </div>
          </aside>
        </div>
      </template>

      <div v-else-if="!loading" class="empty">
        <div class="empty-mark" aria-hidden="true">!</div>
        <p>工单不存在或不属于当前账号</p>
        <button class="btn outline small" type="button" @click="backToOrders">返回我的工单</button>
      </div>
    </PageState>

    <FormDialog
      v-model:visible="acceptVisible"
      title="确认接单"
      confirm-text="确认接单"
      :loading="acceptLoading"
      :error="acceptError"
      @submit="submitAccept"
      @cancel="acceptVisible = false"
    >
      <div class="field">
        <label for="accept-deadline">预计完成时间（可选）</label>
        <input id="accept-deadline" v-model="acceptExpectedTime" class="input" type="datetime-local" />
      </div>
      <p class="hint">不填写时默认按接单后 24 小时计算。</p>
    </FormDialog>

    <FormDialog
      v-model:visible="interruptVisible"
      title="中断维修"
      confirm-text="确认中断"
      :loading="interruptLoading"
      :error="interruptError"
      @submit="submitInterrupt"
      @cancel="interruptVisible = false"
    >
      <div class="field">
        <label for="interrupt-reason">中断原因</label>
        <select id="interrupt-reason" v-model.number="interruptReasonType" class="select">
          <option v-for="(label, value) in INTERRUPT_REASON" :key="value" :value="Number(value)">
            {{ label }}
          </option>
        </select>
      </div>
      <div class="field">
        <label class="required" for="interrupt-content">原因说明</label>
        <textarea
          id="interrupt-content"
          v-model="interruptContent"
          class="textarea"
          placeholder="说明中断原因与预计恢复时间"
        ></textarea>
      </div>
    </FormDialog>
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

.order-actions-row {
  margin-top: 16px;
}

.process-item {
  display: grid;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px solid var(--dr-color-border);
}

.process-item:last-child {
  border-bottom: none;
}

.split-main {
  display: grid;
  gap: 18px;
}

.record-actions {
  align-self: start;
}

.record-actions-grid {
  display: grid;
  gap: 10px;
}

@media (max-width: 900px) {
  .split {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
