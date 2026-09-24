package com.dormrepair.leave.service;

import com.dormrepair.common.enums.LeaveApprovalStatusEnum;
import com.dormrepair.common.enums.LeaveReassignStatusEnum;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairLeaveRequest;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.entity.SysOperationLog;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.domain.mapper.SysOperationLogMapper;
import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.dto.ReviewLeaveRequestDTO;
import com.dormrepair.leave.vo.CreateLeaveRequestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 请假领域写操作的数据库事务载体：
 * 创建申请、审批、请假生效与结束恢复的操作日志均在本类中以独立事务落库。
 */
@Service
public class LeaveTransactionService {
    public static final String BIZ_TYPE_CREATE = "LEAVE_REQUEST_CREATE";
    public static final String BIZ_TYPE_REVIEW = "LEAVE_REQUEST_REVIEW";
    private static final int SYSTEM_ROLE = 4;

    private final RepairLeaveRequestMapper leaves;
    private final SysIdempotentRecordMapper idempotency;
    private final SysOperationLogMapper logs;
    private final ObjectMapper json;

    public LeaveTransactionService(RepairLeaveRequestMapper leaves, SysIdempotentRecordMapper idempotency,
        SysOperationLogMapper logs, ObjectMapper json) {
        this.leaves = leaves; this.idempotency = idempotency; this.logs = logs; this.json = json;
    }

    /**
     * 创建请假申请：幂等记录与请假记录、操作日志同事务提交。
     */
    @Transactional
    public CreateLeaveRequestResponse createLeave(Long userId, String username, Long workerId, String hash, CreateLeaveRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        SysIdempotentRecord idem = new SysIdempotentRecord();
        idem.setBizNo(request.bizNo()); idem.setBizType(BIZ_TYPE_CREATE); idem.setUserId(userId);
        idem.setRequestHash(hash); idem.setStatus(0); idem.setExpireTime(now.plusDays(1));
        idempotency.insert(idem);
        RepairLeaveRequest leave = new RepairLeaveRequest();
        leave.setWorkerId(workerId);
        leave.setStartTime(request.startTime()); leave.setEndTime(request.endTime());
        leave.setReason(request.reason());
        leave.setStatus(LeaveApprovalStatusEnum.PENDING.getCode());
        leave.setReassignStatus(LeaveReassignStatusEnum.PENDING.getCode());
        leave.setCreateTime(now); leave.setUpdateTime(now); leave.setDeleted(0);
        leaves.insert(leave);
        addLog(userId, username, 2, "提交请假申请", leave.getId(), "/api/worker/leave-requests", "POST", now);
        CreateLeaveRequestResponse result = new CreateLeaveRequestResponse(leave.getId(), leave.getStatus());
        try {
            if (idempotency.markSuccess(idem.getId(), json.writeValueAsString(result)) != 1) {
                throw new IllegalStateException("请假申请幂等记录状态更新失败");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("请假申请结果序列化失败", e);
        }
        return result;
    }

    /**
     * 审批请假：仅待审批状态可变更（CAS），通过/驳回不直接修改维修人员工作状态。
     */
    @Transactional
    public void reviewLeave(Long leaveId, Long adminUid, String username, String hash, ReviewLeaveRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        SysIdempotentRecord idem = new SysIdempotentRecord();
        idem.setBizNo(request.bizNo()); idem.setBizType(BIZ_TYPE_REVIEW); idem.setUserId(adminUid);
        idem.setRequestHash(hash); idem.setStatus(0); idem.setExpireTime(now.plusDays(1));
        idempotency.insert(idem);
        RepairLeaveRequest leave = leaves.selectByIdForUpdate(leaveId);
        if (leave == null || Integer.valueOf(1).equals(leave.getDeleted())) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND, "请假申请不存在");
        }
        if (!Integer.valueOf(LeaveApprovalStatusEnum.PENDING.getCode()).equals(leave.getStatus())) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT, "该请假申请已被审批");
        }
        Integer target = "APPROVE".equals(request.action())
            ? LeaveApprovalStatusEnum.APPROVED.getCode() : LeaveApprovalStatusEnum.REJECTED.getCode();
        if (leaves.casReview(leaveId, leave.getStatus(), target, adminUid, request.remark(), now) != 1) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT, "该请假申请已被审批");
        }
        addLog(adminUid, username, 3, "APPROVE".equals(request.action()) ? "审批通过请假" : "审批驳回请假",
            leaveId, "/api/admin/leave-requests/" + leaveId + "/review", "POST", now);
        try {
            if (idempotency.markSuccess(idem.getId(),
                json.writeValueAsString(java.util.Map.of("leaveId", leaveId, "approvalStatus", target))) != 1) {
                throw new IllegalStateException("请假审批幂等记录状态更新失败");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("请假审批结果序列化失败", e);
        }
    }

    /**
     * 记录请假正式生效（维修人员进入请假中）的系统操作日志。
     */
    @Transactional
    public void logLeaveEffective(RepairLeaveRequest leave) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(null); log.setUsername("SYSTEM"); log.setRoleType(SYSTEM_ROLE);
        log.setModuleName("维修请假"); log.setOperationName("请假生效进入请假中");
        log.setBusinessType("LEAVE_REQUEST"); log.setBusinessId(leave.getId());
        log.setRequestMethod("SCHEDULED"); log.setRequestUri("RepairLeaveEffectiveTask");
        log.setResultStatus(1); log.setOperationTime(LocalDateTime.now());
        logs.insert(log);
    }

    /**
     * 记录请假结束恢复正常工作的系统操作日志。
     */
    @Transactional
    public void logLeaveRestored(Long workerId) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(null); log.setUsername("SYSTEM"); log.setRoleType(SYSTEM_ROLE);
        log.setModuleName("维修请假"); log.setOperationName("请假结束恢复正常");
        log.setBusinessType("LEAVE_REQUEST"); log.setBusinessId(workerId);
        log.setRequestMethod("SCHEDULED"); log.setRequestUri("RepairLeaveEndTask");
        log.setResultStatus(1); log.setOperationTime(LocalDateTime.now());
        logs.insert(log);
    }

    private void addLog(Long userId, String username, int roleType, String operationName, Long businessId, String uri, String method, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(userId); log.setUsername(username); log.setRoleType(roleType);
        log.setModuleName("维修请假"); log.setOperationName(operationName);
        log.setBusinessType("LEAVE_REQUEST"); log.setBusinessId(businessId);
        log.setRequestMethod(method); log.setRequestUri(uri);
        log.setResultStatus(1); log.setOperationTime(now);
        logs.insert(log);
    }
}
