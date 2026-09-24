<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import FormDialog from '@/components/common/FormDialog.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import WorkerCard from '@/components/admin/WorkerCard.vue'
import {
  createWorker,
  listWorkerAreaScopes,
  listWorkerFaultTypes,
  pageWorkers,
  saveWorkerAreaScopes,
  saveWorkerFaultTypes,
  updateAccountStatus,
  updateWorker,
} from '@/api/worker'
import { adminAreaTree } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import type { AreaTreeNode, FaultTypeItem, WorkerDetail } from '@/types/config'
import { AREA_TYPE } from '@/constants/config'
import { notifySuccess } from '@/api/successNotifier'

const PAGE_SIZE = 10

const loading = ref(true)
const error = ref<string | null>(null)
const records = ref<WorkerDetail[]>([])
const total = ref(0)
const pageNum = ref(1)

/** 全局基础数据：全部启用故障类型 + 管理员区域树（含停用）。 */
const faultTypeOptions = ref<FaultTypeItem[]>([])
const areaTree = ref<AreaTreeNode[]>([])

/** 区域树 id → 节点映射，用于列表与配置弹窗的名称解析。 */
const areaNodeMap = computed(() => {
  const map = new Map<number, AreaTreeNode>()
  const walk = (nodes: AreaTreeNode[]) => {
    for (const node of nodes) {
      map.set(node.id, node)
      walk(node.children)
    }
  }
  walk(areaTree.value)
  return map
})

function areaNameOf(id: number): string | undefined {
  return areaNodeMap.value.get(id)?.areaName
}

async function loadPage() {
  loading.value = true
  error.value = null
  try {
    const page = await pageWorkers({ pageNum: pageNum.value, pageSize: PAGE_SIZE })
    records.value = page.records
    total.value = page.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '维修人员加载失败'
  } finally {
    loading.value = false
  }
}

async function loadBaseData() {
  // 基础数据失败不阻塞列表，配置弹窗打开时再提示。
  try {
    const [faultTypes, tree] = await Promise.all([listEnabledFaultTypes(), adminAreaTree()])
    faultTypeOptions.value = faultTypes
    areaTree.value = tree
  } catch {
    /* 忽略：列表仍可展示，配置弹窗使用空选项并提示。 */
  }
}

// ---------- 新增 / 编辑 ----------

type CreateMode = 'new-account' | 'bind-account'

interface EditorState {
  visible: boolean
  /** null 表示新增，否则编辑已有人员。 */
  editing: WorkerDetail | null
  mode: CreateMode
  username: string
  password: string
  realName: string
  phone: string
  existingUserId: string
  workerNo: string
  remark: string
}

const editor = reactive<EditorState>({
  visible: false,
  editing: null,
  mode: 'new-account',
  username: '',
  password: '',
  realName: '',
  phone: '',
  existingUserId: '',
  workerNo: '',
  remark: '',
})

const submitting = ref(false)
const dialogError = ref('')

const editorTitle = computed(() => (editor.editing ? '编辑维修人员' : '新增维修人员'))

function openCreate() {
  editor.editing = null
  editor.mode = 'new-account'
  editor.username = ''
  editor.password = ''
  editor.realName = ''
  editor.phone = ''
  editor.existingUserId = ''
  editor.workerNo = ''
  editor.remark = ''
  dialogError.value = ''
  editor.visible = true
}

function openEdit(worker: WorkerDetail) {
  editor.editing = worker
  editor.mode = 'new-account'
  editor.username = worker.username ?? ''
  editor.password = ''
  editor.realName = worker.realName ?? ''
  editor.phone = worker.phone ?? ''
  editor.existingUserId = ''
  editor.workerNo = worker.workerNo
  editor.remark = worker.remark ?? ''
  dialogError.value = ''
  editor.visible = true
}

const PHONE_PATTERN = /^$|^1[3-9]\d{9}$/

