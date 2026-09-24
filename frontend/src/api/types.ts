export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageQuery {
  pageNum: number
  pageSize: number
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}
