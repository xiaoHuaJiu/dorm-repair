<script setup lang="ts">
import { computed } from 'vue'
import type { AreaTreeNode } from '@/types/config'
import { AREA_CHILD_TYPE, AREA_TYPE, areaTypeLabel } from '@/constants/config'

/**
 * 四级区域树递归节点。结构对齐原型 `admin-regions`：
 * 单位/校区 → 区域 → 楼栋 → 房间；房间为叶子节点，其余层级默认折叠。
 */
const props = defineProps<{
  node: AreaTreeNode
  /** 编辑态显示增删操作按钮。 */
  editing: boolean
}>()

const emit = defineEmits<{
  /** 点击“+”，请求在当前节点下新增下级。 */
  add: [node: AreaTreeNode]
  /** 点击“−”，请求删除当前节点。 */
  remove: [node: AreaTreeNode]
}>()

const childType = computed(() => AREA_CHILD_TYPE[props.node.areaType])
const isLeaf = computed(() => props.node.areaType === AREA_TYPE.ROOM)
const hasChildren = computed(() => props.node.children.length > 0)
/** 后端未提供删除接口：仅无子节点时禁用删除按钮并用 title 提示。 */
const removeDisabled = computed(() => !hasChildren.value)
</script>

<template>
  <li>
    <div v-if="isLeaf" class="region-node-row leaf">
      <span class="region-node-copy">
        <small>{{ areaTypeLabel(node.areaType) }}</small>
        <strong>
          {{ node.areaName }}
          <span v-if="node.status !== 1" class="tag amber">停用</span>
        </strong>
      </span>
      <span :hidden="!editing" class="region-actions">
        <button
          class="region-icon remove"
          type="button"
          :disabled="removeDisabled"
          :title="removeDisabled ? '后端暂无删除接口，暂不支持删除节点' : '删除节点'"
          :aria-label="`删除${node.areaName}`"
          @click="emit('remove', node)"
        >−</button>
      </span>
    </div>

    <details v-else class="region-branch">
      <summary class="region-node-row">
        <span class="region-node-main">
          <i class="region-chevron" aria-hidden="true"></i>
          <span class="region-node-copy">
            <small>{{ areaTypeLabel(node.areaType) }}</small>
            <strong>
              {{ node.areaName }}
              <span v-if="node.status !== 1" class="tag amber">停用</span>
            </strong>
          </span>
        </span>
        <span :hidden="!editing" class="region-actions">
          <button
            v-if="childType"
            class="region-icon add"
            type="button"
            :title="`添加${areaTypeLabel(childType)}`"
            :aria-label="`在${node.areaName}下添加${areaTypeLabel(childType)}`"
            @click="emit('add', node)"
          >+</button>
          <button
            class="region-icon remove"
            type="button"
            :disabled="removeDisabled"
            :title="removeDisabled ? '后端暂无删除接口，暂不支持删除节点' : '删除节点'"
            :aria-label="`删除${node.areaName}`"
            @click="emit('remove', node)"
          >−</button>
        </span>
      </summary>
      <ul v-if="hasChildren" class="region-tree">
        <RegionTreeNode
          v-for="child in node.children"
          :key="child.id"
          :node="child"
          :editing="editing"
          @add="emit('add', $event)"
          @remove="emit('remove', $event)"
        />
      </ul>
      <div v-else class="region-empty">暂无下级节点</div>
    </details>
  </li>
</template>
