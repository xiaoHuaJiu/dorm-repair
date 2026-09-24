package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairReworkRecord {
    private Long id;
    private Long orderId;
    private Integer reworkNo;
    private Long applicantUid;
    private String reason;
    private String imageUrls;
    private Long originalAssigneeId;
    private Integer status;
    private Integer adminIntervention;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getReworkNo() { return reworkNo; }
    public void setReworkNo(Integer reworkNo) { this.reworkNo = reworkNo; }
    public Long getApplicantUid() { return applicantUid; }
    public void setApplicantUid(Long applicantUid) { this.applicantUid = applicantUid; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public Long getOriginalAssigneeId() { return originalAssigneeId; }
    public void setOriginalAssigneeId(Long originalAssigneeId) { this.originalAssigneeId = originalAssigneeId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getAdminIntervention() { return adminIntervention; }
    public void setAdminIntervention(Integer adminIntervention) { this.adminIntervention = adminIntervention; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
