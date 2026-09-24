package com.dormrepair.reminder;

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
import com.dormrepair.reminder.service.RepairReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class RepairReminderServiceTest {
    RepairOrderMapper orders;
    RepairReminderRecordMapper reminders;
    SysUserMapper users;
    RepairDispatchService dispatch;
    RepairReminderService service;

    @BeforeEach void setup() {
        orders = mock(RepairOrderMapper.class); reminders = mock(RepairReminderRecordMapper.class);
        users = mock(SysUserMapper.class); dispatch = mock(RepairDispatchService.class);
        service = new RepairReminderService(orders, reminders, users, dispatch, 100);
    }

    @Test void acceptTenMinuteReminderTargetsCurrentWorkerOnly() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 10, 0);
        var candidate = new ReminderOrderCandidate(1L, "RO1", 10L, 20L, now.plusMinutes(10));
        when(orders.selectAcceptReminderCandidates(now.plusMinutes(9), now.plusMinutes(10), 100)).thenReturn(List.of(candidate));
        service.createAcceptReminders(now, ReminderLevel.MINUS_10);
        verify(reminders).insertIgnore(argThat(r -> r.getOrderId() == 1L && r.getReceiverId() == 20L
            && r.getReminderType() == ReminderType.ACCEPT.code() && r.getReminderLevel() == ReminderLevel.MINUS_10.code()));
    }

    @Test void completeTimeoutRemindsWorkerAndAdminsWithoutChangingOrder() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 10, 0);
        var candidate = new ReminderOrderCandidate(2L, "RO2", 10L, 20L, now.minusMinutes(1));
        when(orders.selectCompleteTimeoutCandidates(now, 100)).thenReturn(List.of(candidate));
        when(users.selectEnabledAdminIds()).thenReturn(List.of(30L, 31L));
        service.createCompleteTimeoutReminders(now);
        verify(reminders, times(3)).insertIgnore(any(RepairReminderRecord.class));
        verify(orders, never()).casMarkPending(any(), any(), any());
        verifyNoInteractions(dispatch);
    }

    @Test void acceptTimeoutRevalidatesAndExcludesOriginalWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 10, 0);
        var candidate = new ReminderOrderCandidate(3L, "RO3", 10L, 20L, now.minusMinutes(1));
        when(orders.selectAcceptTimeoutCandidates(now, 100)).thenReturn(List.of(candidate));
        RepairOrder current = new RepairOrder(); current.setId(3L); current.setStatus(1); current.setCurrentAssigneeId(10L); current.setAcceptDeadline(candidate.deadline());
        when(orders.selectById(3L)).thenReturn(current); when(users.selectEnabledAdminIds()).thenReturn(List.of(30L));
        service.processAcceptTimeouts(now);
        verify(dispatch).dispatch(3L, Set.of(10L), DispatchSourceType.ACCEPT_TIMEOUT);
        verify(reminders, times(2)).insertIgnore(any(RepairReminderRecord.class));
    }

    @Test void acceptedOrderIsSkippedAfterTimeoutScan() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 10, 0);
        var candidate = new ReminderOrderCandidate(4L, "RO4", 10L, 20L, now.minusMinutes(1));
        when(orders.selectAcceptTimeoutCandidates(now, 100)).thenReturn(List.of(candidate));
        RepairOrder current = new RepairOrder(); current.setId(4L); current.setStatus(2); current.setCurrentAssigneeId(10L); current.setAcceptDeadline(candidate.deadline());
        when(orders.selectById(4L)).thenReturn(current);
        service.processAcceptTimeouts(now);
        verifyNoInteractions(dispatch, reminders);
    }

    @Test void oneBrokenOrderDoesNotStopTheBatch() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 10, 0);
        var first = new ReminderOrderCandidate(5L, "RO5", 10L, 20L, now.plusMinutes(5));
        var second = new ReminderOrderCandidate(6L, "RO6", 11L, 21L, now.plusMinutes(5));
        when(orders.selectCompleteReminderCandidates(now.plusMinutes(4), now.plusMinutes(5), 100)).thenReturn(List.of(first, second));
        when(reminders.insertIgnore(argThat(r -> r.getOrderId() == 5L))).thenThrow(new IllegalStateException("单条失败"));
        service.createCompleteReminders(now, ReminderLevel.MINUS_5);
        verify(reminders).insertIgnore(argThat(r -> r.getOrderId() == 6L && r.getReceiverId() == 21L));
    }
}
