package com.dormrepair.leave.service;

import com.dormrepair.common.constant.RedisConstant;
import com.dormrepair.common.enums.LeaveApprovalStatusEnum;
import com.dormrepair.common.enums.LeaveReassignStatusEnum;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.result.PageResult;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.leave.dto.AdminLeaveRequestQuery;
import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.dto.LeaveRequestQuery;
import com.dormrepair.leave.vo.CreateLeaveRequestResponse;
import com.dormrepair.leave.vo.LeaveRequestDetailResponse;
import com.dormrepair.leave.vo.LeaveRequestListItem;
import com.dormrepair.order.service.CurrentWorkerResolver;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请创建与查询服务。
 * 创建链路：bizNo 幂等（Redis 门闩 + sys_idempotent_record 唯一约束与结果快照重放）
 * → 校验当前维修人员 → 时间校验 → 时间段重叠校验 → 落库。
 */
@Service
public class LeaveRequestService {
    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);
    private static final List<Integer> CONFLICT_STATUSES =
        List.of(LeaveApprovalStatusEnum.PENDING.getCode(), LeaveApprovalStatusEnum.APPROVED.getCode());

    private final RepairLeaveRequestMapper leaves;
    private final SysIdempotentRecordMapper records;
    private final LeaveRequestHasher hasher;
    private final LeaveTransactionService transactions;
    private final CurrentWorkerResolver workerResolver;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public LeaveRequestService(RepairLeaveRequestMapper leaves, SysIdempotentRecordMapper records,
        LeaveRequestHasher hasher, LeaveTransactionService transactions, CurrentWorkerResolver workerResolver,
        StringRedisTemplate redis, ObjectMapper json) {
        this.leaves = leaves; this.records = records; this.hasher = hasher;
        this.transactions = transactions; this.workerResolver = workerResolver;
        this.redis = redis; this.json = json;
    }

    public CreateLeaveRequestResponse create(CreateLeaveRequestDTO request) {
        LoginUser user = UserContext.getCurrentUser();
        Long uid = user.getUserId();
        Long workerId = workerResolver.resolveCurrentWorkerId();
        String hash = hasher.hashCreate(workerId, request);
        CreateLeaveRequestResponse previous = replayCreateIfPresent(uid, request.bizNo(), hash);
        if (previous != null) return previous;
        String key = RedisConstant.leaveCreateKey(uid, request.bizNo());
        boolean acquired = tryAcquire(key);
        if (!acquired) {
            previous = replayCreateIfPresent(uid, request.bizNo(), hash);
            if (previous != null) return previous;
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING, "请假申请正在提交中，请勿重复操作");
        }
        try {
            validateTime(request);
            if (leaves.countOverlap(workerId, request.startTime(), request.endTime(), CONFLICT_STATUSES) > 0) {
                throw new BusinessException(ResultCodeEnum.DATA_CONFLICT, "该时间段与已有待审批或已通过的请假重叠");
            }
            CreateLeaveRequestResponse result;
            try {
                result = transactions.createLeave(uid, user.getUsername(), workerId, hash, request);
            } catch (DuplicateKeyException e) {
                result = replayCreateIfPresent(uid, request.bizNo(), hash);
                if (result == null) throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
            }
            markSuccess(key, result.leaveId());
            return result;
        } finally {
            if (!isSuccess(key)) release(key);
        }
    }

    public PageResult<LeaveRequestListItem> pageForWorker(LeaveRequestQuery query) {
        Long workerId = workerResolver.resolveCurrentWorkerId();
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<LeaveRequestListItem> values = leaves.selectWorkerPage(workerId, query.getApprovalStatus());
        PageInfo<LeaveRequestListItem> info = new PageInfo<>(values);
        values.forEach(this::fillStatusNames);
        return PageResult.of(info.getTotal(), info.getPageNum(), info.getPageSize(), values);
    }

    public LeaveRequestDetailResponse detailForWorker(Long id) {
        Long workerId = workerResolver.resolveCurrentWorkerId();
        LeaveRequestDetailResponse detail = leaves.selectDetailByWorker(id, workerId);
        if (detail == null) throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND, "请假申请不存在");
        fillDetailNames(detail);
        return detail;
    }

    public PageResult<LeaveRequestListItem> pageForAdmin(AdminLeaveRequestQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<LeaveRequestListItem> values = leaves.selectAdminPage(
            query.getWorkerId(), query.getApprovalStatus(), query.getStartTime(), query.getEndTime());
        PageInfo<LeaveRequestListItem> info = new PageInfo<>(values);
        values.forEach(this::fillStatusNames);
        return PageResult.of(info.getTotal(), info.getPageNum(), info.getPageSize(), values);
    }

    public LeaveRequestDetailResponse detailForAdmin(Long id) {
        LeaveRequestDetailResponse detail = leaves.selectDetailById(id);
        if (detail == null) throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND, "请假申请不存在");
        fillDetailNames(detail);
        return detail;
    }

    private void validateTime(CreateLeaveRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请假开始时间必须早于结束时间");
        }
        if (!request.endTime().isAfter(now)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请假结束时间必须晚于当前时间");
        }
    }

    private CreateLeaveRequestResponse replayCreateIfPresent(Long uid, String bizNo, String hash) {
        SysIdempotentRecord record = records.selectByBusinessKey(LeaveTransactionService.BIZ_TYPE_CREATE, uid, bizNo);
        if (record == null) return null;
        if (!hash.equals(record.getRequestHash())) {
            throw new BusinessException(ResultCodeEnum.ORDER_BIZ_NO_CONFLICT, "当前提交标识已被使用，请刷新页面后重新提交");
        }
        if (!Integer.valueOf(1).equals(record.getStatus()) || record.getResultSnapshot() == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
        }
        try {
            return json.readValue(record.getResultSnapshot(), CreateLeaveRequestResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("请假申请幂等结果快照无法读取", e);
        }
    }

    private boolean tryAcquire(String key) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "PROCESSING",
                Duration.ofMinutes(RedisConstant.LEAVE_IDEMPOTENT_TTL_MINUTES)));
        } catch (RuntimeException e) {
            log.warn("Redis 幂等门闩不可用，降级为 MySQL 唯一约束", e);
            return true;
        }
    }

    private void markSuccess(String key, Long leaveId) {
        try {
            redis.opsForValue().set(key, "SUCCESS:" + leaveId, Duration.ofMinutes(RedisConstant.LEAVE_IDEMPOTENT_TTL_MINUTES));
        } catch (RuntimeException e) {
            log.warn("Redis 幂等成功标记写入失败，MySQL 记录仍可重放", e);
        }
    }

    private boolean isSuccess(String key) {
        try {
            String value = redis.opsForValue().get(key);
            return value != null && value.startsWith("SUCCESS:");
        } catch (RuntimeException e) {
            return true;
        }
    }

    private void release(String key) {
        try {
            redis.delete(key);
        } catch (RuntimeException e) {
            log.warn("Redis 幂等处理中标记清理失败，等待 TTL 到期", e);
        }
    }

    private void fillStatusNames(LeaveRequestListItem item) {
        item.setApprovalStatusName(LeaveApprovalStatusEnum.fromCode(item.getApprovalStatus())
            .map(LeaveApprovalStatusEnum::getDescription).orElse("未知状态"));
        item.setReassignStatusName(LeaveReassignStatusEnum.fromCode(item.getReassignStatus())
            .map(LeaveReassignStatusEnum::getDescription).orElse("未知状态"));
    }

    private void fillDetailNames(LeaveRequestDetailResponse detail) {
        detail.setApprovalStatusName(LeaveApprovalStatusEnum.fromCode(detail.getApprovalStatus())
            .map(LeaveApprovalStatusEnum::getDescription).orElse("未知状态"));
        detail.setReassignStatusName(LeaveReassignStatusEnum.fromCode(detail.getReassignStatus())
            .map(LeaveReassignStatusEnum::getDescription).orElse("未知状态"));
    }
}
