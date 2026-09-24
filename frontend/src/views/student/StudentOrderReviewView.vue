<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import FormDialog from '@/components/common/FormDialog.vue'
import ImageUploader from '@/components/common/ImageUploader.vue'
import FileGallery from '@/components/common/FileGallery.vue'
import { deleteFile, uploadFile } from '@/api/file'
import { confirmRepairOrder, studentOrderDetail, submitRepairEvaluation, submitRepairRework } from '@/api/order'
import { notifySuccess } from '@/api/successNotifier'
import { ORDER_STATUS, PROCESS_RECORD_TYPE } from '@/constants/order'
import { computeOrderPermissions } from '@/utils/orderPermission'
import { ROLE } from '@/constants/role'
import type { OrderFile } from '@/types/file'
import type { OrderDetail } from '@/types/order'

const navItems = [
  { label: '首页', to: '/student/home' },
  { label: '我的工单', to: '/student/orders' },
]

const route = useRoute()
const orderId = Number(route.params.id)

const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<OrderDetail | null>(null)
const score = ref(0)
const ratingContent = ref('')
/** 进行中的提交动作：confirm/evaluate/rework。 */
const submitting = ref('')
const actionError = ref('')

// 返工申请弹窗。
const reworkVisible = ref(false)
const reworkReason = ref('')
const reworkImages = ref<OrderFile[]>([])

