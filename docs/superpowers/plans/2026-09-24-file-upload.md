# 公共文件上传与业务附件关联 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Spring Boot 中转上传至 MinIO 的公共文件服务，并以 fileId 将图片和视频安全接入报修、维修过程/结果与返工流程。

**Architecture:** 新增独立文件模块封装 MinIO、文件元数据、动态预览、删除与孤儿清理；业务服务只调用 `FileService` 完成文件校验和绑定。四张新表表达公共文件与三类业务记录的关系，现有 JSON 地址字段保留只读兼容但新写入改用关联表。

**Tech Stack:** Java 17、Spring Boot 3、Spring MVC Multipart、Spring Security、MyBatis、MySQL 8、MinIO Java SDK、Spring Scheduling、JUnit 5、Mockito、Maven。

**Spec:** `docs/superpowers/specs/2026-09-24-file-upload-design.md`

## Global Constraints

- 只开发后端；前端职责只写入接口说明，不提交前端代码。
- 上传链路固定为前端 -> Spring Boot -> 私有 MinIO Bucket `repair-files`。
- 图片支持 jpg/jpeg/png/webp，最大 10MB；视频支持 mp4/mov/webm，最大 200MB；扩展名与 MIME 必须同时匹配。
- 学生只允许 REPAIR/REWORK，维修人员只允许 PROCESS，管理员不开放业务上传。
- 业务请求只提交 fileId；一个文件只能绑定一次，必须属于当前用户且业务类型匹配。
- 现有 `image_urls` 字段不删除、不迁移，新增流程不再写入。
- 每新增或变更接口同步更新 `doc/api/前端联调接口说明.md`，并维护本阶段验收记录与截图。

## Review Focus

- 原始文件名为 `../../x.jpg` 或 Windows 完整路径时只保存安全文件名，对象名仍完全由 UUID 生成。
- 扩展名与 MIME 仅一方合法时必须拒绝，不得因浏览器声明的 MIME 合法而接受 `.exe`。
- MinIO 上传成功而数据库插入失败时必须补偿删除对象；补偿失败不得泄露凭据或签名 URL。
- 两个业务请求并发绑定同一 fileId 时只能一个成功，另一个返回 409，不能生成双重关联。
- 孤儿清理与业务绑定竞争时必须重新检查绑定状态，绝不删除已经绑定的文件。

---

### Task 1: 文件表结构与 MyBatis 基础

**Files:**
- Create: `doc/database/文件上传数据库变更.sql`
- Modify: `doc/database/dorm_repair_schema.sql`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/SysFile.java`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/RepairOrderFile.java`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/RepairProcessFile.java`
- Create: `backend/src/main/java/com/dormrepair/domain/entity/RepairReworkFile.java`
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/SysFileMapper.java`
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/RepairOrderFileMapper.java`
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/RepairProcessFileMapper.java`
- Create: `backend/src/main/java/com/dormrepair/domain/mapper/RepairReworkFileMapper.java`
- Create: matching XML files under `backend/src/main/resources/mapper/`
- Modify/Test: database contract and consistency tests

**Interfaces:**
- Produces: `SysFileMapper.insert/selectById/selectByIdForUpdate/markDeleted/selectExpiredCandidateIds` and relation Mapper `batchInsert/selectBy.../existsByFileId` methods.

- [ ] **Step 1: Write failing schema and Mapper contract tests** asserting exactly 25 formal tables, `uk_bucket_object`, all three relation unique indexes, required Mapper statements, no DROP/foreign keys.
- [ ] **Step 2: Run** `mvn '-Dtest=SchemaSqlContractTest,MapperXmlContractTest,EntitySchemaConsistencyTest' test`; expect failures for missing tables/classes.
- [ ] **Step 3: Add idempotent SQL** for `sys_file`, `repair_order_file`, `repair_process_file`, `repair_rework_file` exactly as the spec, with InnoDB/utf8mb4 and indexes.
- [ ] **Step 4: Implement four entities and Mapper contracts**, including generated IDs, `FOR UPDATE` lookup, batch relation insert and cross-table bound checks.
- [ ] **Step 5: Apply incremental SQL to Docker MySQL** and run the three contract tests; expect PASS and 25/25 formal tables.
- [ ] **Step 6: Commit** `feat: 增加公共文件与业务附件数据结构`.

