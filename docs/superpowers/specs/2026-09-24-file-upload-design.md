# 公共文件上传与业务附件关联设计

## 1. 目标与范围

本阶段建立后端公共文件上传能力。文件由前端以 `multipart/form-data` 上传到 Spring Boot，再由 Spring Boot 统一写入 MinIO；业务模块只使用文件 ID 和 `FileService`，不直接依赖 MinIO。

覆盖学生报修附件、维修过程及维修结果附件、学生返工附件。支持图片和视频，不实现分片、断点续传、秒传、转码、压缩、内容审核、前端直传和消息队列异步上传。

本阶段不开发前端页面或组件。前端应完成的上传流程、字段迁移、预览、删除、进度、数量限制与防重复要求只写入接口说明。

## 2. 总体结构

新增 `file` 后端模块，包括控制器、服务、枚举、DTO/VO；实体和 Mapper 继续遵循现有 `domain/entity`、`domain/mapper` 结构。

调用关系如下：

```text
FileController -> FileService -> MinioClient + SysFileMapper
业务 Service -> FileService -> 三类附件关联 Mapper
```

MinIO Bucket 使用私有桶 `repair-files`。对象名完全由后端生成：

```text
repair/yyyy/MM/dd/UUID.ext
process/yyyy/MM/dd/UUID.ext
rework/yyyy/MM/dd/UUID.ext
```

原始文件名仅用于展示，必须移除客户端路径信息，不参与对象路径生成。

## 3. 配置

沿用现有 `app.minio` 配置前缀，在 `MinioProperties` 增加 `bucketName`、预览地址有效期和是否自动创建 Bucket。密钥继续由环境变量注入。

新增 `app.file-upload` 配置：图片最大 10MB、视频最大 200MB、允许的扩展名和 MIME 类型、孤儿文件保留 24 小时。Spring Multipart 上限设置为单文件 200MB、单请求 210MB，必须不小于业务层上限。

开发环境启动时幂等检查 Bucket 并创建；生产可关闭自动创建，由运维预建。

## 4. 文件与关联数据模型

新增四张正式表，正式表总数由 21 增至 25：

- `sys_file`：保存原始名称、Bucket、对象名、IMAGE/VIDEO、MIME、后缀、大小、REPAIR/PROCESS/REWORK、状态、上传人、时间和逻辑删除标志。
- `repair_order_file`：关联 `repair_order` 与文件。
- `repair_process_file`：关联实际表 `repair_process_record` 与文件。
- `repair_rework_file`：关联 `repair_rework_record` 与文件。

每张关联表使用“业务记录 ID + file_id”唯一索引，并为业务记录 ID、file_id 建普通索引。不使用外键，保持现有数据库约束风格。

现有 `repair_order.image_urls`、`repair_process_record.image_urls`、`repair_rework_record.image_urls` 字段暂时保留以兼容历史数据，但新接口不再写入；新附件只写关联表。查询时历史 URL 与新文件列表分别表达，不进行隐式数据迁移。

## 5. 上传接口

新增：

```http
POST /api/files/upload
Content-Type: multipart/form-data
file=<binary>
bizType=REPAIR|PROCESS|REWORK
```

上传者必须已登录。学生只允许 REPAIR、REWORK；维修人员只允许 PROCESS；管理员不开放业务上传。服务端依次校验空文件、业务类型、原始文件名、扩展名、MIME 和大小，再生成对象名。

首期允许：

- 图片：jpg、jpeg、png、webp；`image/jpeg`、`image/png`、`image/webp`；单文件不超过 10MB。
- 视频：mp4、mov、webm；`video/mp4`、`video/quicktime`、`video/webm`；单文件不超过 200MB。

扩展名与 MIME 必须同时匹配。当前不引入 Apache Tika，不承诺检测伪造后的真实二进制类型。

成功返回统一 `Result<FileUploadResponse>`，其中包括 `fileId`、`originalName`、`fileType`、`contentType`、`fileSize`、`objectName` 和临时 `previewUrl`。统一成功码仍为 `0`，不采用需求文档示例中的 `200` 业务码。

## 6. 上传事务与补偿

顺序为：校验 -> MinIO `putObject` -> 插入 `sys_file` -> 生成临时预览地址。

