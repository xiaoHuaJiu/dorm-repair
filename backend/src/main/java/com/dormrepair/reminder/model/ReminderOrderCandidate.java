package com.dormrepair.reminder.model;
import java.time.LocalDateTime;
public record ReminderOrderCandidate(Long orderId,String orderNo,Long assigneeWorkerId,Long receiverUserId,LocalDateTime deadline) {}
