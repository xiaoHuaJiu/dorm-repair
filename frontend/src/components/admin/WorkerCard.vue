<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FaultTypeItem, WorkerDetail } from '@/types/config'
import { listWorkerAreaScopes, listWorkerFaultTypes } from '@/api/worker'
import { workStatusLabel, workStatusTone } from '@/constants/config'

const props = defineProps<{
  worker: WorkerDetail
  /** 全部启用故障类型，用于技能 ID → 名称映射。 */
  faultTypeOptions: FaultTypeItem[]
  /** 区域节点 ID → 名称映射（管理员区域树）。 */
  areaNameOf: (id: number) => string | undefined
}>()

const emit = defineEmits<{
  edit: [worker: WorkerDetail]
  /** 打开技能与负责区域配置弹窗。 */
  config: [worker: WorkerDetail]
  'toggle-account': [worker: WorkerDetail]
}>()

const skillIds = ref<number[]>([])
const scopes = ref<{ campusId: number; areaId: number | null; buildingId: number | null }[]>([])
const loadingDetail = ref(true)
const detailFailed = ref(false)

function skillsLabel(): string {
  if (detailFailed.value) return '—'
  if (loadingDetail.value) return '加载中…'
  if (skillIds.value.length === 0) return '未配置'
  const names = skillIds.value
    .map((id) => props.faultTypeOptions.find((item) => item.id === id)?.typeName)
    .filter((name): name is string => Boolean(name))
  return names.length > 0 ? names.join('、') : '未配置'
}

function areasLabel(): string {
  if (detailFailed.value) return '—'
  if (loadingDetail.value) return '加载中…'
  if (scopes.value.length === 0) return '未配置'
  const names = scopes.value.map((scope) => {
    const parts = [props.areaNameOf(scope.campusId)]
    if (scope.areaId != null) parts.push(props.areaNameOf(scope.areaId))
    if (scope.buildingId != null) parts.push(props.areaNameOf(scope.buildingId))
    return parts.filter((name): name is string => Boolean(name)).join('/')
  })
  return names.filter(Boolean).join('、') || '未配置'
}

onMounted(async () => {
  try {
    const [faultTypes, areaScopes] = await Promise.all([
      listWorkerFaultTypes(props.worker.workerId),
      listWorkerAreaScopes(props.worker.workerId),
    ])
    skillIds.value = faultTypes.map((item) => item.faultTypeId)
    scopes.value = areaScopes.map((item) => ({
      campusId: item.campusId,
      areaId: item.areaId,
      buildingId: item.buildingId,
    }))
  } catch {
    detailFailed.value = true
  } finally {
    loadingDetail.value = false
  }
})
</script>

<template>
  <article class="card admin-worker-card">
    <div>
      <div class="section-title">
        <h2>{{ worker.realName || '未填写姓名' }}</h2>
        <span class="tag" :class="workStatusTone(worker.workStatus)">{{ workStatusLabel(worker.workStatus) }}</span>
      </div>
      <p><strong>账号：</strong>{{ worker.username || '未绑定' }}</p>
      <p><strong>编号：</strong>{{ worker.workerNo }}</p>
      <p><strong>技能：</strong>{{ skillsLabel() }}</p>
      <p><strong>区域：</strong>{{ areasLabel() }}</p>
      <p class="muted">当前剩余 — 张工单</p>
    </div>
    <div class="inline-actions">
      <button class="btn outline small" type="button" @click="emit('edit', worker)">编辑</button>
      <button class="btn outline small" type="button" @click="emit('config', worker)">技能与区域</button>
      <button
        class="btn small"
        :class="worker.userStatus === 1 ? 'danger' : 'primary'"
        type="button"
        @click="emit('toggle-account', worker)"
      >
        {{ worker.userStatus === 1 ? '停用账号' : '启用账号' }}
      </button>
    </div>
  </article>
</template>

<style scoped>
.admin-worker-card p {
  margin: 4px 0;
  font-size: 14px;
}

.admin-worker-card .section-title {
  justify-content: flex-start;
  gap: 10px;
}

.admin-worker-card .muted {
  color: var(--dr-color-text-muted);
  font-size: 13px;
}
</style>
