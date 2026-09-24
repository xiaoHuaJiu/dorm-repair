import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { createAppRouter } from '@/router'
import { adminAreaTree, createArea } from '@/api/area'
import { notifyError } from '@/api/errorNotifier'
import { notifySuccess } from '@/api/successNotifier'
import type { AreaTreeNode } from '@/types/config'
import AreaManagementView from './AreaManagementView.vue'

vi.mock('@/api/area', () => ({
  adminAreaTree: vi.fn(),
  createArea: vi.fn(),
}))

vi.mock('@/api/errorNotifier', () => ({
  notifyError: vi.fn(),
}))

vi.mock('@/api/successNotifier', () => ({
  notifySuccess: vi.fn(),
}))

const mockedTree = vi.mocked(adminAreaTree)
const mockedCreate = vi.mocked(createArea)
const mockedNotifyError = vi.mocked(notifyError)
const mockedNotifySuccess = vi.mocked(notifySuccess)

function buildTree(): AreaTreeNode[] {
  return [
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
              children: [
                { id: 4, parentId: 3, areaCode: 'R01', areaName: '101 室', areaType: 4, status: 1, sortNo: 0, children: [] },
              ],
            },
          ],
        },
      ],
    },
  ]
}

function mountView() {
  setActivePinia(createPinia())
  const router = createAppRouter(createMemoryHistory())
  const wrapper = mount(AreaManagementView, {
    global: { plugins: [router] },
  })
  return { router, wrapper }
}

async function enterEditing(wrapper: ReturnType<typeof mountView>['wrapper']) {
  await wrapper.find('[aria-pressed]').trigger('click')
}

