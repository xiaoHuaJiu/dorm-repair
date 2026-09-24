<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import ImageUploader from '@/components/common/ImageUploader.vue'
import { workerOrderDetail, addRepairProcess } from '@/api/order'
import { deleteFile, uploadFile } from '@/api/file'
import { enabledAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { notifySuccess } from '@/api/successNotifier'
import { computeOrderPermissions } from '@/utils/orderPermission'
import { ROLE } from '@/constants/role'
import type { AreaTreeNode, FaultTypeItem } from '@/types/config'
import type { OrderFile } from '@/types/file'
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

const canSubmit = computed(() => detail.value !== null && computeOrderPermissions(ROLE.WORKER, detail.value.baseInfo.status, true).addProcessRecord)

const content = ref('')
/** 已上传的现场图片；提交时仅传 fileId。 */
const images = ref<OrderFile[]>([])
const submitting = ref(false)
const submitError = ref('')

/** 选择图片后逐张上传，失败信息并入提交错误提示。 */
async function onAddFiles(files: File[]) {
  let firstError = ''
  for (const file of files) {
    try {
      const item = await uploadFile(file, 'PROCESS')
      images.value = [...images.value, item]
    } catch (cause) {
      if (!firstError) firstError = cause instanceof Error ? cause.message : '图片上传失败'
    }
  }
  if (firstError) submitError.value = firstError
}

/** 移除图片：从列表移除并删除未绑定文件；删除失败由后端孤儿清理兜底。 */
function onRemoveFile(file: OrderFile) {
  images.value = images.value.filter((item) => item.fileId !== file.fileId)
  void deleteFile(file.fileId).catch(() => undefined)
}

/** 离开页面时放弃未绑定文件，删除释放存储。 */
function discardUnboundFiles() {
  for (const item of images.value) void deleteFile(item.fileId).catch(() => undefined)
}

async function submit() {
  if (!content.value.trim()) {
    submitError.value = '请填写处理内容'
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    await addRepairProcess(orderId, {
      content: content.value.trim(),
      fileIds: images.value.map((item) => item.fileId),
    })
    notifySuccess('维修记录已添加')
    void router.push({ name: 'worker-order-detail', params: { id: orderId } })
  } catch (cause) {
    submitError.value = cause instanceof Error ? cause.message : '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function backToDetail() {
  discardUnboundFiles()
  void router.push({ name: 'worker-order-detail', params: { id: orderId } })
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="2" title="添加维修记录" :subtitle="detail?.baseInfo.orderNo" :nav-items="navItems">
    <PageState :loading="loading" :error="error" @retry="load">
      <section v-if="detail" class="card">
        <div class="section-title">
          <h2>{{ faultTypeName }}</h2>
          <OrderStatusTag :status="detail.baseInfo.status" />
        </div>
        <p class="muted">{{ locationText }}</p>

        <form class="form" @submit.prevent="submit">
          <div class="field">
            <label class="required" for="process-content">处理内容</label>
            <textarea
              id="process-content"
              v-model="content"
              class="textarea"
              placeholder="记录检查结果、处理步骤或等待事项"
            ></textarea>
          </div>
          <div class="field">
            <label for="process-images">现场图片（可选）</label>
            <ImageUploader v-model="images" :max="9" @add-files="onAddFiles" @remove-file="onRemoveFile" />
          </div>
          <div v-if="submitError" class="error" role="alert">{{ submitError }}</div>
          <div class="inline-actions">
            <button
              class="btn primary"
              type="submit"
              :disabled="!canSubmit || submitting"
              :title="canSubmit ? '' : '当前状态不能添加处理记录'"
            >
              {{ submitting ? '保存中…' : '保存处理记录' }}
            </button>
            <button class="btn outline" type="button" @click="backToDetail">返回详情</button>
          </div>
        </form>
      </section>
    </PageState>
  </MobileAppShell>
</template>
