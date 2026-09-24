import MockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { http } from './http'
import { deleteFile, uploadFile } from './file'

describe('文件上传接口模块', () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(http)
  })
  afterEach(() => {
    mock.restore()
  })

  it('上传文件以 multipart 携带 file 与 bizType 枚举名', async () => {
    mock.onPost('/files/upload').reply((config) => {
      const form = config.data as FormData
      expect(form.get('bizType')).toBe('REPAIR')
      expect(form.get('file')).toBeInstanceOf(File)
      return [200, {
        code: 0,
        message: 'success',
        data: {
          fileId: 9,
          originalName: 'a.jpg',
          fileType: 'IMAGE',
          contentType: 'image/jpeg',
          fileSize: 10,
          objectName: 'repair/2026/09/24/a.jpg',
          previewUrl: 'http://minio.local/repair/2026/09/24/a.jpg',
        },
      }]
    })

    const file = new File(['x'], 'a.jpg', { type: 'image/jpeg' })
    const result = await uploadFile(file, 'REPAIR')
    expect(result).toMatchObject({ fileId: 9, fileType: 'IMAGE', previewUrl: 'http://minio.local/repair/2026/09/24/a.jpg' })
  })

  it('维修过程与返工分别携带对应 bizType', async () => {
    const seen: string[] = []
    mock.onPost('/files/upload').reply((config) => {
      seen.push(String((config.data as FormData).get('bizType')))
      return [200, { code: 0, message: 'success', data: { fileId: 1, previewUrl: 'x' } }]
    })

    await uploadFile(new File(['x'], 'p.jpg'), 'PROCESS')
    await uploadFile(new File(['x'], 'r.jpg'), 'REWORK')
    expect(seen).toEqual(['PROCESS', 'REWORK'])
  })

  it('删除未绑定文件请求 DELETE 路径', async () => {
    mock.onDelete('/files/7').reply(200, { code: 0, message: 'success', data: null })
    await expect(deleteFile(7)).resolves.toBeNull()
  })
})
