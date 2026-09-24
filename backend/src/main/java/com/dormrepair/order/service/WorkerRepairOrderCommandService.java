package com.dormrepair.order.service;

import com.dormrepair.common.enums.*;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.*;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.dto.*;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkerRepairOrderCommandService {
    private static final int WORKER_ROLE = 2;
    private final RepairOrderMapper orders;
    private final RepairProcessRecordMapper processes;
    private final RepairMaterialUsageMapper materials;
    private final RepairOrderFlowMapper flows;
    private final SysOperationLogMapper logs;
    private final CurrentWorkerResolver workerResolver;
    private final ObjectMapper objectMapper;

    public WorkerRepairOrderCommandService(RepairOrderMapper orders, RepairProcessRecordMapper processes,
        RepairMaterialUsageMapper materials, RepairOrderFlowMapper flows, SysOperationLogMapper logs,
        CurrentWorkerResolver workerResolver, ObjectMapper objectMapper) {
        this.orders = orders; this.processes = processes; this.materials = materials; this.flows = flows;
        this.logs = logs; this.workerResolver = workerResolver; this.objectMapper = objectMapper;
    }

    @Transactional
    public void accept(Long orderId, AcceptRepairOrderRequest request) {
        Actor actor = actor(); LocalDateTime now = LocalDateTime.now(); RepairOrder order = locked(orderId);
        requireOwner(order, actor.workerId()); requireStatus(order, RepairOrderStatusEnum.PENDING_ACCEPTANCE.getCode());
        if (order.getAcceptDeadline() != null && now.isAfter(order.getAcceptDeadline())) conflict("接单已超时，请刷新工单");
        LocalDateTime expected = request == null ? null : request.expectedCompleteTime();
        if (expected != null && !expected.isAfter(now)) throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "预计完成时间必须晚于接单时间");
        LocalDateTime deadline = expected == null ? now.plusHours(24) : expected;
        if (orders.casAccept(orderId, actor.workerId(), now, expected, deadline) != 1) conflict("工单状态已变化，请刷新后重试");
        addFlow(order, actor, RepairOrderOperationTypeEnum.ACCEPT, 1, 2, "维修人员接单", now);
        addLog(actor, orderId, "接单", "/api/worker/repair-orders/" + orderId + "/accept", now);
    }

    @Transactional
    public void addProcess(Long orderId, AddRepairProcessRequest request) {
        Actor actor = actor(); LocalDateTime now = LocalDateTime.now(); RepairOrder order = locked(orderId);
        requireOwner(order, actor.workerId()); requireStatus(order, 2, 4);
        addProcessEntity(orderId, actor.workerId(), ProcessRecordTypeEnum.NORMAL.getCode(), request.content(), request.imageUrls(), null, now);
        addLog(actor, orderId, "新增维修过程", "/api/worker/repair-orders/" + orderId + "/process-records", now);
    }

    @Transactional
    public void addMaterial(Long orderId, AddMaterialUsageRequest request) {
        Actor actor = actor(); LocalDateTime now = LocalDateTime.now(); RepairOrder order = locked(orderId);
        requireOwner(order, actor.workerId()); requireStatus(order, 2, 4);
        RepairMaterialUsage usage = new RepairMaterialUsage(); usage.setOrderId(orderId); usage.setWorkerId(actor.workerId());
        usage.setMaterialName(request.materialName()); usage.setSpecification(request.specification()); usage.setQuantity(request.quantity());
        usage.setUnit(request.unit()); usage.setRemark(request.remark()); usage.setUseTime(now); usage.setDeleted(0); materials.insert(usage);
        addLog(actor, orderId, "登记维修材料", "/api/worker/repair-orders/" + orderId + "/materials", now);
    }

    @Transactional
    public void interrupt(Long orderId, InterruptRepairRequest request) {
        Actor actor = actor(); LocalDateTime now = LocalDateTime.now(); RepairOrder order = locked(orderId);
        requireOwner(order, actor.workerId()); requireStatus(order, 2);
        if (orders.casInterrupt(orderId, actor.workerId()) != 1) conflict("工单状态已变化，请刷新后重试");
        addProcessEntity(orderId, actor.workerId(), ProcessRecordTypeEnum.INTERRUPT.getCode(), request.content(), null, request.interruptReasonType(), now);
        addFlow(order, actor, RepairOrderOperationTypeEnum.INTERRUPT, 2, 5, request.content(), now);
        addLog(actor, orderId, "中断维修", "/api/worker/repair-orders/" + orderId + "/interrupt", now);
    }

    @Transactional
    public void resume(Long orderId, ResumeRepairRequest request) {
        Actor actor = actor(); LocalDateTime now = LocalDateTime.now(); RepairOrder order = locked(orderId);
        requireOwner(order, actor.workerId()); requireStatus(order, 5);
        if (orders.casResume(orderId, actor.workerId()) != 1) conflict("工单状态已变化，请刷新后重试");
        String content = request == null || request.remark() == null || request.remark().isBlank() ? "恢复维修" : request.remark();
        addProcessEntity(orderId, actor.workerId(), ProcessRecordTypeEnum.RESUME.getCode(), content, null, null, now);
        addFlow(order, actor, RepairOrderOperationTypeEnum.RESUME, 5, 2, content, now);
        addLog(actor, orderId, "恢复维修", "/api/worker/repair-orders/" + orderId + "/resume", now);
    }

    @Transactional
    public void submitResult(Long orderId, SubmitRepairResultRequest request) {
        Actor actor = actor(); LocalDateTime now = LocalDateTime.now(); RepairOrder order = locked(orderId);
        requireOwner(order, actor.workerId()); requireStatus(order, 2, 4);
        if (orders.casSubmitResult(orderId, actor.workerId(), now) != 1) conflict("工单状态已变化，请刷新后重试");
        addProcessEntity(orderId, actor.workerId(), ProcessRecordTypeEnum.SUBMIT_RESULT.getCode(), request.resultDescription(), request.resultImageUrls(), null, now);
        addFlow(order, actor, RepairOrderOperationTypeEnum.SUBMIT_RESULT, order.getStatus(), 3, request.resultDescription(), now);
        addLog(actor, orderId, "提交维修结果", "/api/worker/repair-orders/" + orderId + "/submit-result", now);
    }

    private Actor actor() { LoginUser user = UserContext.getCurrentUser(); return new Actor(user, workerResolver.resolveCurrentWorkerId()); }
    private RepairOrder locked(Long id) { RepairOrder order = orders.selectByIdForUpdate(id); if (order == null || Integer.valueOf(1).equals(order.getDeleted())) throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND); return order; }
    private void requireOwner(RepairOrder order, Long workerId) { if (!workerId.equals(order.getCurrentAssigneeId())) throw new BusinessException(ResultCodeEnum.ORDER_ACCESS_DENIED); }
    private void requireStatus(RepairOrder order, int... allowed) { for (int status : allowed) if (Integer.valueOf(status).equals(order.getStatus())) return; conflict("当前工单状态不允许执行该操作"); }
    private void conflict(String message) { throw new BusinessException(ResultCodeEnum.DATA_CONFLICT, message); }

    private void addProcessEntity(Long orderId, Long workerId, int type, String content, List<String> images, Integer reason, LocalDateTime now) {
        RepairProcessRecord record = new RepairProcessRecord(); record.setOrderId(orderId); record.setWorkerId(workerId); record.setRecordType(type);
        record.setContent(content); record.setImageUrls(toJson(images)); record.setInterruptReasonType(reason); record.setRecordTime(now); record.setDeleted(0); processes.insert(record);
    }
    private String toJson(List<String> values) { if (values == null || values.isEmpty()) return null; try { return objectMapper.writeValueAsString(values); } catch (JsonProcessingException e) { throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "图片地址格式错误"); } }
    private void addFlow(RepairOrder order, Actor actor, RepairOrderOperationTypeEnum type, int from, int to, String reason, LocalDateTime now) {
        RepairOrderFlow flow = new RepairOrderFlow(); flow.setOrderId(order.getId()); flow.setOperationType(type.getCode()); flow.setFromStatus(from); flow.setToStatus(to);
        flow.setOriginalAssigneeId(actor.workerId()); flow.setNewAssigneeId(actor.workerId()); flow.setOperatorId(actor.user().getUserId()); flow.setOperatorRole(WORKER_ROLE);
        flow.setReason(reason); flow.setOperationTime(now); flows.insert(flow);
    }
    private void addLog(Actor actor, Long orderId, String name, String uri, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog(); log.setUserId(actor.user().getUserId()); log.setUsername(actor.user().getUsername()); log.setRoleType(WORKER_ROLE);
        log.setModuleName("维修工单"); log.setOperationName(name); log.setBusinessType("repair_order"); log.setBusinessId(orderId); log.setRequestMethod("POST"); log.setRequestUri(uri);
        log.setResultStatus(1); log.setOperationTime(now); logs.insert(log);
    }
    private record Actor(LoginUser user, Long workerId) {}
}
