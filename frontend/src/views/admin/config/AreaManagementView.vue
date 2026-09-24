<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminAppShell from '@/layouts/AdminAppShell.vue'
import PageState from '@/components/common/PageState.vue'
import FormDialog from '@/components/common/FormDialog.vue'
import RegionTreeNode from '@/components/admin/RegionTreeNode.vue'
import { adminAreaTree, createArea } from '@/api/area'
import type { AreaTreeNode } from '@/types/config'
import { AREA_CHILD_TYPE, AREA_TYPE, areaTypeLabel } from '@/constants/config'
import { notifySuccess } from '@/api/successNotifier'
import { notifyError } from '@/api/errorNotifier'

const loading = ref(true)
const error = ref<string | null>(null)
const tree = ref<AreaTreeNode[]>([])

/** 已展开节点 id 集合；树刷新重挂载后据此恢复展开状态，避免新增后整树收拢。 */
const expandedIds = reactive(new Set<number>())

/** 浏览态隐藏增删按钮，点击“编辑”进入维护态。 */
const editing = ref(false)

interface AddDialogState {
  visible: boolean
  /** 父节点；null 表示新增根节点（学校/单位）。 */
  parent: AreaTreeNode | null
  childType: number
  areaCode: string
  areaName: string
  sortNo: string
  remark: string
}

const addDialog = reactive<AddDialogState>({
  visible: false,
  parent: null,
  childType: AREA_TYPE.CAMPUS,
  areaCode: '',
  areaName: '',
  sortNo: '0',
  remark: '',
})

const submitting = ref(false)
const dialogError = ref('')

async function loadTree() {
  loading.value = true
  error.value = null
  try {
    tree.value = await adminAreaTree()
  } catch (cause) {
    // 请求层已统一提示，这里只保留页面错误态供重试。
    error.value = cause instanceof Error ? cause.message : '区域树加载失败'
  } finally {
    loading.value = false
  }
}

function openAdd(parent: AreaTreeNode | null) {
  addDialog.parent = parent
  addDialog.childType = parent ? (AREA_CHILD_TYPE[parent.areaType] ?? AREA_TYPE.CAMPUS) : AREA_TYPE.CAMPUS
  addDialog.areaCode = ''
  addDialog.areaName = ''
  addDialog.sortNo = '0'
  addDialog.remark = ''
  dialogError.value = ''
  addDialog.visible = true
}

function onRemove(node: AreaTreeNode) {
  // 后端未提供删除接口；有下级时按原型交互提示，无下级时按钮已禁用。
  if (node.children.length > 0) {
    notifyError('请先删除下级节点')
  }
}

async function submitAdd() {
  const parent = addDialog.parent
  const name = addDialog.areaName.trim()
  const code = addDialog.areaCode.trim()
  const sortNo = Number(addDialog.sortNo)

  if (!name) return void (dialogError.value = '名称不能为空')
  if (name.length > 100) return void (dialogError.value = '名称不能超过 100 个字符')
  if (!code) return void (dialogError.value = '位置编码不能为空')
  if (code.length > 50) return void (dialogError.value = '位置编码不能超过 50 个字符')
  if (!Number.isInteger(sortNo)) return void (dialogError.value = '排序必须是整数')
  if (addDialog.remark.length > 500) return void (dialogError.value = '备注不能超过 500 个字符')

  submitting.value = true
  dialogError.value = ''
  try {
    await createArea({
      parentId: parent ? parent.id : 0,
      areaCode: code,
      areaName: name,
      areaType: addDialog.childType,
      sortNo,
      remark: addDialog.remark.trim() || undefined,
    })
    addDialog.visible = false
    notifySuccess(parent ? `已添加${areaTypeLabel(addDialog.childType)}节点` : '已新增学校/单位')
    // 刷新前记录父节点展开，保证新节点在刷新后立即可见。
    if (parent) expandedIds.add(parent.id)
    await loadTree()
  } catch (cause) {
    dialogError.value = cause instanceof Error ? cause.message : '添加失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadTree)
</script>

<template>
  <AdminAppShell title="区域配置" subtitle="维护学校/单位、区域、楼栋与房间">
    <template #head-action>
      <button
        class="btn"
        :class="editing ? 'primary' : 'outline'"
        type="button"
        :aria-pressed="editing"
        @click="editing = !editing"
      >
        {{ editing ? '完成' : '编辑' }}
      </button>
    </template>

    <PageState :loading="loading" :error="error" :empty="!error && tree.length === 0" empty-text="暂无区域数据"
      @retry="loadTree">
      <section class="card region-panel">
        <div class="region-tree-wrap">
          <ul class="region-tree">
            <RegionTreeNode
              v-for="node in tree"
              :key="node.id"
              :node="node"
              :editing="editing"
              :expanded-ids="expandedIds"
              @add="openAdd"
              @remove="onRemove"
            />
          </ul>
        </div>
        <button :hidden="!editing" class="region-root-add" type="button" @click="openAdd(null)">
          + 新增学校/单位
        </button>
      </section>
    </PageState>

    <FormDialog
      v-model:visible="addDialog.visible"
      :title="`添加${areaTypeLabel(addDialog.childType)}`"
      :confirm-text="`添加${areaTypeLabel(addDialog.childType)}`"
      :loading="submitting"
      :error="dialogError"
      @submit="submitAdd"
    >
      <div class="form">
        <p class="form-hint">节点类型固定为“{{ areaTypeLabel(addDialog.childType) }}”，保存后不可改变层级。</p>
        <div class="field">
          <label class="required" for="region-name">{{ areaTypeLabel(addDialog.childType) }}名称</label>
          <input
            id="region-name"
            v-model="addDialog.areaName"
            class="input"
            :placeholder="`请输入${areaTypeLabel(addDialog.childType)}名称`"
            maxlength="100"
          >
        </div>
        <div class="field">
          <label class="required" for="region-code">位置编码</label>
          <input id="region-code" v-model="addDialog.areaCode" class="input" placeholder="请输入位置编码" maxlength="50">
        </div>
        <div class="field">
          <label class="required" for="region-sort">排序</label>
          <input id="region-sort" v-model="addDialog.sortNo" class="input" type="number" step="1">
        </div>
        <div class="field">
          <label for="region-remark">备注</label>
          <textarea id="region-remark" v-model="addDialog.remark" class="textarea" maxlength="500" />
        </div>
      </div>
    </FormDialog>
  </AdminAppShell>
</template>