### Task 2: 配置、枚举与错误契约

**Files:**
- Modify: `MinioProperties.java`, `MinioConfig.java`, `application.yml`, `.env.example`, `scripts/start-backend.ps1`
- Create: `FileUploadProperties.java`, `FileTypeEnum.java`, `FileBizTypeEnum.java`
- Modify: `ResultCodeEnum.java`, `BusinessException.java`, `GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/dormrepair/file/FileUploadContractTest.java`

**Interfaces:**
- Produces: `FileBizTypeEnum.fromCode(String)`、`FileUploadProperties`、文件专项错误码和 Multipart 超限响应。

- [ ] **Step 1: Write failing tests** for enum parsing, role allowance, configured extensions/MIME/limits, file errors mapped to 400/403/404/409/500, and `MaxUploadSizeExceededException` mapped to HTTP 400 unified `Result`.
- [ ] **Step 2: Run** `mvn -Dtest=FileUploadContractTest test`; expect compile/assertion failures.
- [ ] **Step 3: Extend existing `app.minio` properties** with `bucketName=repair-files`, preview expiry and `autoCreateBucket`; add `app.file-upload` allowlists, sizes and orphan retention.
- [ ] **Step 4: Add Spring multipart limits** `max-file-size: 200MB`, `max-request-size: 210MB`; expose corresponding environment-variable defaults without secrets.
- [ ] **Step 5: Implement enums and errors**: empty, invalid biz type, disallowed type, too large, upload failed, not found, not owner, already bound, delete failed.
- [ ] **Step 6: Run tests and commit** `feat: 定义文件上传配置与异常契约`.

### Task 3: 公共上传、预览与 MinIO 补偿

**Files:**
- Create: `file/controller/FileController.java`
- Create: `file/service/FileService.java`, `FileServiceImpl.java`
- Create: `file/vo/FileUploadResponse.java`, `FileResponse.java`
- Create: `file/service/MinioBucketInitializer.java`
- Test: `FileServiceTest.java`, `FileControllerTest.java`, `FileMinioIntegrationTest.java`

**Interfaces:**
- Produces: `FileUploadResponse upload(MultipartFile, FileBizTypeEnum)`、`String getPreviewUrl(Long)`、`FileResponse getFile(Long)`.

- [ ] **Step 1: Write failing unit tests** for empty file, sanitized Unix/Windows path names, uppercase extensions, extension/MIME mismatch, image/video size boundaries, UUID object path and role matrix.
- [ ] **Step 2: Write failing compensation test**: mock `SysFileMapper.insert` failure after successful `putObject`, verify `removeObject` called once and client receives FILE_UPLOAD_FAILED.
- [ ] **Step 3: Implement validation and object naming**; use `UserContext`, never use original name in object path, and stream `MultipartFile` to MinIO without loading full content into memory.
- [ ] **Step 4: Implement upload transaction and compensation**, structured safe logging, private presigned GET URL and bucket initialization.
- [ ] **Step 5: Expose authenticated `POST /api/files/upload`** with `file` and `bizType`, returning unified `Result<FileUploadResponse>`.
- [ ] **Step 6: Run real MinIO integration tests** for one PNG and one MP4 fixture, assert object and `sys_file`, then remove test objects.
- [ ] **Step 7: Commit** `feat: 实现Spring Boot中转MinIO文件上传`.

### Task 4: 未绑定删除与孤儿文件清理

**Files:**
- Modify: `FileService.java`, `FileServiceImpl.java`, `FileController.java`
- Create: `file/job/OrphanFileCleanupJob.java`
- Test: `FileLifecycleServiceTest.java`, `OrphanFileCleanupJobTest.java`, `FileLifecycleMinioIntegrationTest.java`

**Interfaces:**
- Produces: `deleteUnusedFile(Long)`、`cleanupExpiredOrphans(int)` and `DELETE /api/files/{fileId}`.

- [ ] **Step 1: Write failing tests** for owner deletion, non-owner 403, bound 409, missing 404, MinIO delete failure preserving DB row, and successful MinIO deletion preceding logical deletion.
- [ ] **Step 2: Write cleanup tests** for older/younger than 24 hours, bound exclusion, per-item failure continuation, batch limit and cleanup/binding race recheck.
- [ ] **Step 3: Implement cross-relation `isBound`** and locked delete flow; do not mark deleted when MinIO removal fails.
- [ ] **Step 4: Implement scheduled bounded cleanup** using configuration duration/batch size and the same safe deletion primitive.
- [ ] **Step 5: Run unit plus real MinIO lifecycle tests and commit** `feat: 实现未绑定文件删除与孤儿清理`.