async function submitEditor() {
  const workerNo = editor.workerNo.trim()

  if (!workerNo) return void (dialogError.value = '维修人员编号不能为空')
  if (workerNo.length > 50) return void (dialogError.value = '维修人员编号不能超过 50 个字符')
  if (editor.remark.length > 500) return void (dialogError.value = '备注不能超过 500 个字符')

  submitting.value = true
  dialogError.value = ''
  try {
    if (editor.editing) {
      await updateWorker(editor.editing.workerId, { workerNo, remark: editor.remark.trim() || undefined })
      notifySuccess('已保存修改')
    } else if (editor.mode === 'bind-account') {
      const userId = Number(editor.existingUserId)
      if (!Number.isInteger(userId) || userId <= 0) {
        submitting.value = false
        return void (dialogError.value = '请输入正确的账号 ID')
      }
      await createWorker({ existingUserId: userId, workerNo, remark: editor.remark.trim() || undefined })
      notifySuccess('已创建维修人员并绑定账号')
    } else {
      const username = editor.username.trim()
      const realName = editor.realName.trim()
      if (!username) return void (dialogError.value = '用户名不能为空')
      if (username.length > 100) return void (dialogError.value = '用户名不能超过 100 个字符')
      if (!editor.password) return void (dialogError.value = '密码不能为空')
      if (editor.password.length < 8 || editor.password.length > 64) {
        return void (dialogError.value = '密码长度必须为 8 到 64 个字符')
      }
      if (!realName) return void (dialogError.value = '姓名不能为空')
      if (realName.length > 100) return void (dialogError.value = '姓名不能超过 100 个字符')
      if (!PHONE_PATTERN.test(editor.phone.trim())) return void (dialogError.value = '手机号格式不正确')
      await createWorker({
        username,
        password: editor.password,
        realName,
        phone: editor.phone.trim() || undefined,
        workerNo,
        remark: editor.remark.trim() || undefined,
      })
      notifySuccess('已创建维修人员账号')
    }
    editor.visible = false
    await loadPage()
  } catch (cause) {
    dialogError.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    submitting.value = false
  }
}

// ---------- 账号启停 ----------

const accountTarget = ref<WorkerDetail | null>(null)
const accountDialogVisible = ref(false)
const accountSubmitting = ref(false)

function onToggleAccount(worker: WorkerDetail) {
  accountTarget.value = worker
  accountDialogVisible.value = true
}

async function confirmToggleAccount() {
  const worker = accountTarget.value
  if (!worker) return
  accountSubmitting.value = true
  try {
    const next = worker.userStatus === 1 ? 0 : 1
    await updateAccountStatus(worker.workerId, next)
    accountDialogVisible.value = false
    notifySuccess(next === 1 ? '账号已启用' : '账号已停用')
    await loadPage()
  } finally {
    accountSubmitting.value = false
  }
}

// ---------- 技能与区域配置 ----------

interface ScopeRow {
  campusId: number | null
  areaId: number | null
  buildingId: number | null
}

const configTarget = ref<WorkerDetail | null>(null)
const configDialogVisible = ref(false)
const configLoading = ref(false)
const configSaving = ref(false)
const configError = ref('')
const selectedFaultTypeIds = ref<number[]>([])
const scopeRows = ref<ScopeRow[]>([])

/** 区域树中指定类型的顶层候选（按树顺序）。 */
function nodesOfType(type: number): AreaTreeNode[] {
  const result: AreaTreeNode[] = []
  const walk = (nodes: AreaTreeNode[]) => {
    for (const node of nodes) {
      if (node.areaType === type) result.push(node)
      walk(node.children)
    }
  }
  walk(areaTree.value)
  return result
}

const campusOptions = computed(() => nodesOfType(AREA_TYPE.CAMPUS))

function areaOptionsOf(row: ScopeRow): AreaTreeNode[] {
  if (row.campusId == null) return []
  return areaNodeMap.value.get(row.campusId)?.children ?? []
}

function buildingOptionsOf(row: ScopeRow): AreaTreeNode[] {
  if (row.areaId == null) return []
  return areaNodeMap.value.get(row.areaId)?.children ?? []
}

function onCampusChange(row: ScopeRow) {
  row.areaId = null
  row.buildingId = null
}

function onAreaChange(row: ScopeRow) {
  row.buildingId = null
}

function addScopeRow() {
  scopeRows.value.push({ campusId: null, areaId: null, buildingId: null })
}

function removeScopeRow(index: number) {
  scopeRows.value.splice(index, 1)
}

