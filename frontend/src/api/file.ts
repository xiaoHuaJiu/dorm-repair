import { request } from './http'
import type { FileBizType, OrderFile } from '@/types/file'

/**
 * 文件上传接口。路径与后端 FileController 一致：
 * POST /api/files/upload、DELETE /api/files/{fileId}。
 */

/**
 * 上传单个文件。
 * bizType 传后端枚举名：报修 REPAIR、维修过程 PROCESS、返工 REWORK；
 * 上传后需在业务提交时携带返回的 fileId 完成绑定。
 */
export function uploadFile(file: File, bizType: FileBizType): Promise<OrderFile> {
  const form = new FormData()
  form.append('file', file)
  form.append('bizType', bizType)
  return request<OrderFile>({ method: 'POST', url: '/files/upload', data: form })
}

/** 删除已上传但未绑定的文件（仅限上传人本人）。 */
export function deleteFile(fileId: number): Promise<null> {
  return request<null>({ method: 'DELETE', url: `/files/${fileId}` })
}