### Task 5: 学生报修绑定 fileIds

**Files:**
- Modify: `CreateRepairOrderRequest.java`, `StudentRepairOrderCreationService.java`, `RepairOrderCreationTransactionService.java`, `RepairOrderRequestHasher.java`
- Modify: relevant order detail VO/service later consumed by Task 8
- Test: `Plan4ContractTest.java`, `Plan4MySqlRedisIntegrationTest.java`, `RepairOrderFileBindingMySqlTest.java`

**Interfaces:**
- Consumes: `FileService.bindOrderFiles(orderId,fileIds,currentUserId)`.
- Produces: create request field `List<Long> fileIds`, max 9, and transactional order attachment binding.

- [ ] **Step 1: Write failing tests** for request validation, hash including normalized fileIds, valid REPAIR binding, other-owner/type mismatch/missing/deleted/already-bound rejection and duplicate IDs normalization.
- [ ] **Step 2: Change request contract** from `imageUrls` to `fileIds`; remove new writes to `repair_order.image_urls` while keeping legacy read compatibility.
- [ ] **Step 3: Bind files after generated order ID inside the existing creation transaction** so relation failure rolls back both order and relations.
- [ ] **Step 4: Add a real MySQL concurrency test** where two creates attempt the same fileId; assert only one relation and one business success.
- [ ] **Step 5: Run PLAN-4 regression plus binding tests and commit** `feat: 接入报修附件fileId绑定`.

### Task 6: 维修过程和维修结果绑定 fileIds

**Files:**
- Modify: `AddRepairProcessRequest.java`, `SubmitRepairResultRequest.java`, `WorkerRepairOrderCommandService.java`
- Modify: `RepairProcessRecordMapper.java/xml` only if generated ID support is missing
- Test: `WorkerRepairOrderCommandServiceTest.java`, `Plan6MySqlIntegrationTest.java`, `RepairProcessFileBindingMySqlTest.java`

**Interfaces:**
- Consumes: `FileService.bindProcessFiles(processId,fileIds,currentUserId)`.
- Produces: `AddRepairProcessRequest.fileIds` and `SubmitRepairResultRequest.fileIds`.

- [ ] **Step 1: Write failing tests** for PROCESS binding on normal process and submit-result records, student-owned or REPAIR files rejected, duplicate fileIds normalized and relation insert rollback.
- [ ] **Step 2: Change both request fields to `fileIds`** and ensure `RepairProcessRecordMapper.insert` returns the generated record ID.
- [ ] **Step 3: Refactor `addProcessEntity` to return the process record**, then bind files in the same existing transaction; stop writing new `image_urls` JSON.
- [ ] **Step 4: Run PLAN-6 regression and real MySQL binding tests; commit** `feat: 接入维修过程与结果附件绑定`.

### Task 7: 学生返工绑定 fileIds

**Files:**
- Modify: `CreateReworkRequest.java`, `StudentRepairOrderTransactionService.java`
- Test: `StudentRepairOrderCommandServiceTest.java`, `Plan7MySqlIntegrationTest.java`, `RepairReworkFileBindingMySqlTest.java`

**Interfaces:**
- Consumes: `FileService.bindReworkFiles(reworkId,fileIds,currentUserId)`.
- Produces: `CreateReworkRequest.fileIds` and transactional rework attachment binding.

- [ ] **Step 1: Write failing tests** for valid REWORK files, PROCESS/REPAIR type mismatch, other-owner, already-bound, max 9, duplicate IDs and binding failure rollback of order status/count/record/alert/flow/log.
- [ ] **Step 2: Change request to `fileIds`**, obtain generated rework ID from current Mapper insert, and stop writing new `image_urls` JSON.
- [ ] **Step 3: Bind inside the PLAN-7 transaction after rework insert and before completion**, preserving existing warning/exception rules.
- [ ] **Step 4: Run PLAN-7 unit, MySQL and concurrency regression tests; commit** `feat: 接入返工附件fileId绑定`.

### Task 8: 工单详情附件聚合与临时预览

