<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MobileAppShell from '@/layouts/MobileAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import OrderStatusTag from '@/components/common/OrderStatusTag.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import { studentOrderDetail } from '@/api/order'
import { ORDER_STATUS, PROCESS_RECORD_TYPE } from '@/constants/order'
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

const completed = computed(() => detail.value?.baseInfo.status === ORDER_STATUS.COMPLETED)

const existingScore = computed(() => detail.value?.evaluation?.score ?? null)

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
          </div>
          <div class="section">
            <h2>现场确认</h2>
            <p class="muted">请确认故障是否已经解决。申请返工不会创建新工单。</p>
            <div class="inline-actions">
              <button
                class="btn primary"
                type="button"
                disabled
                :title="completed ? '已完成，无需再次确认' : '确认完成接口待后端提供'"
                @click.stop
              >
                确认已解决
              </button>
              <button
                class="btn danger"
                type="button"
                disabled
                :title="completed ? '已完成，无法申请返工' : '返工申请接口待后端提供'"
                @click.stop
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
                :disabled="existingScore !== null"
                @click="score = n"
              >
                ★
              </button>
            </div>
            <textarea
              v-model="ratingContent"
              class="textarea"
              placeholder="说说本次维修体验（可选）"
              :disabled="existingScore !== null"
            ></textarea>
            <div v-if="existingScore !== null" class="muted rating-done">已评价 {{ existingScore }} 星</div>
            <button
              v-else
              class="btn primary submit-rating"
              type="button"
              disabled
              title="评价接口待后端提供"
              @click.stop
            >
              提交评价
            </button>
          </div>
        </section>
        <aside class="card">
          <h2>维修轨迹</h2>
          <OrderTimeline :flows="detail.flows" />
        </aside>
      </div>
    </PageState>
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