async function openConfig(worker: WorkerDetail) {
  configTarget.value = worker
  configDialogVisible.value = true
  configLoading.value = true
  configError.value = ''
  selectedFaultTypeIds.value = []
  scopeRows.value = []
  try {
    const [faultTypes, scopes] = await Promise.all([
      listWorkerFaultTypes(worker.workerId),
      listWorkerAreaScopes(worker.workerId),
    ])
    selectedFaultTypeIds.value = faultTypes.map((item) => item.faultTypeId)
    scopeRows.value = scopes.map((item) => ({
      campusId: item.campusId,
      areaId: item.areaId,
      buildingId: item.buildingId,
    }))
  } catch (cause) {
    configError.value = cause instanceof Error ? cause.message : '配置加载失败'
  } finally {
    configLoading.value = false
  }
}

function toggleFaultType(id: number) {
  const index = selectedFaultTypeIds.value.indexOf(id)
  if (index >= 0) selectedFaultTypeIds.value.splice(index, 1)
  else selectedFaultTypeIds.value.push(id)
}

async function submitConfig() {
  const worker = configTarget.value
  if (!worker) return
  const invalidScope = scopeRows.value.some((row) => row.campusId == null)
  if (invalidScope) return void (configError.value = '每条负责范围必须选择校区')

  configSaving.value = true
  configError.value = ''
  try {
    await Promise.all([
      saveWorkerFaultTypes(worker.workerId, selectedFaultTypeIds.value),
      saveWorkerAreaScopes(
        worker.workerId,
        scopeRows.value.map((row) => ({
          campusId: row.campusId as number,
          areaId: row.areaId ?? null,
          buildingId: row.buildingId ?? null,
        })),
      ),
    ])
    configDialogVisible.value = false
    notifySuccess('技能与负责区域已保存')
    await loadPage()
  } catch (cause) {
    configError.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    configSaving.value = false
  }
}

function onPageChange(next: number) {
  pageNum.value = next
  void loadPage()
}

onMounted(() => {
  void loadPage()
  void loadBaseData()
})
</script>