**Files:**
- Create: `order/vo/RepairAttachmentResponse.java`
- Modify: `RepairOrderBaseInfoResponse.java`, process/rework detail VO as actually used by `RepairOrderDetailService`
- Modify: `RepairOrderDetailService.java` and relation query Mappers
- Test: `RepairOrderDetailServiceTest.java`, `RepairOrderAttachmentDetailMySqlTest.java`

**Interfaces:**
- Consumes: `FileService.listOrderFiles/listProcessFiles/listReworkFiles` returning preview-enriched `FileResponse`.
- Produces: structured `files` arrays at order, process/result and rework levels while preserving legacy `imageUrls` reads.

- [ ] **Step 1: Write failing detail tests** for all three attachment locations, IMAGE/VIDEO metadata, temporary URLs, empty arrays, legacy URL compatibility and existing role-based detail trimming.
- [ ] **Step 2: Add focused relation queries** preserving sort order and filtering deleted/failed files.
- [ ] **Step 3: Aggregate attachments only after existing order access checks**, generate fresh presigned URLs and never persist/log them.
- [ ] **Step 4: Run detail/security/MySQL tests and commit** `feat: 在工单详情返回结构化附件`.

### Task 9: 接口、架构、安全、数据库和验收文档

**Files:**
- Create: `doc/api/公共文件上传接口.md`
- Create: `doc/architecture/公共文件存储与附件关联设计.md`
- Create: `doc/security/文件上传与访问安全.md`
- Create: `doc/rules/addons/minio.md`
- Create: `doc/verify/文件上传验收记录.md`
- Modify: `doc/api/前端联调接口说明.md`, database docs, `README.md`

**Interfaces:**
- Documents: upload/delete, changed fileIds contracts, detail file structures, errors, frontend responsibilities, 25 formal tables and lifecycle.

- [ ] **Step 1: Document upload and delete interfaces** with multipart examples, unified responses, role matrix, validation and all error scenarios.
- [ ] **Step 2: Update the three existing business contracts** from URL arrays to fileIds and explain the required frontend sequence: select -> upload -> preview -> collect fileId -> submit business -> handle deletion/loading/progress.
- [ ] **Step 3: Document private Bucket, presigned URL, ownership/binding security, compensation and orphan cleanup; update database count/indexes and cross-computer startup configuration.**
- [ ] **Step 4: Create verification record skeleton** with commands, expected evidence and screenshot names.
- [ ] **Step 5: Commit** `docs: 完善文件上传联调与安全说明`.

### Task 10: 最终验收、截图与最新版服务

**Files:**
- Create: `doc/verify/images/文件上传-图片视频成功上传.png`
- Create: `doc/verify/images/文件上传-格式大小与权限校验.png`
- Create: `doc/verify/images/文件上传-三类业务附件绑定.png`
- Create: `doc/verify/images/文件上传-重复绑定并发测试.png`
- Create: `doc/verify/images/文件上传-删除补偿与孤儿清理.png`
- Create: `doc/verify/images/文件上传-Maven全量测试.png`
- Create: `doc/verify/images/文件上传-Maven打包.png`
- Create: `doc/verify/images/文件上传-接口冒烟与健康检查.png`
- Modify: `doc/verify/文件上传验收记录.md`

**Interfaces:**
- Produces: 可复核的真实 MySQL/MinIO/HTTP 证据和运行中的最新版 8811 后端。

- [ ] **Step 1: Run all file-focused unit, contract, MySQL and MinIO tests**; include extension/MIME mismatch, exact size boundaries, compensation and concurrency.
- [ ] **Step 2: Run full `mvn test` with local environment mappings** and require zero failures/errors.
- [ ] **Step 3: Stop the old 8811 process, run `mvn -DskipTests package`, and start the latest backend hidden through the existing script.**
- [ ] **Step 4: Use real student and worker tokens** to upload a small image/video, bind across all three actual workflows, verify previews and role errors; use only generated local test data.
- [ ] **Step 5: Verify deletion and cleanup** against real MinIO and DB, then request `/api/health` and record HTTP 200/code 0/status UP/PID.
- [ ] **Step 6: Save semantically named screenshots**, update actual test counts/results/uncompleted scope, run `git diff --check` and ensure `.env`, logs, JARs and fixtures are not staged.
- [ ] **Step 7: Commit** `test: 完成文件上传阶段验收`.
