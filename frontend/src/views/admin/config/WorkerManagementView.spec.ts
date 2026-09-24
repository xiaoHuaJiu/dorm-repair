import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
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
import { notifySuccess } from '@/api/successNotifier'
import type { AreaTreeNode, FaultTypeItem, WorkerDetail } from '@/types/config'
import type { PageResult } from '@/api/types'
import WorkerManagementView from './WorkerManagementView.vue'

vi.mock('@/api/worker', () => ({
  pageWorkers: vi.fn(),
  createWorker: vi.fn(),
  updateWorker: vi.fn(),
  updateAccountStatus: vi.fn(),
  listWorkerFaultTypes: vi.fn(),
  saveWorkerFaultTypes: vi.fn(),
  listWorkerAreaScopes: vi.fn(),
  saveWorkerAreaScopes: vi.fn(),
}))

vi.mock('@/api/area', () => ({
  adminAreaTree: vi.fn(),
}))

vi.mock('@/api/faultType', () => ({
  listEnabledFaultTypes: vi.fn(),
}))

vi.mock('@/api/successNotifier', () => ({
  notifySuccess: vi.fn(),
}))

const mockedPage = vi.mocked(pageWorkers)
const mockedCreate = vi.mocked(createWorker)
const mockedUpdate = vi.mocked(updateWorker)
const mockedAccountStatus = vi.mocked(updateAccountStatus)
const mockedWorkerFaultTypes = vi.mocked(listWorkerFaultTypes)
const mockedSaveFaultTypes = vi.mocked(saveWorkerFaultTypes)
const mockedWorkerScopes = vi.mocked(listWorkerAreaScopes)
const mockedSaveScopes = vi.mocked(saveWorkerAreaScopes)
const mockedAreaTree = vi.mocked(adminAreaTree)
const mockedEnabledFaultTypes = vi.mocked(listEnabledFaultTypes)
const mockedNotifySuccess = vi.mocked(notifySuccess)

const WORKERS: WorkerDetail[] = [
  {
    workerId: 1,
    userId: 10,
    username: 'worker1',
    realName: '张师傅',
    phone: '13800138000',
    workerNo: 'W001',
    userStatus: 1,
    workStatus: 0,
    remark: null,
  },
]

const FAULT_TYPES: FaultTypeItem[] = [
  { id: 11, typeCode: 'SHUI', typeName: '水暖维修', status: 1, sortNo: 0, remark: null },
  { id: 12, typeCode: 'DIAN', typeName: '电路维修', status: 1, sortNo: 1, remark: null },
]

const AREA_TREE: AreaTreeNode[] = [
  {
    id: 1,
    parentId: null,
    areaCode: 'C01',
    areaName: '东校区',
    areaType: 1,
    status: 1,
    sortNo: 0,
    children: [
      {
        id: 2,
        parentId: 1,
        areaCode: 'A01',
        areaName: '宿舍区',
        areaType: 2,
        status: 1,
        sortNo: 0,
        children: [
          {
            id: 3,
            parentId: 2,
            areaCode: 'B01',
            areaName: '1 号楼',
            areaType: 3,
            status: 1,
            sortNo: 0,
            children: [],
          },
        ],
      },
    ],
  },
]

function buildPage(records: WorkerDetail[] = WORKERS): PageResult<WorkerDetail> {
  return { total: records.length, pageNum: 1, pageSize: 10, records }
}

