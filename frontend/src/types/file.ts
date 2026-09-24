/**
 * 文件上传公共类型。字段与后端 `FileUploadResponse`、`FileResponse` 契约一致。
 */

/** 文件业务类型；上传时传枚举名，学生可用 REPAIR/REWORK，维修人员可用 PROCESS。 */
export type FileBizType = 'REPAIR' | 'PROCESS' | 'REWORK'

/** 已上传文件引用（后端 `FileResponse`，详情响应中挂载于 files 字段）。 */
export interface OrderFile {
  fileId: number
  originalName: string
  /** 文件类型：IMAGE、VIDEO。 */
  fileType: string
  contentType: string
  fileSize: number
  /** 预览地址（30 分钟内有效）。 */
  previewUrl: string
}