数据库事务不能覆盖 MinIO。MinIO 成功但数据库插入或后续返回构建失败时，服务主动删除刚上传的对象；补偿失败记录错误日志，但不向客户端暴露对象地址、凭据或内部异常。MinIO 上传失败时不写数据库。

`FileService` 对业务模块提供：上传、按 ID 查询、生成预览地址、校验并绑定、查询业务附件、删除本人未绑定文件、清理孤儿文件等能力。MinIO 调用只存在于文件模块。

## 7. 业务绑定规则

三个既有流程改为接收 `fileIds`：

- 学生创建报修：`CreateRepairOrderRequest.imageUrls` 改为 `fileIds`，创建 `repair_order` 后批量写 `repair_order_file`。
- 维修人员添加过程和提交维修结果：两个请求中的图片地址改为 `fileIds`，创建实际 `repair_process_record` 后写 `repair_process_file`。
- 学生申请返工：`CreateReworkRequest.imageUrls` 改为 `fileIds`，创建 `repair_rework_record` 后写 `repair_rework_file`。

绑定与业务记录创建处于同一数据库事务。绑定前必须将 fileIds 去重并检查：全部存在、上传成功、未删除、上传人为当前用户、`biz_type` 与业务入口一致、未出现在任何关联表。任一文件不合格则整个业务事务回滚。

每个业务操作最多绑定 9 个文件，图片和视频合计计数。一个 fileId 只能绑定一个业务记录，禁止跨工单或跨业务复用。

## 8. 查询与预览

工单详情在现有权限判定通过后返回结构化附件：

```json
{
  "fileId": 10001,
  "originalName": "漏水现场.jpg",
  "fileType": "IMAGE",
  "contentType": "image/jpeg",
  "fileSize": 1542440,
  "previewUrl": "临时签名地址"
}
```

报修附件放在工单基础信息中；过程/维修结果附件跟随各自过程记录；返工附件跟随返工记录。预览 URL 动态生成，不落业务表和日志。学生仍遵循既有详情数据裁剪规则，维修人员和管理员遵循现有工单访问权限。

## 9. 删除与孤儿清理

新增：

```http
DELETE /api/files/{fileId}
```

只允许上传人删除本人尚未绑定、上传成功且未逻辑删除的文件。已绑定返回 409，非上传者返回 403，不存在返回 404。

删除顺序为 MinIO 对象删除成功后再将数据库记录逻辑删除。MinIO 删除失败则保留数据库记录并返回文件删除失败，保证真实对象仍可追踪。

定时任务清理创建超过 24 小时、上传成功、未删除且未绑定的文件。每批限制数量，逐个调用同一删除能力；单个失败不阻断整批，并记录可追踪日志。清理任务不得删除已绑定文件。

## 10. 错误与安全

新增文件专项错误码，覆盖空文件、业务类型非法、格式非法、超限、上传失败、文件不存在、非上传者、已绑定、删除失败。参数与格式错误返回 400；身份错误 401；角色或所有权错误 403；不存在 404；已绑定或状态冲突 409；无法处理的内部错误 500。响应继续使用统一 `Result`。

Spring Multipart 超限异常纳入全局异常处理并转为明确的 400 响应。日志可记录 fileId、userId、bizType、fileType、size、objectName，不记录密钥、Token、完整临时签名 URL和文件内容。

## 11. 文档与验收

同步更新数据库主结构、增量 SQL、数据库说明、架构说明、安全说明、README、专项上传接口文档和 `doc/api/前端联调接口说明.md`。联调文档明确前端职责，但不提交前端代码。

新增本阶段验收记录和语义明确的验收截图。验证范围包括：允许的图片和视频真实上传、非法格式、扩展名/MIME 不匹配、超限、角色权限、MinIO 对象与 `sys_file` 一致、三类业务绑定、跨用户与重复绑定拦截、临时预览、未绑定删除、已绑定禁止删除、补偿删除、孤儿清理、全量测试、打包及 8811 最新服务健康检查。

## 12. 明确不做

- 不开发任何前端代码。
- 不迁移历史 `image_urls` 数据。
- 不提供独立公开预览接口；详情返回临时签名 URL。
- 不实现已绑定文件的业务删除或替换流程。
- 不做文件内容扫描、视频转码、分片上传和 MinIO 直传。