function mountView() {
  setActivePinia(createPinia())
  const router = createAppRouter(createMemoryHistory())
  const wrapper = mount(WorkerManagementView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

describe('维修人员管理页', () => {
  beforeEach(() => {
    mockedPage.mockReset()
    mockedCreate.mockReset()
    mockedUpdate.mockReset()
    mockedAccountStatus.mockReset()
    mockedWorkerFaultTypes.mockReset()
    mockedSaveFaultTypes.mockReset()
    mockedWorkerScopes.mockReset()
    mockedSaveScopes.mockReset()
    mockedAreaTree.mockReset()
    mockedEnabledFaultTypes.mockReset()
    mockedNotifySuccess.mockReset()

    mockedPage.mockResolvedValue(buildPage())
    mockedAreaTree.mockResolvedValue(AREA_TREE)
    mockedEnabledFaultTypes.mockResolvedValue(FAULT_TYPES)
    mockedWorkerFaultTypes.mockResolvedValue([{ id: 1, workerId: 1, faultTypeId: 11, status: 1 }])
    mockedWorkerScopes.mockResolvedValue([
      { id: 1, workerId: 1, campusId: 1, areaId: 2, buildingId: 3, status: 1 },
    ])
    mockedSaveFaultTypes.mockResolvedValue(null)
    mockedSaveScopes.mockResolvedValue(null)
  })

  it('渲染维修人员卡片与工作状态', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(mockedPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(wrapper.find('.worker-admin-list').exists()).toBe(true)
    expect(wrapper.text()).toContain('张师傅')
    expect(wrapper.find('.tag.green').text()).toBe('正常')
  })

  it('新增弹窗默认新建账号模式并校验密码长度', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('新增维修人员')

    await wrapper.find('#worker-username').setValue('worker2')
    await wrapper.find('#worker-password').setValue('short')
    await wrapper.find('#worker-real-name').setValue('李师傅')
    await wrapper.find('#worker-no').setValue('W002')
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal .error').text()).toBe('密码长度必须为 8 到 64 个字符')
    expect(mockedCreate).not.toHaveBeenCalled()
  })

  it('新建账号模式提交正确参数并刷新列表', async () => {
    mockedCreate.mockResolvedValue({ id: 2 })
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')

    await wrapper.find('#worker-username').setValue('worker2')
    await wrapper.find('#worker-password').setValue('password1')
    await wrapper.find('#worker-real-name').setValue('李师傅')
    await wrapper.find('#worker-phone').setValue('13900139000')
    await wrapper.find('#worker-no').setValue('W002')
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith({
      username: 'worker2',
      password: 'password1',
      realName: '李师傅',
      phone: '13900139000',
      workerNo: 'W002',
      remark: undefined,
    })
    expect(mockedNotifySuccess).toHaveBeenCalledWith('已创建维修人员账号')
    expect(mockedPage).toHaveBeenCalledTimes(2)
  })

  it('绑定已有账号模式只提交账号 ID 与编号', async () => {
    mockedCreate.mockResolvedValue({ id: 2 })
    const { wrapper } = mountView()
    await flushPromises()
    await wrapper.find('.page-head .btn.primary').trigger('click')

    const bindButton = wrapper
      .findAll('.modal .inline-actions .btn')
      .find((button) => button.text() === '绑定已有账号')
    await bindButton?.trigger('click')

    await wrapper.find('#worker-user-id').setValue('7')
    await wrapper.find('#worker-no').setValue('W003')
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith({ existingUserId: 7, workerNo: 'W003', remark: undefined })
  })

  it('编辑弹窗修改编号与备注', async () => {
    mockedUpdate.mockResolvedValue(null)
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.find('.admin-worker-card .btn.outline').trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('编辑维修人员')

    await wrapper.find('#worker-no').setValue('W009')
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedUpdate).toHaveBeenCalledWith(1, expect.objectContaining({ workerNo: 'W009' }))
  })

  it('账号停用需确认，确认后调用接口并刷新', async () => {
    mockedAccountStatus.mockResolvedValue(null)
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.find('.admin-worker-card .btn.danger').trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('停用账号')

    const confirm = wrapper.findAll('.modal .btn').find((button) => button.text() === '确认停用')
    await confirm?.trigger('click')
    await flushPromises()

    expect(mockedAccountStatus).toHaveBeenCalledWith(1, 0)
    expect(mockedNotifySuccess).toHaveBeenCalledWith('账号已停用')
  })

  it('技能与区域弹窗加载已有配置并保存', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    const configButton = wrapper.findAll('.admin-worker-card .btn').find((button) => button.text() === '技能与区域')
    await configButton?.trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal h2').text()).toBe('配置技能与区域 - 张师傅')
    expect(mockedWorkerFaultTypes).toHaveBeenCalledWith(1)
    expect(mockedWorkerScopes).toHaveBeenCalledWith(1)
    // 已选技能勾选状态。
    expect((wrapper.find('.check-row input').element as HTMLInputElement).checked).toBe(true)

    // 追加第二个技能并新增一条负责范围。
    const checkboxes = wrapper.findAll('.check-row input')
    await checkboxes[1].setValue(true)
    await wrapper.find('.scope-add').trigger('click')

    const scopeRows = wrapper.findAll('.scope-row')
    const lastRow = scopeRows[scopeRows.length - 1]
    const campusSelect = lastRow.find('select')
    await campusSelect.setValue('1')
    await flushPromises()

    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedSaveFaultTypes).toHaveBeenCalledWith(1, [11, 12])
    expect(mockedSaveScopes).toHaveBeenCalledWith(1, [
      { campusId: 1, areaId: 2, buildingId: 3 },
      { campusId: 1, areaId: null, buildingId: null },
    ])
    expect(mockedNotifySuccess).toHaveBeenCalledWith('技能与负责区域已保存')
  })

  it('负责范围未选校区时校验不提交', async () => {
    const { wrapper } = mountView()
    await flushPromises()

    const configButton = wrapper.findAll('.admin-worker-card .btn').find((button) => button.text() === '技能与区域')
    await configButton?.trigger('click')
    await flushPromises()

    await wrapper.find('.scope-add').trigger('click')
    await wrapper.find('.modal-actions .btn.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal .error').text()).toBe('每条负责范围必须选择校区')
    expect(mockedSaveScopes).not.toHaveBeenCalled()
  })

  it('空数据显示空状态', async () => {
    mockedPage.mockResolvedValue(buildPage([]))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无维修人员')
  })

  it('加载失败显示错误状态并可重试', async () => {
    mockedPage.mockRejectedValueOnce(new Error('网络错误'))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('重试')

    mockedPage.mockResolvedValueOnce(buildPage())
    await wrapper.find('.empty .btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('张师傅')
  })
})
