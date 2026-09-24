<script setup lang="ts">
import { ref, watch } from 'vue'
import FormDialog from '@/components/common/FormDialog.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ImageUploader from '@/components/common/ImageUploader.vue'
import { areaChildren } from '@/api/area'
import { listEnabledFaultTypes } from '@/api/faultType'
import { checkRepairDuplicate, createRepairOrder } from '@/api/order'
import { notifySuccess } from '@/api/successNotifier'
import type { AreaDetail, FaultTypeItem } from '@/types/config'
import type { SuspectedOrder } from '@/types/order'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: [orderId: number, orderNo: string]
}>()

/** 打开弹窗时生成的幂等键；同一次弹窗会话内提交失败重试保持同一 bizNo。 */
const bizNo = ref('')
/** 疑似重复弹窗与工单列表。 */
const duplicateVisible = ref(false)
const suspectedOrders = ref<SuspectedOrder[]>([])
/** 用户已确认"仍然提交"的请求快照，用于二次弹窗后继续提交。 */
const pendingSubmit = ref<(() => void) | null>(null)

const loading = ref(false)
const error = ref('')

// 四级位置级联。
const campusOptions = ref<AreaDetail[]>([])
const areaOptions = ref<AreaDetail[]>([])
const buildingOptions = ref<AreaDetail[]>([])
const roomOptions = ref<AreaDetail[]>([])
const campusId = ref<number | null>(null)
const areaId = ref<number | null>(null)
const buildingId = ref<number | null>(null)
const roomId = ref<number | null>(null)
const locationLoading = ref(false)

// 故障类型与其他表单字段。
const faultTypes = ref<FaultTypeItem[]>([])
const faultTypeId = ref<number | null>(null)
const locationDetail = ref('')
const problemDescription = ref('')
const contactName = ref('')
const contactPhone = ref('')
const imageUrls = ref<string[]>([])

