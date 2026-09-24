<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import FormDialog from '@/components/common/FormDialog.vue'
import { createFaultType, pageFaultTypes, updateFaultType, updateFaultTypeStatus } from '@/api/faultType'
import type { FaultTypeItem } from '@/types/config'
import { COMMON_STATUS, commonStatusLabel } from '@/constants/config'
import { notifySuccess } from '@/api/successNotifier'

const PAGE_SIZE = 9

const loading = ref(true)
const error = ref<string | null>(null)
const records = ref<FaultTypeItem[]>([])
const total = ref(0)
const pageNum = ref(1)

/** 启停请求中的类型 ID 集合，用于按钮防重复点击。 */
const togglingIds = ref<Set<number>>(new Set())

interface EditorState {
  visible: boolean
  /** null 表示新增，否则编辑已有类型。 */
  editing: FaultTypeItem | null
  typeCode: string
  typeName: string
  sortNo: string
  remark: string
}

const editor = reactive<EditorState>({
  visible: false,
  editing: null,
  typeCode: '',
  typeName: '',
  sortNo: '0',
  remark: '',
})

const submitting = ref(false)
const dialogError = ref('')

const editorTitle = computed(() => (editor.editing ? '修改故障类型' : '新增故障类型'))

async function loadPage() {
  loading.value = true
  error.value = null
  try {
    const page = await pageFaultTypes({ pageNum: pageNum.value, pageSize: PAGE_SIZE })
    records.value = page.records
    total.value = page.total
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '故障类型加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editor.editing = null
  editor.typeCode = ''
  editor.typeName = ''
  editor.sortNo = '0'
  editor.remark = ''
  dialogError.value = ''
  editor.visible = true
}

function openEdit(item: FaultTypeItem) {
  editor.editing = item
  editor.typeCode = item.typeCode
  editor.typeName = item.typeName
  editor.sortNo = String(item.sortNo)
  editor.remark = item.remark ?? ''
  dialogError.value = ''
  editor.visible = true
}

function onPageChange(next: number) {
  pageNum.value = next
  void loadPage()
}

async function toggleStatus(item: FaultTypeItem) {
  if (togglingIds.value.has(item.id)) return
  togglingIds.value.add(item.id)
  try {
    const next = item.status === COMMON_STATUS.ENABLED ? COMMON_STATUS.DISABLED : COMMON_STATUS.ENABLED
    await updateFaultTypeStatus(item.id, next)
    notifySuccess(next === COMMON_STATUS.ENABLED ? '已启用' : '已停用')
    await loadPage()
  } finally {
    togglingIds.value.delete(item.id)
  }
}

async function submitEditor() {
  const name = editor.typeName.trim()
  const code = editor.typeCode.trim()
  const sortNo = Number(editor.sortNo)

  if (!editor.editing && !code) return void (dialogError.value = '类型编码不能为空')
  if (code.length > 50) return void (dialogError.value = '类型编码不能超过 50 个字符')
  if (!name) return void (dialogError.value = '类型名称不能为空')
  if (name.length > 100) return void (dialogError.value = '类型名称不能超过 100 个字符')
  if (!Number.isInteger(sortNo)) return void (dialogError.value = '排序必须是整数')
  if (editor.remark.length > 500) return void (dialogError.value = '备注不能超过 500 个字符')

  submitting.value = true
  dialogError.value = ''
  try {
    if (editor.editing) {
      await updateFaultType(editor.editing.id, {
        typeName: name,
        sortNo,
        remark: editor.remark.trim() || undefined,
      })
      notifySuccess('已保存修改')
    } else {
      await createFaultType({
        typeCode: code,
        typeName: name,
        sortNo,
        remark: editor.remark.trim() || undefined,
      })
      notifySuccess('已新增故障类型')
    }
    editor.visible = false
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
  <AdminAppShell title="故障类型管理" subtitle="维护系统可受理的维修类型">
    <template #head-action>
      <button class="btn primary" type="button" @click="openCreate">新增故障类型</button>
    </template>

    <PageState
      :loading="loading"
      :error="error"
      :empty="!error && records.length === 0"
      empty-text="暂无故障类型"
      @retry="loadPage"
    >
      <div class="fault-grid">
        <article v-for="item in records" :key="item.id" class="card fault-card">
          <div>
            <strong>{{ item.typeName }}</strong>
            <span class="tag" :class="item.status === COMMON_STATUS.ENABLED ? 'green' : 'amber'" style="margin-left: 8px">
              {{ commonStatusLabel(item.status) }}
            </span>
            <p class="muted-card">{{ item.typeCode }}</p>
          </div>
          <div class="inline-actions">
            <button class="btn outline small" type="button" @click="openEdit(item)">修改</button>
            <button
              class="btn small"
              :class="item.status === COMMON_STATUS.ENABLED ? 'danger' : 'primary'"
              type="button"
              :disabled="togglingIds.has(item.id)"
              @click="toggleStatus(item)"
            >
              {{ item.status === COMMON_STATUS.ENABLED ? '停用' : '启用' }}
            </button>
            <button
              class="btn danger small"
              type="button"
              disabled
              title="后端暂无删除接口，暂不支持删除"
            >
              删除
            </button>
          </div>
        </article>
      </div>
      <PaginationBar :total="total" :page-num="pageNum" :page-size="PAGE_SIZE" @update:page-num="onPageChange" />
    </PageState>

    <FormDialog
      v-model:visible="editor.visible"
      :title="editorTitle"
      :loading="submitting"
      :error="dialogError"
      @submit="submitEditor"
    >
      <div class="form">
        <div v-if="editor.editing" class="field">
          <label for="fault-code">类型编码（创建后不可修改）</label>
          <input id="fault-code" class="input" :value="editor.typeCode" disabled>
        </div>
        <div v-else class="field">
          <label class="required" for="fault-code">类型编码</label>
          <input id="fault-code" v-model="editor.typeCode" class="input" placeholder="请输入类型编码" maxlength="50">
        </div>
        <div class="field">
          <label class="required" for="fault-name">类型名称</label>
          <input id="fault-name" v-model="editor.typeName" class="input" placeholder="请输入类型名称" maxlength="100">
        </div>
        <div class="field">
          <label class="required" for="fault-sort">排序</label>
          <input id="fault-sort" v-model="editor.sortNo" class="input" type="number" step="1">
        </div>
        <div class="field">
          <label for="fault-remark">备注</label>
          <textarea id="fault-remark" v-model="editor.remark" class="textarea" maxlength="500" />
        </div>
      </div>
    </FormDialog>
  </AdminAppShell>
</template>

<style scoped>
.muted-card {
  margin: 3px 0 0;
  color: var(--dr-color-text-muted);
  font-size: 12px;
}
</style>
