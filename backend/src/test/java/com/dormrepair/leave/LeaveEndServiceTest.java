package com.dormrepair.leave;

import com.dormrepair.common.enums.WorkerWorkStatusEnum;
import com.dormrepair.config.LeaveProperties;
import com.dormrepair.domain.entity.RepairWorker;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.RepairWorkerMapper;
import com.dormrepair.leave.service.LeaveEndService;
import com.dormrepair.leave.service.LeaveTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PLAN-9 请假结束恢复服务单元测试：正常恢复、连续请假保持、非请假中跳过（暂未运行）。
 */
class LeaveEndServiceTest {
    private RepairLeaveRequestMapper leaves;
    private RepairWorkerMapper workers;
    private LeaveTransactionService transactions;
    private LeaveEndService service;

    @BeforeEach
    void setup() {
        leaves = mock(RepairLeaveRequestMapper.class);
        workers = mock(RepairWorkerMapper.class);
        transactions = mock(LeaveTransactionService.class);
        service = new LeaveEndService(leaves, workers, transactions, new LeaveProperties());
    }

    private RepairWorker workerOnLeave() {
        RepairWorker worker = new RepairWorker();
        worker.setId(7L);
        worker.setWorkStatus(WorkerWorkStatusEnum.ON_LEAVE.getCode());
        return worker;
    }

    @Test
    void restoresWorkerWhenNoActiveLeave() {
        when(workers.selectById(7L)).thenReturn(workerOnLeave());
        when(leaves.countActiveLeave(7L, any())).thenReturn(0);
        when(workers.casUpdateStatus(7L, WorkerWorkStatusEnum.ON_LEAVE.getCode(), WorkerWorkStatusEnum.NORMAL.getCode())).thenReturn(1);
        service.restoreWorker(7L, LocalDateTime.now());
        verify(transactions).logLeaveRestored(7L);
    }

    @Test
    void keepsOnLeaveWhenBackToBackLeaveActive() {
        when(workers.selectById(7L)).thenReturn(workerOnLeave());
        when(leaves.countActiveLeave(7L, any())).thenReturn(1);
        service.restoreWorker(7L, LocalDateTime.now());
        verify(workers, never()).casUpdateStatus(anyLong(), anyInt(), anyInt());
        verify(transactions, never()).logLeaveRestored(anyLong());
    }

    @Test
    void skipsWorkerNotOnLeave() {
        RepairWorker worker = new RepairWorker();
        worker.setId(7L);
        worker.setWorkStatus(WorkerWorkStatusEnum.NORMAL.getCode());
        when(workers.selectById(7L)).thenReturn(worker);
        service.restoreWorker(7L, LocalDateTime.now());
        verify(leaves, never()).countActiveLeave(anyLong(), any());
        verify(workers, never()).casUpdateStatus(anyLong(), anyInt(), anyInt());
    }

    @Test
    void skipsMissingWorker() {
        when(workers.selectById(7L)).thenReturn(null);
        service.restoreWorker(7L, LocalDateTime.now());
        verifyNoInteractions(leaves);
        verify(workers, never()).casUpdateStatus(anyLong(), anyInt(), anyInt());
    }
}