describe('区域配置页', () => {
  beforeEach(() => {
    mockedTree.mockReset()
    mockedCreate.mockReset()
    mockedNotifyError.mockReset()
    mockedNotifySuccess.mockReset()
    mockedTree.mockResolvedValue(buildTree())
  })

  it('加载并渲染四级区域树', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(mockedTree).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.region-panel').exists()).toBe(true)
    expect(wrapper.text()).toContain('东校区')
    expect(wrapper.text()).toContain('宿舍区')
  })

  it('浏览态隐藏增删按钮与根节点按钮，点击编辑后显示', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.find('[aria-pressed]').text()).toBe('编辑')
    // 用 hidden 属性断言：jsdom 的 getComputedStyle 存在缓存，
    // 先后两次 isVisible 会拿到缓存的 display 值，无法反映属性切换。
    expect(wrapper.find('.region-actions').attributes('hidden')).toBeDefined()
    expect(wrapper.find('.region-root-add').attributes('hidden')).toBeDefined()

    await enterEditing(wrapper)
    expect(wrapper.find('[aria-pressed]').text()).toBe('完成')
    expect(wrapper.find('[aria-pressed]').classes()).toContain('primary')
    expect(wrapper.find('.region-actions').attributes('hidden')).toBeUndefined()
    expect(wrapper.find('.region-root-add').attributes('hidden')).toBeUndefined()

    await enterEditing(wrapper)
    expect(wrapper.find('[aria-pressed]').text()).toBe('编辑')
    expect(wrapper.find('.region-actions').attributes('hidden')).toBeDefined()
  })

  it('编辑态点击 + 打开添加下级弹窗，标题为子级类型', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)

    await wrapper.find('.region-icon.add').trigger('click')
    expect(wrapper.find('.modal').exists()).toBe(true)
    expect(wrapper.find('.modal h2').text()).toBe('添加区域')
    expect(wrapper.find('.modal').text()).toContain('节点类型固定为“区域”')
  })

  it('添加弹窗名校验：空名称不提交接口', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)
    await wrapper.find('.region-icon.add').trigger('click')

    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()
    expect(wrapper.find('.modal .error').text()).toBe('名称不能为空')
    expect(mockedCreate).not.toHaveBeenCalled()
  })

  it('添加下级提交正确参数并刷新树', async () => {
    mockedCreate.mockResolvedValue({ id: 9 })
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)
    await wrapper.find('.region-icon.add').trigger('click')

    await wrapper.find('#region-name').setValue('新区域')
    await wrapper.find('#region-code').setValue('A02')
    await wrapper.find('#region-sort').setValue('3')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith({
      parentId: 1,
      areaCode: 'A02',
      areaName: '新区域',
      areaType: 2,
      sortNo: 3,
      remark: undefined,
    })
    expect(mockedNotifySuccess).toHaveBeenCalledWith('已添加区域节点')
    expect(mockedTree).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.modal').exists()).toBe(false)
  })

  it('编辑态点击底部按钮打开新增学校/单位弹窗，parentId 为 0', async () => {
    mockedCreate.mockResolvedValue({ id: 9 })
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)

    await wrapper.find('.region-root-add').trigger('click')
    expect(wrapper.find('.modal h2').text()).toBe('添加单位/校区')

    await wrapper.find('#region-name').setValue('西校区')
    await wrapper.find('#region-code').setValue('C02')
    await wrapper.find('#region-sort').setValue('1')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith(
      expect.objectContaining({ parentId: 0, areaType: 1, areaName: '西校区' }),
    )
  })

  it('展开状态下新增下级，刷新后树保持展开且父节点自动展开', async () => {
    mockedCreate.mockResolvedValue({ id: 9 })
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)

    // 模拟用户点击 summary 展开“东校区”：设置 open 后触发 toggle 同步到受控状态。
    const rootDetails = wrapper.find('details.region-branch')
    ;(rootDetails.element as HTMLDetailsElement).open = true
    await rootDetails.trigger('toggle')
    expect(rootDetails.attributes('open')).toBeDefined()

    // 在“宿舍区”下新增下级（第 2 个 + 按钮）。
    await wrapper.findAll('.region-icon.add')[1].trigger('click')
    await wrapper.find('#region-name').setValue('2 号楼')
    await wrapper.find('#region-code').setValue('B02')
    await wrapper.find('#region-sort').setValue('1')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith(expect.objectContaining({ parentId: 2 }))
    // 刷新重挂载后：“东校区”保持用户展开状态，“宿舍区”（新增父节点）自动展开。
    const details = wrapper.findAll('details.region-branch')
    expect(details[0].attributes('open')).toBeDefined()
    expect(details[1].attributes('open')).toBeDefined()
  })

  it('手动收起的分支在新增刷新后保持收起', async () => {
    mockedCreate.mockResolvedValue({ id: 9 })
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)

    // 展开“宿舍区”后再收起，验证收起状态同样受控。
    const branchDetails = wrapper.findAll('details.region-branch')[1]
    ;(branchDetails.element as HTMLDetailsElement).open = true
    await branchDetails.trigger('toggle')
    ;(branchDetails.element as HTMLDetailsElement).open = false
    await branchDetails.trigger('toggle')

    // 在“东校区”下新增下级。
    await wrapper.find('.region-icon.add').trigger('click')
    await wrapper.find('#region-name').setValue('新区域')
    await wrapper.find('#region-code').setValue('A02')
    await wrapper.find('#region-sort').setValue('3')
    await wrapper.find('.modal .btn.primary').trigger('click')
    await flushPromises()

    expect(mockedCreate).toHaveBeenCalledWith(expect.objectContaining({ parentId: 1 }))
    // “东校区”为新增父节点被强制展开；手动收起的“宿舍区”保持收起。
    const details = wrapper.findAll('details.region-branch')
    expect(details[0].attributes('open')).toBeDefined()
    expect(details[1].attributes('open')).toBeUndefined()
  })

  it('删除有下级节点时提示先删除下级', async () => {
    const { wrapper } = mountView()
    await flushPromises()
    await enterEditing(wrapper)

    // 根节点（东校区）有子节点：可点击并提示。
    await wrapper.find('.region-icon.remove').trigger('click')
    expect(mockedNotifyError).toHaveBeenCalledWith('请先删除下级节点')
  })

  it('空数据展示空状态', async () => {
    mockedTree.mockResolvedValue([])
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无区域数据')
  })

  it('加载失败展示错误状态，点击重试重新加载', async () => {
    mockedTree.mockRejectedValueOnce(new Error('网络错误'))
    const { wrapper } = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('重试')

    mockedTree.mockResolvedValueOnce(buildTree())
    await wrapper.find('.empty .btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('东校区')
  })
})
