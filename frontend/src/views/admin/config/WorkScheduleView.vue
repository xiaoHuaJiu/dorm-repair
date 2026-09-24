<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import FormDialog from '@/components/common/FormDialog.vue'
import { createWorkSchedule, pageWorkSchedules, updateWorkScheduleStatus } from '@/api/workSchedule'
import type { WorkScheduleItem } from '@/types/config'
import { COMMON_STATUS } from '@/constants/config'
import { notifySuccess } from '@/api/successNotifier'

const PAGE_SIZE = 10

const loading = ref(true)
const error = ref<string | null>(null)
const records = ref<WorkScheduleItem[]>([])
const total = ref(0)
const pageNum = ref(1)

const togglingIds = ref<Set<number>>(new Set())

interface EditorState {
  visible: boolean
  scheduleName: string
  startDate: string
  endDate: string
  workStartTime: string
  workEndTime: string
  remark: string
}

const editor = reactive<EditorState>({
  visible: false,
  scheduleName: '',
  startDate: '',
  endDate: '',
  workStartTime: '',
  workEndTime: '',
  remark: '',
})

const submitting = ref(false)
const dialogError = ref('')

async function loadPage() {
  loading.value = true
  error.value = null
  try {
    const page = await pageWorkSchedules({ pageNum: pageNum.value, pageSize: PAGE_SIZE })
    records.value = page.records
    total.value = page.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '工作时间方案加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editor.scheduleName = ''
  editor.startDate = ''
  editor.endDate = ''
  editor.workStartTime = ''
  editor.workEndTime = ''
  editor.remark = ''
  dialogError.value = ''
  editor.visible = true
}

function onPageChange(next: number) {
  pageNum.value = next
  void loadPage()
}

async function toggleStatus(item: WorkScheduleItem) {
  if (togglingIds.value.has(item.id)) return
  togglingIds.value.add(item.id)
  try {
    const next = item.status === COMMON_STATUS.ENABLED ? COMMON_STATUS.DISABLED : COMMON_STATUS.ENABLED
    await updateWorkScheduleStatus(item.id, next)
    notifySuccess(next === COMMON_STATUS.ENABLED ? '方案已启用' : '方案已停用')
    await loadPage()
  } finally {
    togglingIds.value.delete(item.id)
  }
}

async function submitCreate() {
  const name = editor.scheduleName.trim()

  if (!name) return void (dialogError.value = '方案名称不能为空')
  if (name.length > 100) return void (dialogError.value = '方案名称不能超过 100 个字符')
  if (!editor.startDate || !editor.endDate) return void (dialogError.value = '请选择开始和结束日期')
  if (editor.endDate < editor.startDate) return void (dialogError.value = '结束日期不能早于开始日期')
  if (!editor.workStartTime || !editor.workEndTime) return void (dialogError.value = '请选择上班和下班时间')
  if (editor.workEndTime <= editor.workStartTime) return void (dialogError.value = '下班时间必须晚于上班时间')
  if (editor.remark.length > 500) return void (dialogError.value = '备注不能超过 500 个字符')

  submitting.value = true
  dialogError.value = ''
  try {
    await createWorkSchedule({
      scheduleName: name,
      startDate: editor.startDate,
      endDate: editor.endDate,
      workStartTime: editor.workStartTime,
      workEndTime: editor.workEndTime,
      status: COMMON_STATUS.ENABLED,
      remark: editor.remark.trim() || undefined,
    })
    editor.visible = false
    notifySuccess('已新增时间方案')
    await loadPage()
  } catch (cause) {
    dialogError.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <AdminAppShell title="工作时间配置" subtitle="同一时间只启用一套工作时间方案">
    <template #head-action>
      <button class="btn primary" type="button" @click="openCreate">新增时间方案</button>
    </template>

    <PageState
      :loading="loading"
      :error="error"
      :empty="!error && records.length === 0"
      empty-text="暂无工作时间方案"
      @retry="loadPage"
    >
      <div class="schedule-list">
        <article
          v-for="item in records"
          :key="item.id"
          class="card schedule-card"
          :class="{ enabled: item.status === COMMON_STATUS.ENABLED }"
        >
          <div>
            <span class="tag" :class="item.status === COMMON_STATUS.ENABLED ? 'green' : 'amber'">
              {{ item.status === COMMON_STATUS.ENABLED ? '当前启用' : '未启用' }}
            </span>
            <h2 class="schedule-name">{{ item.scheduleName }}</h2>
            <p>{{ item.startDate }} 至 {{ item.endDate }}</p>
            <strong>{{ item.workStartTime }} — {{ item.workEndTime }}</strong>
          </div>
          <div class="inline-actions">
            <button
              class="btn small"
              :class="item.status === COMMON_STATUS.ENABLED ? 'danger' : 'primary'"
              type="button"
              :disabled="togglingIds.has(item.id)"
              @click="toggleStatus(item)"
            >
              {{ item.status === COMMON_STATUS.ENABLED ? '停用' : '启用' }}
            </button>
          </div>
        </article>
      </div>
      <PaginationBar :total="total" :page-num="pageNum" :page-size="PAGE_SIZE" @update:page-num="onPageChange" />
    </PageState>

    <FormDialog
      v-model:visible="editor.visible"
      title="新增时间方案"
      confirm-text="保存方案"
      :loading="submitting"
      :error="dialogError"
      @submit="submitCreate"
    >
      <div class="form">
        <div class="field">
          <label class="required" for="schedule-name">方案名称</label>
          <input id="schedule-name" v-model="editor.scheduleName" class="input" maxlength="100">
        </div>
        <div class="grid two">
          <div class="field">
            <label class="required" for="schedule-start-date">开始日期</label>
            <input id="schedule-start-date" v-model="editor.startDate" class="input" type="date">
          </div>
          <div class="field">
            <label class="required" for="schedule-end-date">结束日期</label>
            <input id="schedule-end-date" v-model="editor.endDate" class="input" type="date">
          </div>
          <div class="field">
            <label class="required" for="schedule-start-time">上班时间</label>
            <input id="schedule-start-time" v-model="editor.workStartTime" class="input" type="time">
          </div>
          <div class="field">
            <label class="required" for="schedule-end-time">下班时间</label>
            <input id="schedule-end-time" v-model="editor.workEndTime" class="input" type="time">
          </div>
        </div>
        <div class="field">
          <label for="schedule-remark">备注</label>
          <textarea id="schedule-remark" v-model="editor.remark" class="textarea" maxlength="500" />
        </div>
      </div>
    </FormDialog>
  </AdminAppShell>
</template>

<style scoped>
.schedule-name {
  margin: 8px 0 4px;
  font-size: 18px;
}

.schedule-card p {
  margin: 0 0 3px;
  color: var(--dr-color-text-muted);
  font-size: 13px;
}
</style>