async function load() {
  loading.value = true
  error.value = null
  try {
    detail.value = await studentOrderDetail(orderId)
  } catch (cause) {
    detail.value = null
    error.value = cause instanceof Error ? cause.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 维修结果说明：取最新一条"提交维修结果"过程记录。 */
const repairResultText = computed(() => {
  const records = detail.value?.processRecords ?? []
  const result = records.filter((item) => item.recordType === PROCESS_RECORD_TYPE.SUBMIT_RESULT).at(-1)
  return result?.content || '维修人员尚未提交完成说明'
})

/** 维修结果图片：取最新一条"提交维修结果"过程记录的附件。 */
const resultImages = computed(() => {
  const records = detail.value?.processRecords ?? []
  const result = records.filter((item) => item.recordType === PROCESS_RECORD_TYPE.SUBMIT_RESULT).at(-1)
  return result?.files ?? []
})

const completed = computed(() => detail.value?.baseInfo.status === ORDER_STATUS.COMPLETED)

const permissions = computed(() =>
  detail.value === null ? null : computeOrderPermissions(ROLE.STUDENT, detail.value.baseInfo.status),
)

const existingScore = computed(() => detail.value?.evaluation?.score ?? null)

function errorMessageOf(cause: unknown, fallback: string): string {
  return cause instanceof Error ? cause.message : fallback
}

async function confirmDone() {
  submitting.value = 'confirm'
  actionError.value = ''
  try {
    await confirmRepairOrder(orderId)
    notifySuccess('已确认完成')
    await load()
  } catch (cause) {
    actionError.value = errorMessageOf(cause, '确认失败，请稍后重试')
  } finally {
    submitting.value = ''
  }
}

async function submitRating() {
  if (score.value < 1 || score.value > 5) {
    actionError.value = '请选择评分'
    return
  }
  submitting.value = 'evaluate'
  actionError.value = ''
  try {
    await submitRepairEvaluation(orderId, {
      score: score.value,
      content: ratingContent.value.trim() || undefined,
    })
    notifySuccess('评价已提交')
    await load()
  } catch (cause) {
    actionError.value = errorMessageOf(cause, '评价提交失败，请稍后重试')
  } finally {
    submitting.value = ''
  }
}

function openRework() {
  reworkReason.value = ''
  reworkImages.value = []
  reworkVisible.value = true
}

function closeRework() {
  if (submitting.value === 'rework') return
  // 关闭即放弃未绑定文件，删除释放存储；重复触发时仅执行一次。
  if (reworkVisible.value) {
    for (const item of reworkImages.value) void deleteFile(item.fileId).catch(() => undefined)
  }
  reworkVisible.value = false
}

async function onAddReworkFiles(files: File[]) {
  let firstError = ''
  for (const file of files) {
    try {
      const item = await uploadFile(file, 'REWORK')
      reworkImages.value = [...reworkImages.value, item]
    } catch (cause) {
      if (!firstError) firstError = errorMessageOf(cause, '图片上传失败')
    }
  }
  if (firstError) actionError.value = firstError
}

function onRemoveReworkFile(file: OrderFile) {
  reworkImages.value = reworkImages.value.filter((item) => item.fileId !== file.fileId)
  void deleteFile(file.fileId).catch(() => undefined)
}

async function submitRework() {
  if (!reworkReason.value.trim()) {
    actionError.value = '请填写返工原因'
    return
  }
  submitting.value = 'rework'
  actionError.value = ''
  try {
    await submitRepairRework(orderId, {
      reason: reworkReason.value.trim(),
      fileIds: reworkImages.value.map((item) => item.fileId),
    })
    notifySuccess('返工申请已提交')
    reworkVisible.value = false
    await load()
  } catch (cause) {
    actionError.value = errorMessageOf(cause, '返工申请提交失败，请稍后重试')
  } finally {
    submitting.value = ''
  }
}

onMounted(load)
</script>

<template>
  <MobileAppShell :role="1" title="验收维修结果" :subtitle="detail?.baseInfo.orderNo" :nav-items="navItems">
    <PageState :loading="loading" :error="error" @retry="load">
      <div v-if="detail" class="split">
        <section class="card">
          <h2>{{ detail.baseInfo.problemDescription.slice(0, 24) }}</h2>
          <div class="review-status">
            <OrderStatusTag :status="detail.baseInfo.status" />
          </div>
          <div class="notice">
            <strong>维修结果</strong>
            <p>{{ repairResultText }}</p>
            <FileGallery :files="resultImages" />
          </div>
          <div v-if="permissions?.confirm || permissions?.requestRework" class="section">
            <h2>现场确认</h2>
            <p class="muted">请确认故障是否已经解决。申请返工不会创建新工单。</p>
            <div class="inline-actions">
              <button
                class="btn primary"
                type="button"
                :disabled="submitting !== ''"
                @click="confirmDone"
              >
                {{ submitting === 'confirm' ? '提交中…' : '确认已解决' }}
              </button>
              <button
                class="btn danger"
                type="button"
                :disabled="submitting !== ''"
                @click="openRework"
              >
                问题未解决，申请返工
              </button>
            </div>
          </div>
          <div v-if="completed" class="section">
            <h2>服务评价</h2>
            <div class="stars" aria-label="服务评分">
              <button
                v-for="n in 5"
                :key="n"
                class="star"
                :class="{ active: n <= (existingScore ?? score) }"
                type="button"
                :aria-label="`${n}星`"
                :disabled="existingScore !== null || submitting !== ''"
                @click="score = n"
              >
                ★
              </button>
            </div>
            <textarea
              v-model="ratingContent"
              class="textarea"
              placeholder="说说本次维修体验（可选）"
              :disabled="existingScore !== null || submitting !== ''"
            ></textarea>
            <div v-if="existingScore !== null" class="muted rating-done">已评价 {{ existingScore }} 星</div>
            <button
              v-else
              class="btn primary submit-rating"
              type="button"
              :disabled="submitting !== ''"
              @click="submitRating"
            >
              {{ submitting === 'evaluate' ? '提交中…' : '提交评价' }}
            </button>
          </div>
          <div v-if="actionError" class="error" role="alert">{{ actionError }}</div>
        </section>
        <aside class="card">
          <h2>维修轨迹</h2>
          <OrderTimeline :flows="detail.flows" />
        </aside>
      </div>
    </PageState>

    <FormDialog
      :visible="reworkVisible"
      title="申请返工"
      confirm-text="提交返工"
      :loading="submitting === 'rework'"
      :error="actionError"
      @update:visible="closeRework"
      @submit="submitRework"
      @cancel="closeRework"
    >
      <form class="form" @submit.prevent>
        <div class="field">
          <label class="required" for="rework-reason">返工原因</label>
          <textarea
            id="rework-reason"
            v-model="reworkReason"
            class="textarea"
            placeholder="说明哪些问题仍未解决"
          ></textarea>
        </div>
        <div class="field">
          <label for="rework-images">现场图片（可选）</label>
          <ImageUploader v-model="reworkImages" :max="9" @add-files="onAddReworkFiles" @remove-file="onRemoveReworkFile" />
        </div>
      </form>
    </FormDialog>
  </MobileAppShell>
</template>

<style scoped>
.review-status {
  display: flex;
  gap: 6px;
  margin: 10px 0 0;
}

.notice {
  margin-top: 14px;
}

.rating-done {
  margin-top: 10px;
}
</style>
