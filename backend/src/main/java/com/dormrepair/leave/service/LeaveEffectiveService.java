package com.dormrepair.leave.service;

import com.dormrepair.common.enums.LeaveApprovalStatusEnum;
import com.dormrepair.common.enums.LeaveReassignStatusEnum;
import com.dormrepair.common.enums.WorkerWorkStatusEnum;
import com.dormrepair.config.LeaveProperties;
import com.dormrepair.dispatch.model.DispatchResult;
import com.dormrepair.dispatch.model.DispatchSourceType;
import com.dormrepair.dispatch.service.RepairDispatchService;
import com.dormrepair.domain.entity.RepairDispatchAlert;
import com.dormrepair.domain.entity.RepairLeaveRequest;
import com.dormrepair.domain.mapper.RepairDispatchAlertMapper;
import com.dormrepair.domain.mapper.RepairLeaveRequestMapper;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.domain.mapper.RepairWorkerMapper;
import com.dormrepair.worker.vo.WorkerDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 请假正式生效与工单转派核心逻辑，供 {@code RepairLeaveEffectiveTask} 定时调用。
 *
 * <p>处理链路（无整体大事务，各步骤独立提交）：
 * <ol>
 *   <li>CAS 抢任务：reassign_status 0→1（处理中超时允许 1→1 恢复执行），抢到才继续；</li>
 *   <li>维修人员 work_status → 请假中，并写操作日志；</li>
 *   <li>查询该人员待接单/维修中/返工中/已中断工单，逐单调用 RepairDispatchService，
 *       单工单异常隔离，不因个别失败中断后续；</li>
 *   <li>汇总统计：全部成功或无需转派 → 2 已完成；存在失败 → 3 部分失败并生成管理员汇总提醒。</li>
 * </ol>
 * 恢复执行时重新查询仍属于请假人员的工单，已成功转走的工单不会重复转派。
 */
@Service
public class LeaveEffectiveService {
    private static final Logger log = LoggerFactory.getLogger(LeaveEffectiveService.class);
    /** 需要转派的未完成工单状态：待接单、维修中、返工中、已中断（不含待确认）。 */
    private static final List<Integer> REASSIGNABLE_STATUSES = List.of(1, 2, 4, 5);
    private static final String SUMMARY_ALERT_REASON = "LEAVE_REASSIGN_PARTIAL_FAILED";

    private final RepairLeaveRequestMapper leaves;
    private final RepairOrderMapper orders;
    private final RepairWorkerMapper workers;
    private final RepairDispatchService dispatch;
    private final LeaveTransactionService transactions;
    private final RepairDispatchAlertMapper alerts;
    private final LeaveProperties properties;

    public LeaveEffectiveService(RepairLeaveRequestMapper leaves, RepairOrderMapper orders, RepairWorkerMapper workers,
        RepairDispatchService dispatch, LeaveTransactionService transactions, RepairDispatchAlertMapper alerts,
        LeaveProperties properties) {
        this.leaves = leaves; this.orders = orders; this.workers = workers; this.dispatch = dispatch;
        this.transactions = transactions; this.alerts = alerts; this.properties = properties;
    }

    public void processDueLeaves() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusMinutes(properties.getStaleMinutes());
        List<Long> ids = leaves.selectEffectiveIds(now, staleBefore, properties.getEffectiveBatchSize());
        for (Long id : ids) {
            try {
                processOne(id, now, staleBefore);
            } catch (RuntimeException e) {
                log.error("请假生效任务处理异常，leaveId={}", id, e);
            }
        }
    }

    public void processOne(Long leaveId, LocalDateTime now, LocalDateTime staleBefore) {
        // 第一步：CAS 抢任务，防止多线程重复执行同一请假单
        if (leaves.casTakeForEffective(leaveId, staleBefore) != 1) return;
        RepairLeaveRequest leave = leaves.selectById(leaveId);
        if (leave == null || !Integer.valueOf(LeaveApprovalStatusEnum.APPROVED.getCode()).equals(leave.getStatus())
            || leave.getEndTime() == null || !leave.getEndTime().isAfter(now)) {
            // 请假已结束或状态已变化，无需再进入请假状态，直接闭环
            leaves.casUpdateReassignStatus(leaveId, LeaveReassignStatusEnum.PROCESSING.getCode(),
                LeaveReassignStatusEnum.COMPLETED.getCode());
            return;
        }
        // 第二步：人员进入请假中（CAS 从正常置为请假中，停用人员不被覆盖），此后自动派单候选过滤将其排除
        workers.casUpdateStatus(leave.getWorkerId(), WorkerWorkStatusEnum.NORMAL.getCode(), WorkerWorkStatusEnum.ON_LEAVE.getCode());
        transactions.logLeaveEffective(leave);
        // 第三步：逐单转派，单工单异常隔离
        List<Long> orderIds = orders.selectOpenOrderIdsByAssignee(leave.getWorkerId(), REASSIGNABLE_STATUSES);
        int success = 0;
        int failed = 0;
        for (Long orderId : orderIds) {
            try {
                DispatchResult result = dispatch.dispatch(orderId, Set.of(leave.getWorkerId()), DispatchSourceType.LEAVE_REASSIGN);
                if (result.success()) success++;
                else failed++;
            } catch (RuntimeException e) {
                failed++;
                log.error("请假转派单工单异常，leaveId={}, orderId={}", leaveId, orderId, e);
            }
        }
        // 第四步：汇总终态（提醒只在本次 CAS 成功闭环时写入，避免并发处理时重复/误报）
        int finalStatus = failed == 0
            ? LeaveReassignStatusEnum.COMPLETED.getCode() : LeaveReassignStatusEnum.PARTIAL_FAILED.getCode();
        boolean closed = leaves.casUpdateReassignStatus(leaveId, LeaveReassignStatusEnum.PROCESSING.getCode(), finalStatus) == 1;
        if (failed > 0 && closed) {
            recordSummaryAlert(leave, orderIds.size(), success, failed);
        }
    }

    private void recordSummaryAlert(RepairLeaveRequest leave, int total, int success, int failed) {
        try {
            String workerName = resolveWorkerName(leave.getWorkerId());
            String detail = String.format(
                "维修人员%s（工号%s）的请假已生效（%s ~ %s），共需转派%d张工单，自动转派成功%d张、失败%d张，失败工单已进入待人工派单，请及时处理。",
                workerName, resolveWorkerNo(leave.getWorkerId()), leave.getStartTime(), leave.getEndTime(), total, success, failed);
            LocalDateTime now = LocalDateTime.now();
            RepairDispatchAlert alert = new RepairDispatchAlert();
            alert.setOrderId(null);
            alert.setSourceType(DispatchSourceType.LEAVE_REASSIGN.getCode());
            alert.setFailureReason(SUMMARY_ALERT_REASON);
            alert.setFailureDetail(detail.length() > 500 ? detail.substring(0, 500) : detail);
            alert.setFirstOccurredTime(now);
            alert.setLastOccurredTime(now);
            alerts.insertSummaryAlert(alert);
        } catch (RuntimeException e) {
            log.error("请假转派失败汇总提醒写入失败，leaveId={}", leave.getId(), e);
        }
    }

    private String resolveWorkerName(Long workerId) {
        WorkerDetailResponse detail = workers.selectDetail(workerId);
        return detail == null ? "未知" : detail.realName();
    }

    private String resolveWorkerNo(Long workerId) {
        WorkerDetailResponse detail = workers.selectDetail(workerId);
        return detail == null ? "未知" : detail.workerNo();
    }
}
