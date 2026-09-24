package com.dormrepair.leave;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.service.LeaveRequestHasher;
import com.dormrepair.leave.service.LeaveRequestService;
import com.dormrepair.leave.service.LeaveTransactionService;
import com.dormrepair.order.service.CurrentWorkerResolver;
import com.dormrepair.security.model.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PLAN-9 请假申请服务单元测试：时间校验、重叠冲突、bizNo 幂等重放（暂未运行）。
 */
class LeaveRequestServiceTest {
    private RepairLeaveRequestMapper leaves;
    private SysIdempotentRecordMapper records;
    private LeaveTransactionService transactions;
    private CurrentWorkerResolver workerResolver;
    private StringRedisTemplate redis;
    private LeaveRequestService service;
    private LeaveRequestHasher hasher;

    @BeforeEach
    void setup() {
        leaves = mock(RepairLeaveRequestMapper.class);
        records = mock(SysIdempotentRecordMapper.class);
        transactions = mock(LeaveTransactionService.class);
        workerResolver = mock(CurrentWorkerResolver.class);
        redis = mock(StringRedisTemplate.class);
        hasher = new LeaveRequestHasher();
        service = new LeaveRequestService(leaves, records, hasher, transactions, workerResolver, redis, new ObjectMapper());
        when(workerResolver.resolveCurrentWorkerId()).thenReturn(7L);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        loginWorker();
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginWorker() {
        LoginUser user = new LoginUser(21L, "worker9", "张师傅", UserRoleEnum.WORKER);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private CreateLeaveRequestDTO request(String bizNo, LocalDateTime start, LocalDateTime end) {
        return new CreateLeaveRequestDTO(bizNo, start, end, "家中有事");
    }

    @Test
    void createSuccessInsertsLeave() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime end = start.plusDays(2);
        when(leaves.countOverlap(eq(7L), any(), any(), anyList())).thenReturn(0);
        when(transactions.createLeave(eq(21L), eq("worker9"), eq(7L), anyString(), any()))
            .thenReturn(new com.dormrepair.leave.vo.CreateLeaveRequestResponse(101L, 0));
        com.dormrepair.leave.vo.CreateLeaveRequestResponse result = service.create(request("biz-1", start, end));
        assertThat(result.leaveId()).isEqualTo(101L);
        verify(transactions).createLeave(eq(21L), eq("worker9"), eq(7L), anyString(), any());
    }

    @Test
    void createRejectsInvalidTimeRange() {
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        assertThatThrownBy(() -> service.create(request("biz-2", end, end.minusHours(1))))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", ResultCodeEnum.PARAM_ERROR);
        assertThatThrownBy(() -> service.create(request("biz-3", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1))))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", ResultCodeEnum.PARAM_ERROR);
        verify(leaves, never()).insert(any());
    }

    @Test
    void createRejectsOverlappingPendingOrApprovedLeave() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime end = start.plusDays(2);
        when(leaves.countOverlap(eq(7L), any(), any(), anyList())).thenReturn(1);
        assertThatThrownBy(() -> service.create(request("biz-4", start, end)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", ResultCodeEnum.DATA_CONFLICT);
        verify(transactions, never()).createLeave(anyLong(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    void createReplaysExistingBizNo() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime end = start.plusDays(2);
        String hash = hasher.hashCreate(7L, request("biz-5", start, end));
        SysIdempotentRecord record = new SysIdempotentRecord();
        record.setBizNo("biz-5"); record.setBizType(LeaveTransactionService.BIZ_TYPE_CREATE); record.setUserId(21L);
        record.setRequestHash(hash); record.setStatus(1);
        record.setResultSnapshot("{\"leaveId\":102,\"approvalStatus\":0}");
        when(records.selectByBusinessKey(LeaveTransactionService.BIZ_TYPE_CREATE, 21L, "biz-5")).thenReturn(record);
        com.dormrepair.leave.vo.CreateLeaveRequestResponse result = service.create(request("biz-5", start, end));
        assertThat(result.leaveId()).isEqualTo(102L);
        verify(transactions, never()).createLeave(anyLong(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    void createRejectsBizNoConflictWithDifferentContent() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime end = start.plusDays(2);
        SysIdempotentRecord record = new SysIdempotentRecord();
        record.setBizNo("biz-6"); record.setBizType(LeaveTransactionService.BIZ_TYPE_CREATE); record.setUserId(21L);
        record.setRequestHash("different-hash"); record.setStatus(1);
        record.setResultSnapshot("{\"leaveId\":103,\"approvalStatus\":0}");
        when(records.selectByBusinessKey(LeaveTransactionService.BIZ_TYPE_CREATE, 21L, "biz-6")).thenReturn(record);
        assertThatThrownBy(() -> service.create(request("biz-6", start, end)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("code", ResultCodeEnum.ORDER_BIZ_NO_CONFLICT);
    }
}
