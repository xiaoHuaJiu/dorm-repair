package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairTransferRequest {
    private Long id;
    private Long orderId;
    private Long applicantWorkerId;
    private Long originalAssigneeId;
    private Integer reasonType;
    private String reasonDescription;
    private Integer approvalStatus;
    private Long reviewAdminId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private Integer executeStatus;
    private Long newAssigneeId;
    private LocalDateTime executeTime;
    private String failureReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getApplicantWorkerId() { return applicantWorkerId; }
    public void setApplicantWorkerId(Long applicantWorkerId) { this.applicantWorkerId = applicantWorkerId; }
    public Long getOriginalAssigneeId() { return originalAssigneeId; }
    public void setOriginalAssigneeId(Long originalAssigneeId) { this.originalAssigneeId = originalAssigneeId; }
    public Integer getReasonType() { return reasonType; }
    public void setReasonType(Integer reasonType) { this.reasonType = reasonType; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
    public Long getReviewAdminId() { return reviewAdminId; }
    public void setReviewAdminId(Long reviewAdminId) { this.reviewAdminId = reviewAdminId; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
    public Integer getExecuteStatus() { return executeStatus; }
    public void setExecuteStatus(Integer executeStatus) { this.executeStatus = executeStatus; }
    public Long getNewAssigneeId() { return newAssigneeId; }
    public void setNewAssigneeId(Long newAssigneeId) { this.newAssigneeId = newAssigneeId; }
    public LocalDateTime getExecuteTime() { return executeTime; }
    public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
