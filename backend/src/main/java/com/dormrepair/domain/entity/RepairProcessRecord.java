package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairProcessRecord {
    private Long id;
    private Long orderId;
    private Long workerId;
    private Integer recordType;
    private String content;
    private String imageUrls;
    private Integer interruptReasonType;
    private LocalDateTime recordTime;
    private LocalDateTime createTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Integer getRecordType() { return recordType; }
    public void setRecordType(Integer recordType) { this.recordType = recordType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public Integer getInterruptReasonType() { return interruptReasonType; }
    public void setInterruptReasonType(Integer interruptReasonType) { this.interruptReasonType = interruptReasonType; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
