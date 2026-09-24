package com.dormrepair.leave.service;

import com.dormrepair.common.constant.RedisConstant;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.leave.dto.ReviewLeaveRequestDTO;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 管理员审批请假服务。
 * 幂等链路：bizNo 幂等（Redis 门闩 + sys_idempotent_record 重放）+
 * 审批状态 CAS（仅待审批可变更），并发审批只有一个成功，其余返回 409。
 */
@Service
public class LeaveReviewService {
    private static final Logger log = LoggerFactory.getLogger(LeaveReviewService.class);

    private final SysIdempotentRecordMapper records;
    private final LeaveRequestHasher hasher;
    private final LeaveTransactionService transactions;
    private final StringRedisTemplate redis;

    public LeaveReviewService(SysIdempotentRecordMapper records, LeaveRequestHasher hasher,
        LeaveTransactionService transactions, StringRedisTemplate redis) {
        this.records = records; this.hasher = hasher; this.transactions = transactions; this.redis = redis;
    }

    public void review(Long leaveId, ReviewLeaveRequestDTO request) {
        LoginUser user = UserContext.getCurrentUser();
        Long uid = user.getUserId();
        String hash = hasher.hashReview(uid, leaveId, request);
        Boolean replayed = replayReviewIfPresent(uid, request.bizNo(), hash);
        if (replayed != null) {
            if (replayed) return;
            throw new BusinessException(ResultCodeEnum.ORDER_BIZ_NO_CONFLICT, "当前提交标识已被使用，请刷新页面后重新提交");
        }
        String key = RedisConstant.leaveReviewKey(uid, request.bizNo());
        boolean acquired = tryAcquire(key);
        if (!acquired) {
            replayed = replayReviewIfPresent(uid, request.bizNo(), hash);
            if (replayed != null && replayed) return;
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING, "审批正在提交中，请勿重复操作");
        }
        try {
            try {
                transactions.reviewLeave(leaveId, uid, user.getUsername(), hash, request);
            } catch (DuplicateKeyException e) {
                replayed = replayReviewIfPresent(uid, request.bizNo(), hash);
                if (replayed == null || !replayed) {
                    throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
                }
            }
            markSuccess(key, leaveId);
        } finally {
            if (!isSuccess(key)) release(key);
        }
    }

    /**
     * @return true 已成功过（幂等重放成功）；false 摘要冲突；null 无记录
     */
    private Boolean replayReviewIfPresent(Long uid, String bizNo, String hash) {
        SysIdempotentRecord record = records.selectByBusinessKey(LeaveTransactionService.BIZ_TYPE_REVIEW, uid, bizNo);
        if (record == null) return null;
        if (!hash.equals(record.getRequestHash())) return false;
        if (!Integer.valueOf(1).equals(record.getStatus()) || record.getResultSnapshot() == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
        }
        return true;
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
}
