import type { PageQuery } from './types'

export function normalizePageQuery(query: Partial<PageQuery> = {}): PageQuery {
  const pageNum = Number.isInteger(query.pageNum) && (query.pageNum ?? 0) > 0 ? query.pageNum! : 1
  const requestedSize = Number.isInteger(query.pageSize) && (query.pageSize ?? 0) > 0 ? query.pageSize! : 10
  return { pageNum, pageSize: Math.min(requestedSize, 100) }
}