<template>
  <AdminAppShell title="维修人员" subtitle="维护账号、技能、区域与启停状态">
    <template #head-action>
      <button class="btn primary" type="button" @click="openCreate">新增维修人员</button>
    </template>

    <PageState
      :loading="loading"
      :error="error"
      :empty="!error && records.length === 0"
      empty-text="暂无维修人员"
      @retry="loadPage"
    >
      <div class="worker-admin-list">
        <WorkerCard
          v-for="worker in records"
          :key="worker.workerId"
          :worker="worker"
          :fault-type-options="faultTypeOptions"
          :area-name-of="areaNameOf"
          @edit="openEdit"
          @config="openConfig"
          @toggle-account="onToggleAccount"
        />
      </div>
      <PaginationBar :total="total" :page-num="pageNum" :page-size="PAGE_SIZE" @update:page-num="onPageChange" />
    </PageState>

    <!-- 新增 / 编辑维修人员 -->
    <FormDialog
      v-model:visible="editor.visible"
      :title="editorTitle"
      :loading="submitting"
      :error="dialogError"
      @submit="submitEditor"
    >
      <div class="form">
        <div v-if="!editor.editing" class="field">
          <label>账号方式</label>
          <div class="inline-actions">
            <button
              class="btn small"
              :class="editor.mode === 'new-account' ? 'primary' : 'outline'"
              type="button"
              @click="editor.mode = 'new-account'"
            >
              新建账号
            </button>
            <button
              class="btn small"
              :class="editor.mode === 'bind-account' ? 'primary' : 'outline'"
              type="button"
              @click="editor.mode = 'bind-account'"
            >
              绑定已有账号
            </button>
          </div>
        </div>

        <template v-if="editor.mode === 'bind-account' && !editor.editing">
          <div class="field">
            <label class="required" for="worker-user-id">已有账号 ID</label>
            <input id="worker-user-id" v-model="editor.existingUserId" class="input" type="number" step="1">
          </div>
        </template>
        <template v-else>
          <div v-if="!editor.editing" class="field">
            <label class="required" for="worker-username">用户名</label>
            <input id="worker-username" v-model="editor.username" class="input" maxlength="100">
          </div>
          <div v-if="!editor.editing" class="field">
            <label class="required" for="worker-password">密码（8 到 64 个字符）</label>
            <input id="worker-password" v-model="editor.password" class="input" type="password" minlength="8" maxlength="64">
          </div>
          <div class="field">
            <label :class="{ required: !editor.editing }" for="worker-real-name">姓名</label>
            <input id="worker-real-name" v-model="editor.realName" class="input" maxlength="100">
          </div>
          <div class="field">
            <label for="worker-phone">手机号</label>
            <input id="worker-phone" v-model="editor.phone" class="input" placeholder="选填，格式 1 开头 11 位">
          </div>
        </template>

        <div class="field">
          <label class="required" for="worker-no">维修人员编号</label>
          <input id="worker-no" v-model="editor.workerNo" class="input" maxlength="50">
        </div>
        <div class="field">
          <label for="worker-remark">备注</label>
          <textarea id="worker-remark" v-model="editor.remark" class="textarea" maxlength="500" />
        </div>
      </div>
    </FormDialog>

    <!-- 账号启停确认 -->
    <ConfirmDialog
      v-model:visible="accountDialogVisible"
      :title="accountTarget?.userStatus === 1 ? '停用账号' : '启用账号'"
      :content="accountTarget?.userStatus === 1
        ? `确认停用“${accountTarget?.realName || ''}”的登录账号吗？停用后该账号无法登录。`
        : `确认启用“${accountTarget?.realName || ''}”的登录账号吗？`"
      :danger="accountTarget?.userStatus === 1"
      :confirm-text="accountTarget?.userStatus === 1 ? '确认停用' : '确认启用'"
      :loading="accountSubmitting"
      @confirm="confirmToggleAccount"
    />

    <!-- 技能与负责区域配置 -->
    <FormDialog
      v-model:visible="configDialogVisible"
      :title="`配置技能与区域 - ${configTarget?.realName ?? ''}`"
      confirm-text="保存配置"
      :loading="configSaving"
      :error="configError"
      @submit="submitConfig"
    >
      <div v-if="configLoading" class="empty">
        <p>配置加载中…</p>
      </div>
      <div v-else class="form">
        <fieldset class="config-group">
          <legend>维修技能</legend>
          <p v-if="faultTypeOptions.length === 0" class="hint">暂无启用的故障类型，请先到故障类型管理添加。</p>
          <label v-for="faultType in faultTypeOptions" :key="faultType.id" class="check-row">
            <input
              type="checkbox"
              :checked="selectedFaultTypeIds.includes(faultType.id)"
              @change="toggleFaultType(faultType.id)"
            >
            <span>{{ faultType.typeName }}</span>
          </label>
        </fieldset>

        <fieldset class="config-group">
          <legend>负责范围</legend>
          <div v-if="scopeRows.length === 0" class="hint">未配置负责范围</div>
          <div v-for="(row, index) in scopeRows" :key="index" class="scope-row">
            <div class="scope-selects">
              <select v-model="row.campusId" class="select" aria-label="校区" @change="onCampusChange(row)">
                <option :value="null" disabled>选择校区</option>
                <option v-for="campus in campusOptions" :key="campus.id" :value="campus.id">
                  {{ campus.areaName }}
                </option>
              </select>
              <select v-model="row.areaId" class="select" aria-label="区域" :disabled="row.campusId == null"
                @change="onAreaChange(row)">
                <option :value="null" disabled>选择区域（可选）</option>
                <option v-for="area in areaOptionsOf(row)" :key="area.id" :value="area.id">
                  {{ area.areaName }}
                </option>
              </select>
              <select v-model="row.buildingId" class="select" aria-label="楼栋" :disabled="row.areaId == null">
                <option :value="null" disabled>选择楼栋（可选）</option>
                <option v-for="building in buildingOptionsOf(row)" :key="building.id" :value="building.id">
                  {{ building.areaName }}
                </option>
              </select>
            </div>
            <button class="btn danger small" type="button" @click="removeScopeRow(index)">删除</button>
          </div>
          <button class="btn outline small scope-add" type="button" @click="addScopeRow">+ 添加负责范围</button>
        </fieldset>
      </div>
    </FormDialog>
  </AdminAppShell>
</template>

<style scoped>
.config-group {
  margin: 0;
  padding: 14px;
  border: 1px solid var(--dr-color-border);
  border-radius: 12px;
}

.config-group legend {
  padding: 0 6px;
  font-weight: 700;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  cursor: pointer;
}

.scope-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 10px;
}

.scope-selects {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
}

.scope-add {
  width: 100%;
  border-style: dashed;
}

@media (max-width: 600px) {
  .scope-row {
    align-items: stretch;
    flex-direction: column;
  }

  .scope-selects {
    grid-template-columns: 1fr;
  }
}
</style>
