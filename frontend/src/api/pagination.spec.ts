import { describe, expect, it } from 'vitest'
import { normalizePageQuery } from './pagination'

describe('分页参数归一化', () => {
  it('应用默认值且不修改原对象', () => {
    expect(normalizePageQuery()).toEqual({ pageNum: 1, pageSize: 10 })
    const source = { pageNum: 0, pageSize: 101 }
    expect(normalizePageQuery(source)).toEqual({ pageNum: 1, pageSize: 100 })
    expect(source).toEqual({ pageNum: 0, pageSize: 101 })
  })

  it('非法值回退到默认值', () => {
    expect(normalizePageQuery({ pageNum: -2, pageSize: -1 })).toEqual({ pageNum: 1, pageSize: 10 })
  })
})
