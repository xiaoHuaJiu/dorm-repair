package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairOrderFlow {
    private Long id;
    private Long orderId;
    private Integer operationType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long originalAssigneeId;
    private Long newAssigneeId;
    private Long operatorId;
    private Integer operatorRole;
    private Integer sourceType;
    private Long relatedBusinessId;
    private String reason;
    private LocalDateTime operationTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getOperationType() { return operationType; }
    public void setOperationType(Integer operationType) { this.operationType = operationType; }
    public Integer getFromStatus() { return fromStatus; }
    public void setFromStatus(Integer fromStatus) { this.fromStatus = fromStatus; }
    public Integer getToStatus() { return toStatus; }
    public void setToStatus(Integer toStatus) { this.toStatus = toStatus; }
    public Long getOriginalAssigneeId() { return originalAssigneeId; }
    public void setOriginalAssigneeId(Long originalAssigneeId) { this.originalAssigneeId = originalAssigneeId; }
    public Long getNewAssigneeId() { return newAssigneeId; }
    public void setNewAssigneeId(Long newAssigneeId) { this.newAssigneeId = newAssigneeId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Integer getOperatorRole() { return operatorRole; }
    public void setOperatorRole(Integer operatorRole) { this.operatorRole = operatorRole; }
    public Integer getSourceType() { return sourceType; }
    public void setSourceType(Integer sourceType) { this.sourceType = sourceType; }
    public Long getRelatedBusinessId() { return relatedBusinessId; }
    public void setRelatedBusinessId(Long relatedBusinessId) { this.relatedBusinessId = relatedBusinessId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getOperationTime() { return operationTime; }
    public void setOperationTime(LocalDateTime operationTime) { this.operationTime = operationTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
