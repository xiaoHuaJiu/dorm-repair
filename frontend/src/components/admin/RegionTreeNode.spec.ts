import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { reactive } from 'vue'
import type { AreaTreeNode } from '@/types/config'
import RegionTreeNode from './RegionTreeNode.vue'

function buildNode(overrides: Partial<AreaTreeNode> = {}): AreaTreeNode {
  return {
    id: 1,
    parentId: null,
    areaCode: 'C01',
    areaName: '东校区',
    areaType: 1,
    status: 1,
    sortNo: 0,
    children: [],
    ...overrides,
  }
}

/** 统一提供受控展开状态集合，避免各用例重复传参。 */
function mountNode(
  node: AreaTreeNode,
  props: { editing?: boolean; expandedIds?: Set<number> } = {},
) {
  return mount(RegionTreeNode, {
    props: { node, editing: false, expandedIds: new Set<number>(), ...props },
  })
}

describe('区域树递归节点', () => {
  it('渲染分支节点：类型标签、名称，且默认折叠', () => {
    const wrapper = mountNode(buildNode())
    expect(wrapper.find('.region-branch').exists()).toBe(true)
    expect(wrapper.find('.region-node-copy small').text()).toBe('单位/校区')
    expect(wrapper.find('.region-node-copy strong').text()).toBe('东校区')
    expect((wrapper.find('details').element as HTMLDetailsElement).open).toBe(false)
  })

  it('房间节点渲染为叶子行，无折叠容器', () => {
    const wrapper = mountNode(buildNode({ areaType: 4, areaName: '101 室' }))
    expect(wrapper.find('.region-node-row.leaf').exists()).toBe(true)
    expect(wrapper.find('details').exists()).toBe(false)
    expect(wrapper.find('.region-node-copy small').text()).toBe('房间')
  })

  it('浏览态隐藏操作按钮，编辑态显示', async () => {
    const wrapper = mountNode(buildNode())
    // 用 hidden 属性断言：jsdom 的 getComputedStyle 存在缓存，
    // 先后两次 isVisible 会拿到缓存的 display 值，无法反映属性切换。
    expect(wrapper.find('.region-actions').attributes('hidden')).toBeDefined()

    await wrapper.setProps({ editing: true })
    expect(wrapper.find('.region-actions').attributes('hidden')).toBeUndefined()
  })

  it('房间节点没有新增下级按钮，只有删除按钮', () => {
    const wrapper = mountNode(buildNode({ areaType: 4, areaName: '101 室' }), { editing: true })
    expect(wrapper.find('.region-icon.add').exists()).toBe(false)
    expect(wrapper.find('.region-icon.remove').exists()).toBe(true)
  })

  it('分支节点有新增下级按钮，点击发出 add 事件', async () => {
    const node = buildNode()
    const wrapper = mountNode(node, { editing: true })
    await wrapper.find('.region-icon.add').trigger('click')
    expect(wrapper.emitted('add')?.[0]).toEqual([node])
  })

  it('无子节点时删除按钮禁用（后端无删除接口），有子节点时可点击并发出 remove', async () => {
    const leafWrapper = mountNode(buildNode(), { editing: true })
    expect(leafWrapper.find('.region-icon.remove').attributes('disabled')).toBeDefined()
    expect(leafWrapper.find('.region-icon.remove').attributes('title')).toContain('后端暂无删除接口')

    const child = buildNode({ id: 2, parentId: 1, areaType: 2, areaName: '宿舍区' })
    const wrapper = mountNode(buildNode({ children: [child] }), { editing: true })
    expect(wrapper.find('.region-icon.remove').attributes('disabled')).toBeUndefined()
    await wrapper.find('.region-icon.remove').trigger('click')
    expect(wrapper.emitted('remove')).toHaveLength(1)
  })

  it('停用节点显示停用标签', () => {
    const wrapper = mountNode(buildNode({ status: 0 }))
    expect(wrapper.find('.tag.amber').text()).toBe('停用')
  })

  it('有子节点时递归渲染下级，无子节点时显示空提示', () => {
    const child = buildNode({ id: 2, parentId: 1, areaType: 2, areaName: '宿舍区' })
    const withChildren = mountNode(buildNode({ children: [child] }))
    expect(withChildren.text()).toContain('宿舍区')

    const empty = mountNode(buildNode())
    expect(empty.find('.region-empty').text()).toBe('暂无下级节点')
  })

  it('展开与收起时同步共享集合', async () => {
    const expandedIds = reactive(new Set<number>())
    const wrapper = mountNode(buildNode(), { expandedIds })
    const details = wrapper.find('details.region-branch')

    // 模拟浏览器点击 summary：open 属性先变化，随后派发 toggle 事件。
    ;(details.element as HTMLDetailsElement).open = true
    await details.trigger('toggle')
    expect([...expandedIds]).toEqual([1])

    ;(details.element as HTMLDetailsElement).open = false
    await details.trigger('toggle')
    expect([...expandedIds]).toEqual([])
  })
})