function newBizNo(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

async function loadCampusOptions() {
  locationLoading.value = true
  try {
    campusOptions.value = await areaChildren(0)
  } finally {
    locationLoading.value = false
  }
}

/** 从 change 事件读取选中的数字值；v-model 在 change 事件处理器之后才更新，因此不能依赖 ref。 */
function selectNumberValue(event: Event): number | null {
  const target = event.target as HTMLSelectElement | null
  if (!target || target.value === '') return null
  const value = Number(target.value)
  return Number.isNaN(value) ? null : value
}

async function onCampusChange(event: Event) {
  areaId.value = null
  buildingId.value = null
  roomId.value = null
  areaOptions.value = []
  buildingOptions.value = []
  roomOptions.value = []
  const next = selectNumberValue(event)
  if (next !== null) areaOptions.value = await areaChildren(next)
}

async function onAreaChange(event: Event) {
  buildingId.value = null
  roomId.value = null
  buildingOptions.value = []
  roomOptions.value = []
  const next = selectNumberValue(event)
  if (next !== null) buildingOptions.value = await areaChildren(next)
}

async function onBuildingChange(event: Event) {
  roomId.value = null
  roomOptions.value = []
  const next = selectNumberValue(event)
  if (next !== null) roomOptions.value = await areaChildren(next)
}

function resetForm() {
  campusId.value = null
  areaId.value = null
  buildingId.value = null
  roomId.value = null
  areaOptions.value = []
  buildingOptions.value = []
  roomOptions.value = []
  faultTypeId.value = null
  locationDetail.value = ''
  problemDescription.value = ''
  contactName.value = ''
  contactPhone.value = ''
  imageUrls.value = []
  error.value = ''
  duplicateVisible.value = false
  suspectedOrders.value = []
  pendingSubmit.value = null
}

watch(
  () => props.visible,
  (visible) => {
    if (!visible) return
    resetForm()
    bizNo.value = newBizNo()
    void loadCampusOptions()
    void listEnabledFaultTypes().then((list) => { faultTypes.value = list })
  },
  { immediate: true },
)

function validate(): string | null {
  if (campusId.value === null || areaId.value === null || buildingId.value === null || roomId.value === null) {
    return '请完整选择校区、区域、楼栋和房间'
  }
  if (faultTypeId.value === null) return '请选择故障类型'
  if (!problemDescription.value.trim()) return '请填写问题描述'
  if (!contactName.value.trim()) return '请填写联系人'
  if (!/^[0-9+() -]{6,30}$/.test(contactPhone.value.trim())) return '请填写正确的联系方式'
  return null
}

function buildPayload(confirmDuplicate: boolean) {
  return {
    bizNo: bizNo.value,
    campusId: campusId.value as number,
    areaId: areaId.value as number,
    buildingId: buildingId.value as number,
    roomId: roomId.value as number,
    faultTypeId: faultTypeId.value as number,
    locationDetail: locationDetail.value.trim() || undefined,
    problemDescription: problemDescription.value.trim(),
    contactName: contactName.value.trim(),
    contactPhone: contactPhone.value.trim(),
    imageUrls: imageUrls.value,
    confirmDuplicate,
  }
}

async function doSubmit(confirmDuplicate: boolean) {
  loading.value = true
  error.value = ''
  try {
    const response = await createRepairOrder(buildPayload(confirmDuplicate))
    if (response.orderId !== null) {
      emit('update:visible', false)
      notifySuccess('报修提交成功')
      emit('created', response.orderId, response.orderNo)
      return
    }
    // 服务端拦截：存在疑似重复工单且尚未确认仍然提交。
    suspectedOrders.value = response.suspectedOrders
    duplicateVisible.value = true
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '提交失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function submit() {
  const message = validate()
  if (message) {
    error.value = message
    return
  }
  loading.value = true
  error.value = ''
  try {
    const response = await checkRepairDuplicate({
      campusId: campusId.value as number,
      areaId: areaId.value as number,
      buildingId: buildingId.value as number,
      roomId: roomId.value as number,
      faultTypeId: faultTypeId.value as number,
    })
    if (response.duplicate) {
      suspectedOrders.value = response.suspectedOrders
      pendingSubmit.value = () => void doSubmit(true)
      duplicateVisible.value = true
      return
    }
    await doSubmit(false)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '提交失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function backToEdit() {
  duplicateVisible.value = false
  pendingSubmit.value = null
  suspectedOrders.value = []
}

function forceSubmit() {
  duplicateVisible.value = false
  const run = pendingSubmit.value
  pendingSubmit.value = null
  suspectedOrders.value = []
  if (run) void run()
}

function close() {
  if (loading.value) return
  emit('update:visible', false)
}
</script>

<template>
  <FormDialog
    :visible="visible"
    title="提交报修"
    confirm-text="提交报修"
    :loading="loading"
    :error="error"
    @update:visible="emit('update:visible', $event)"
    @submit="submit"
    @cancel="close"
  >
    <form class="form repair-form" @submit.prevent>
      <div class="grid two">
        <div class="field">
          <label class="required" for="modal-campus">校区</label>
          <select id="modal-campus" v-model="campusId" class="select" :disabled="locationLoading" @change="onCampusChange">
            <option :value="null">请选择校区</option>
            <option v-for="item in campusOptions" :key="item.id" :value="item.id">{{ item.areaName }}</option>
          </select>
        </div>
        <div class="field">
          <label class="required" for="modal-area">区域</label>
          <select id="modal-area" v-model="areaId" class="select" :disabled="campusId === null" @change="onAreaChange">
            <option :value="null">{{ campusId === null ? '请先选择校区' : '请选择区域' }}</option>
            <option v-for="item in areaOptions" :key="item.id" :value="item.id">{{ item.areaName }}</option>
          </select>
        </div>
        <div class="field">
          <label class="required" for="modal-building">楼栋</label>
          <select id="modal-building" v-model="buildingId" class="select" :disabled="areaId === null" @change="onBuildingChange">
            <option :value="null">{{ areaId === null ? '请先选择区域' : '请选择楼栋' }}</option>
            <option v-for="item in buildingOptions" :key="item.id" :value="item.id">{{ item.areaName }}</option>
          </select>
        </div>
        <div class="field">
          <label class="required" for="modal-room">房间</label>
          <select id="modal-room" v-model="roomId" class="select" :disabled="buildingId === null">
            <option :value="null">{{ buildingId === null ? '请先选择楼栋' : '请选择房间' }}</option>
            <option v-for="item in roomOptions" :key="item.id" :value="item.id">{{ item.areaName }}</option>
          </select>
        </div>
      </div>
      <div class="field">
        <label for="modal-detail">具体位置</label>
        <input id="modal-detail" v-model="locationDetail" class="input" name="detail" placeholder="例如：卫生间洗手池下方" />
      </div>
      <div class="field">
        <label class="required" for="modal-fault">故障类型</label>
        <select id="modal-fault" v-model="faultTypeId" class="select" name="faultType">
          <option :value="null">请选择故障类型</option>
          <option v-for="item in faultTypes" :key="item.id" :value="item.id">{{ item.typeName }}</option>
        </select>
      </div>
      <div class="field">
        <label class="required" for="modal-description">问题描述</label>
        <textarea id="modal-description" v-model="problemDescription" class="textarea" name="description" placeholder="请描述故障表现和发生时间"></textarea>
      </div>
      <div class="field">
        <label for="modal-photo">现场图片（可选）</label>
        <ImageUploader v-model="imageUrls" :max="9" disabled>
          <template #hint>图片上传接口待后端提供，暂不可上传。</template>
        </ImageUploader>
      </div>
      <div class="grid two">
        <div class="field">
          <label class="required" for="modal-contact">联系人</label>
          <input id="modal-contact" v-model="contactName" class="input" name="contact" />
        </div>
        <div class="field">
          <label class="required" for="modal-phone">联系方式</label>
          <input id="modal-phone" v-model="contactPhone" class="input" name="phone" inputmode="tel" placeholder="手机号或联系电话" />
        </div>
      </div>
    </form>
  </FormDialog>

  <ConfirmDialog
    :visible="duplicateVisible"
    title="发现相似未完成工单"
    confirm-text="仍然提交"
    cancel-text="返回修改"
    danger
    @update:visible="backToEdit"
    @confirm="forceSubmit"
    @cancel="backToEdit"
  >
    <div class="notice red">
      同一位置与故障类型存在近 24 小时内的未完成工单，你可以先核对以下工单，也可以继续提交。
    </div>
    <ul class="suspected-list">
      <li v-for="item in suspectedOrders.slice(0, 3)" :key="item.orderId">
        <strong>{{ item.orderNo }}</strong>
        <span>{{ item.faultTypeName }} · {{ item.locationText }}</span>
        <span class="muted">{{ item.reportTime }} · {{ item.statusName }}</span>
      </li>
    </ul>
    <p v-if="suspectedOrders.length > 3" class="muted">等 {{ suspectedOrders.length }} 条相似工单</p>
  </ConfirmDialog>
</template>

<style scoped>
.suspected-list {
  display: grid;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.suspected-list li {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--dr-color-border);
  border-radius: 10px;
  font-size: 13px;
}

.muted {
  color: var(--dr-color-text-muted);
}
</style>
