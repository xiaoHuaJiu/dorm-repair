package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairReminderRecord {
    private Long id;
    private Long orderId;
    private Integer reminderType;
    private Integer reminderLevel;
    private LocalDateTime deadlineTime;
    private Long receiverId;
    private Integer sendStatus;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getReminderType() { return reminderType; }
    public void setReminderType(Integer reminderType) { this.reminderType = reminderType; }
    public Integer getReminderLevel() { return reminderLevel; }
    public void setReminderLevel(Integer reminderLevel) { this.reminderLevel = reminderLevel; }
    public LocalDateTime getDeadlineTime() { return deadlineTime; }
    public void setDeadlineTime(LocalDateTime deadlineTime) { this.deadlineTime = deadlineTime; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Integer getSendStatus() { return sendStatus; }
    public void setSendStatus(Integer sendStatus) { this.sendStatus = sendStatus; }
    public LocalDateTime getSendTime() { return sendTime; }
    public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
