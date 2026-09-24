package com.dormrepair.leave.service;

import com.dormrepair.common.enums.WorkerWorkStatusEnum;
import com.dormrepair.config.LeaveProperties;
import com.dormrepair.domain.entity.RepairWorker;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.RepairWorkerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假结束自动恢复核心逻辑，供 {@code RepairLeaveEndTask} 定时调用。
 *
 * <p>恢复条件：存在已到结束时间的已通过请假，且该维修人员当前处于请假中，
 * 且当前时刻不存在其他生效请假（连续请假判断）。满足时 work_status 请假中 → 正常，
 * 恢复后该人员重新进入自动派单候选池；请假期间已转出的旧工单不会自动转回。
 */
@Service
public class LeaveEndService {
    private static final Logger log = LoggerFactory.getLogger(LeaveEndService.class);

    private final RepairLeaveRequestMapper leaves;
    private final RepairWorkerMapper workers;
    private final LeaveTransactionService transactions;
    private final LeaveProperties properties;

    public LeaveEndService(RepairLeaveRequestMapper leaves, RepairWorkerMapper workers,
        LeaveTransactionService transactions, LeaveProperties properties) {
        this.leaves = leaves; this.workers = workers; this.transactions = transactions; this.properties = properties;
    }

    public void processEndedLeaves() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> workerIds = leaves.selectWorkerIdsWithEndedLeave(now, properties.getEndBatchSize());
        for (Long workerId : workerIds) {
            try {
                restoreWorker(workerId, now);
            } catch (RuntimeException e) {
                log.error("请假结束恢复任务处理异常，workerId={}", workerId, e);
            }
        }
    }

    public void restoreWorker(Long workerId, LocalDateTime now) {
        RepairWorker worker = workers.selectById(workerId);
        if (worker == null || !Integer.valueOf(WorkerWorkStatusEnum.ON_LEAVE.getCode()).equals(worker.getWorkStatus())) {
            return;
        }
        // 连续请假：当前仍存在其他生效请假时保持请假中
        if (leaves.countActiveLeave(workerId, now) > 0) return;
        // CAS 恢复正常，防止并发重复恢复
        if (workers.casUpdateStatus(workerId, WorkerWorkStatusEnum.ON_LEAVE.getCode(),
            WorkerWorkStatusEnum.NORMAL.getCode()) != 1) return;
        transactions.logLeaveRestored(workerId);
    }
}
