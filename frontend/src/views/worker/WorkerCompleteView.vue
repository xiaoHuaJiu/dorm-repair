<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import ImageUploader from '@/components/common/ImageUploader.vue'
import { workerOrderDetail, submitRepairResult } from '@/api/order'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { notifySuccess } from '@/api/successNotifier'
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

const canSubmit = computed(() => detail.value !== null && computeOrderPermissions(ROLE.WORKER, detail.value.baseInfo.status, true).submitResult)

const resultDescription = ref('')
/** 图片地址列表；文件上传接口未提供，恒为空且选择入口禁用。 */
const images = ref<string[]>([])
const submitting = ref(false)
const submitError = ref('')

async function submit() {
  if (!resultDescription.value.trim()) {
    submitError.value = '请填写完成说明'
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    await submitRepairResult(orderId, { resultDescription: resultDescription.value.trim() })
    notifySuccess('维修结果已提交，等待学生确认')
    void router.push({ name: 'worker-order-detail', params: { id: orderId } })
  } catch (cause) {
    submitError.value = cause instanceof Error ? cause.message : '提交失败，请稍后重试'
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
  <MobileAppShell :role="2" title="提交维修结果" :subtitle="detail?.baseInfo.orderNo" :nav-items="navItems">
    <PageState :loading="loading" :error="error" @retry="load">
      <section v-if="detail" class="card">
        <div class="section-title">
          <h2>{{ faultTypeName }}</h2>
          <OrderStatusTag :status="detail.baseInfo.status" />
        </div>
        <p class="muted">{{ locationText }}</p>

        <form class="form" @submit.prevent="submit">
          <div class="notice">提交后工单进入“待确认”，学生将看到完成说明和图片。</div>
          <div class="field">
            <label class="required" for="complete-description">完成说明</label>
            <textarea
              id="complete-description"
              v-model="resultDescription"
              class="textarea"
              placeholder="说明故障原因、处理方式和测试结果"
            ></textarea>
          </div>
          <div class="field">
            <label for="complete-images">完成图片（可选）</label>
            <ImageUploader v-model="images" :max="9" disabled>
              <template #hint>文件上传接口待提供，暂不能上传图片。</template>
            </ImageUploader>
          </div>
          <div v-if="submitError" class="error" role="alert">{{ submitError }}</div>
          <div class="inline-actions">
            <button
              class="btn primary"
              type="submit"
              :disabled="!canSubmit || submitting"
              :title="canSubmit ? '' : '当前状态不能提交维修结果'"
            >
              {{ submitting ? '提交中…' : '提交维修完成' }}
            </button>
            <button class="btn outline" type="button" @click="backToDetail">返回详情</button>
          </div>
        </form>
      </section>
    </PageState>
  </MobileAppShell>
</template>
