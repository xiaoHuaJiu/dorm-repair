import MockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { http } from './http'
import {
  checkRepairDuplicate,
  confirmRepairOrder,
  createRepairOrder,
  pageAdminOrders,
  pageStudentOrders,
  pageWorkerOrders,
  studentOrderDetail,
  submitRepairEvaluation,
  submitRepairRework,
  workerOrderDetail,
  adminOrderDetail,
} from './order'

describe('工单接口模块', () => {
  let mock: MockAdapter

  beforeEach(() => {
    mock = new MockAdapter(http)
  })
  afterEach(() => {
    mock.restore()
  })

  it('学生分页查询本人工单', async () => {
    mock.onGet('/student/repair-orders').reply((config) => [200, {
      code: 0,
      message: 'success',
      data: { total: 1, pageNum: 1, pageSize: 10, records: [], params: config.params },
    }])

    const result = await pageStudentOrders({ pageNum: 1, pageSize: 10, statusList: [2] })
    expect((result as unknown as { params: unknown }).params).toMatchObject({ pageNum: 1, pageSize: 10, statusList: [2] })
  })

  it('维修人员分页查询本人负责工单', async () => {
    mock.onGet('/worker/repair-orders').reply(200, { code: 0, message: 'success', data: { records: [] } })
    await expect(pageWorkerOrders({ pageNum: 1, pageSize: 10 })).resolves.toEqual({ records: [] })
  })

  it('管理员分页查询学校全部工单', async () => {
    mock.onGet('/admin/repair-orders').reply(200, { code: 0, message: 'success', data: { records: [] } })
    await expect(pageAdminOrders({ pageNum: 1, pageSize: 10, exceptionFlag: true })).resolves.toEqual({ records: [] })
  })

  it('三端详情分别请求各自端路径', async () => {
    const detail = { baseInfo: { id: 1 }, flows: [] }
    mock.onGet('/student/repair-orders/1').reply(200, { code: 0, message: 'success', data: detail })
    mock.onGet('/worker/repair-orders/1').reply(200, { code: 0, message: 'success', data: detail })
    mock.onGet('/admin/repair-orders/1').reply(200, { code: 0, message: 'success', data: detail })

    await expect(studentOrderDetail(1)).resolves.toEqual(detail)
    await expect(workerOrderDetail(1)).resolves.toEqual(detail)
    await expect(adminOrderDetail(1)).resolves.toEqual(detail)
  })

  it('疑似重复检测提交四级位置与故障类型', async () => {
    mock.onPost('/student/repair-orders/check-duplicate').reply((config) => [200, {
      code: 0,
      message: 'success',
      data: { duplicate: false, suspectedOrders: [], body: JSON.parse(config.data) },
    }])

    const result = await checkRepairDuplicate({
      campusId: 1,
      areaId: 2,
      buildingId: 3,
      roomId: 4,
      faultTypeId: 5,
    })
    expect((result as unknown as { body: unknown }).body).toMatchObject({ campusId: 1, areaId: 2, buildingId: 3, roomId: 4, faultTypeId: 5 })
  })

  it('创建报修携带 bizNo 防重键与完整请求体', async () => {
    mock.onPost('/student/repair-orders').reply((config) => [200, {
      code: 0,
      message: 'success',
      data: { created: true, body: JSON.parse(config.data) },
    }])

    const result = await createRepairOrder({
      bizNo: 'biz-1',
      campusId: 1,
      areaId: 2,
      buildingId: 3,
      roomId: 4,
      faultTypeId: 5,
      locationDetail: '卫生间',
      problemDescription: '水龙头漏水',
      contactName: '林同学',
      contactPhone: '13800138000',
      fileIds: [3, 5],
      confirmDuplicate: false,
    })
    expect((result as unknown as { body: unknown }).body).toMatchObject({
      bizNo: 'biz-1',
      problemDescription: '水龙头漏水',
      contactName: '林同学',
      fileIds: [3, 5],
      confirmDuplicate: false,
    })
  })

  it('学生确认完成请求对应端 confirm 路径', async () => {
    mock.onPost('/student/repair-orders/12/confirm').reply(200, { code: 0, message: 'success', data: null })
    await expect(confirmRepairOrder(12)).resolves.toBeNull()
  })

  it('学生提交评价携带评分与内容', async () => {
    mock.onPost('/student/repair-orders/12/evaluation').reply(200, { code: 0, message: 'success', data: null })

    await submitRepairEvaluation(12, { score: 5, content: '师傅很专业' })
    expect(JSON.parse(mock.history.post.at(-1)?.data as string)).toMatchObject({ score: 5, content: '师傅很专业' })
  })

  it('学生申请返工携带原因与附件引用', async () => {
    mock.onPost('/student/repair-orders/12/rework').reply(200, { code: 0, message: 'success', data: null })

    await submitRepairRework(12, { reason: '仍有渗水', fileIds: [8] })
    expect(JSON.parse(mock.history.post.at(-1)?.data as string)).toMatchObject({ reason: '仍有渗水', fileIds: [8] })
  })
})
