package com.dormrepair.reminder.service;

import com.dormrepair.dispatch.model.DispatchSourceType;
import com.dormrepair.dispatch.service.RepairDispatchService;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.entity.RepairReminderRecord;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.domain.mapper.RepairReminderRecordMapper;
import com.dormrepair.domain.mapper.SysUserMapper;
import com.dormrepair.reminder.enums.ReminderLevel;
import com.dormrepair.reminder.enums.ReminderType;
import com.dormrepair.reminder.model.ReminderOrderCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class RepairReminderService {
    private static final Logger log = LoggerFactory.getLogger(RepairReminderService.class);
    private final RepairOrderMapper orders;
    private final RepairReminderRecordMapper reminders;
    private final SysUserMapper users;
    private final RepairDispatchService dispatch;
    private final int batchSize;

    public RepairReminderService(RepairOrderMapper orders, RepairReminderRecordMapper reminders,
                                 SysUserMapper users, RepairDispatchService dispatch,
                                 @Value("${app.task.batch-size:100}") int batchSize) {
        this.orders=orders; this.reminders=reminders; this.users=users; this.dispatch=dispatch; this.batchSize=batchSize;
    }

    public void createAcceptReminders(LocalDateTime scanTime, ReminderLevel level) {
        requireAdvanceLevel(level);
        process(orders.selectAcceptReminderCandidates(scanTime.plusMinutes(level.fromMinutes()),
            scanTime.plusMinutes(level.toMinutes()), batchSize), c -> create(c, ReminderType.ACCEPT, level, c.receiverUserId(), scanTime));
    }

    public void createCompleteReminders(LocalDateTime scanTime, ReminderLevel level) {
        requireAdvanceLevel(level);
        process(orders.selectCompleteReminderCandidates(scanTime.plusMinutes(level.fromMinutes()),
            scanTime.plusMinutes(level.toMinutes()), batchSize), c -> create(c, ReminderType.COMPLETE, level, c.receiverUserId(), scanTime));
    }

    public void createCompleteTimeoutReminders(LocalDateTime scanTime) {
        List<Long> admins = users.selectEnabledAdminIds();
        process(orders.selectCompleteTimeoutCandidates(scanTime, batchSize), candidate -> {
            create(candidate, ReminderType.COMPLETE, ReminderLevel.TIMEOUT, candidate.receiverUserId(), scanTime);
            for (Long adminId : admins) create(candidate, ReminderType.COMPLETE, ReminderLevel.TIMEOUT, adminId, scanTime);
        });
    }

    public void processAcceptTimeouts(LocalDateTime scanTime) {
        List<Long> admins = users.selectEnabledAdminIds();
        process(orders.selectAcceptTimeoutCandidates(scanTime, batchSize), candidate -> {
            RepairOrder current = orders.selectById(candidate.orderId());
            if (!stillAcceptTimeout(current, candidate, scanTime)) return;
            create(candidate, ReminderType.ACCEPT, ReminderLevel.TIMEOUT, candidate.receiverUserId(), scanTime);
            for (Long adminId : admins) create(candidate, ReminderType.ACCEPT, ReminderLevel.TIMEOUT, adminId, scanTime);
            dispatch.dispatch(candidate.orderId(), Set.of(candidate.assigneeWorkerId()), DispatchSourceType.ACCEPT_TIMEOUT);
        });
    }

    private boolean stillAcceptTimeout(RepairOrder order, ReminderOrderCandidate candidate, LocalDateTime scanTime) {
        return order != null && Integer.valueOf(1).equals(order.getStatus())
            && Objects.equals(candidate.assigneeWorkerId(), order.getCurrentAssigneeId())
            && Objects.equals(candidate.deadline(), order.getAcceptDeadline())
            && order.getAcceptDeadline() != null && !order.getAcceptDeadline().isAfter(scanTime);
    }

    private void create(ReminderOrderCandidate candidate, ReminderType type, ReminderLevel level, Long receiverId, LocalDateTime now) {
        if (receiverId == null) return;
        RepairReminderRecord record = new RepairReminderRecord();
        record.setOrderId(candidate.orderId()); record.setReminderType(type.code()); record.setReminderLevel(level.code());
        record.setDeadlineTime(candidate.deadline()); record.setReceiverId(receiverId); record.setSendStatus(1); record.setSendTime(now);
        reminders.insertIgnore(record);
    }

    private void process(List<ReminderOrderCandidate> candidates, CandidateAction action) {
        for (ReminderOrderCandidate candidate : candidates) {
            try { action.run(candidate); }
            catch (RuntimeException e) { log.error("定时提醒处理单张工单失败，orderId={}", candidate.orderId(), e); }
        }
    }

    private void requireAdvanceLevel(ReminderLevel level) {
        if (level == ReminderLevel.TIMEOUT) throw new IllegalArgumentException("到期提醒必须使用超时任务");
    }
    @FunctionalInterface private interface CandidateAction { void run(ReminderOrderCandidate candidate); }
}
