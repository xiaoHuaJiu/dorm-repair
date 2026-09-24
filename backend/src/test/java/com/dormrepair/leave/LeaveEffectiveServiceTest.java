package com.dormrepair.leave;

import com.dormrepair.common.enums.WorkerWorkStatusEnum;
import com.dormrepair.config.LeaveProperties;
import com.dormrepair.dispatch.model.DispatchResult;
import com.dormrepair.dispatch.model.DispatchSourceType;
import com.dormrepair.dispatch.service.RepairDispatchService;
import com.dormrepair.domain.entity.RepairLeaveRequest;
import com.dormrepair.domain.mapper.RepairDispatchAlertMapper;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.domain.mapper.RepairWorkerMapper;
import com.dormrepair.leave.service.LeaveEffectiveService;
import com.dormrepair.leave.service.LeaveTransactionService;
import com.dormrepair.worker.vo.WorkerDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PLAN-9 请假生效服务单元测试：CAS 抢任务、逐单转派、汇总终态、部分失败提醒（暂未运行）。
 */
class LeaveEffectiveServiceTest {
    private RepairLeaveRequestMapper leaves;
    private RepairOrderMapper orders;
    private RepairWorkerMapper workers;
    private RepairDispatchService dispatch;
    private LeaveTransactionService transactions;
    private RepairDispatchAlertMapper alerts;
    private LeaveEffectiveService service;

    @BeforeEach
    void setup() {
        leaves = mock(RepairLeaveRequestMapper.class);
        orders = mock(RepairOrderMapper.class);
        workers = mock(RepairWorkerMapper.class);
        dispatch = mock(RepairDispatchService.class);
        transactions = mock(LeaveTransactionService.class);
        alerts = mock(RepairDispatchAlertMapper.class);
        service = new LeaveEffectiveService(leaves, orders, workers, dispatch, transactions, alerts, new LeaveProperties());
    }

    private RepairLeaveRequest approvedLeave(long id) {
        RepairLeaveRequest leave = new RepairLeaveRequest();
        leave.setId(id);
        leave.setWorkerId(7L);
        leave.setStatus(1);
        leave.setStartTime(LocalDateTime.now().minusMinutes(5));
        leave.setEndTime(LocalDateTime.now().plusDays(1));
        return leave;
    }

    @Test
    void skippedWhenCasTakeFails() {
        when(leaves.casTakeForEffective(101L, any())).thenReturn(0);
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(workers, never()).casUpdateStatus(anyLong(), anyInt(), anyInt());
        verify(dispatch, never()).dispatch(anyLong(), any(), any());
    }

    @Test
    void marksWorkerOnLeaveAndReassignsEachOrder() {
        RepairLeaveRequest leave = approvedLeave(101L);
        when(leaves.casTakeForEffective(101L, any())).thenReturn(1);
        when(leaves.selectById(101L)).thenReturn(leave);
        when(orders.selectOpenOrderIdsByAssignee(7L, List.of(1, 2, 4, 5))).thenReturn(List.of(301L, 302L));
        when(dispatch.dispatch(eq(301L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenReturn(DispatchResult.success(8L));
        when(dispatch.dispatch(eq(302L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenReturn(DispatchResult.success(9L));
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(workers).casUpdateStatus(7L, WorkerWorkStatusEnum.NORMAL.getCode(), WorkerWorkStatusEnum.ON_LEAVE.getCode());
        verify(transactions).logLeaveEffective(leave);
        verify(leaves).casUpdateReassignStatus(101L, 1, 2);
        verify(alerts, never()).insertSummaryAlert(any());
    }

    @Test
    void partialFailureRecordsSummaryAlertAndMarksPartialFailed() {
        RepairLeaveRequest leave = approvedLeave(101L);
        when(leaves.casTakeForEffective(101L, any())).thenReturn(1);
        when(leaves.selectById(101L)).thenReturn(leave);
        when(orders.selectOpenOrderIdsByAssignee(7L, List.of(1, 2, 4, 5))).thenReturn(List.of(301L, 302L, 303L));
        when(dispatch.dispatch(eq(301L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenReturn(DispatchResult.success(8L));
        when(dispatch.dispatch(eq(302L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenReturn(DispatchResult.failure(com.dormrepair.dispatch.model.DispatchFailureReason.NO_AVAILABLE_WORKER));
        when(dispatch.dispatch(eq(303L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenThrow(new IllegalStateException("boom"));
        when(workers.selectDetail(7L)).thenReturn(new WorkerDetailResponse(7L, 21L, "worker9", "张师傅", null, "W009", 1, 1, null));
        when(leaves.casUpdateReassignStatus(101L, 1, 3)).thenReturn(1);
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(leaves).casUpdateReassignStatus(101L, 1, 3);
        verify(alerts).insertSummaryAlert(any());
    }

    @Test
    void singleOrderFailureIsIsolatedAndNextOrderStillDispatched() {
        RepairLeaveRequest leave = approvedLeave(101L);
        when(leaves.casTakeForEffective(101L, any())).thenReturn(1);
        when(leaves.selectById(101L)).thenReturn(leave);
        when(orders.selectOpenOrderIdsByAssignee(7L, List.of(1, 2, 4, 5))).thenReturn(List.of(301L, 302L));
        when(dispatch.dispatch(eq(301L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenThrow(new RuntimeException("boom"));
        when(dispatch.dispatch(eq(302L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenReturn(DispatchResult.success(9L));
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(dispatch).dispatch(eq(302L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN));
        verify(leaves).casUpdateReassignStatus(101L, 1, 3);
    }

    @Test
    void noOrdersMeansCompletedWithoutDispatch() {
        RepairLeaveRequest leave = approvedLeave(101L);
        when(leaves.casTakeForEffective(101L, any())).thenReturn(1);
        when(leaves.selectById(101L)).thenReturn(leave);
        when(orders.selectOpenOrderIdsByAssignee(7L, List.of(1, 2, 4, 5))).thenReturn(List.of());
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(dispatch, never()).dispatch(anyLong(), any(), any());
        verify(leaves).casUpdateReassignStatus(101L, 1, 2);
        verify(alerts, never()).insertSummaryAlert(any());
    }

    @Test
    void losingFinalCasWritesNoSummaryAlert() {
        RepairLeaveRequest leave = approvedLeave(101L);
        when(leaves.casTakeForEffective(101L, any())).thenReturn(1);
        when(leaves.selectById(101L)).thenReturn(leave);
        when(orders.selectOpenOrderIdsByAssignee(7L, List.of(1, 2, 4, 5))).thenReturn(List.of(301L));
        when(dispatch.dispatch(eq(301L), eq(Set.of(7L)), eq(DispatchSourceType.LEAVE_REASSIGN)))
            .thenReturn(DispatchResult.failure(com.dormrepair.dispatch.model.DispatchFailureReason.ORDER_STATE_CHANGED));
        // 终态 CAS 未命中（并发进程已闭环），即使存在失败也不应再写汇总提醒
        when(leaves.casUpdateReassignStatus(101L, 1, 3)).thenReturn(0);
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(alerts, never()).insertSummaryAlert(any());
    }

    @Test
    void alreadyEndedLeaveClosesWithoutChangingWorkerStatus() {
        RepairLeaveRequest leave = approvedLeave(101L);
        leave.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(leaves.casTakeForEffective(101L, any())).thenReturn(1);
        when(leaves.selectById(101L)).thenReturn(leave);
        service.processOne(101L, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5));
        verify(workers, never()).casUpdateStatus(anyLong(), anyInt(), anyInt());
        verify(leaves).casUpdateReassignStatus(101L, 1, 2);
    }
}
