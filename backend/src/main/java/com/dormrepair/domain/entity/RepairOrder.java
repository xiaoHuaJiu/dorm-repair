package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairOrder {
    private Long id;
    private String orderNo;
    private Long studentUid;
    private String contactName;
    private String contactPhone;
    private Long campusId;
    private Long areaId;
    private Long buildingId;
    private Long roomId;
    private String locationDetail;
    private Long faultTypeId;
    private String problemDescription;
    private String imageUrls;
    private Integer status;
    private Long currentAssigneeId;
    private LocalDateTime dispatchTime;
    private LocalDateTime acceptDeadline;
    private LocalDateTime acceptTime;
    private LocalDateTime expectedCompleteTime;
    private LocalDateTime completeDeadline;
    private LocalDateTime repairSubmitTime;
    private LocalDateTime confirmTime;
    private LocalDateTime completeTime;
    private Integer reworkCount;
    private Integer exceptionFlag;
    private Integer duplicateFlag;
    private Long duplicateOrderId;
    private LocalDateTime reportTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getStudentUid() { return studentUid; }
    public void setStudentUid(Long studentUid) { this.studentUid = studentUid; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getLocationDetail() { return locationDetail; }
    public void setLocationDetail(String locationDetail) { this.locationDetail = locationDetail; }
    public Long getFaultTypeId() { return faultTypeId; }
    public void setFaultTypeId(Long faultTypeId) { this.faultTypeId = faultTypeId; }
    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getCurrentAssigneeId() { return currentAssigneeId; }
    public void setCurrentAssigneeId(Long currentAssigneeId) { this.currentAssigneeId = currentAssigneeId; }
    public LocalDateTime getDispatchTime() { return dispatchTime; }
    public void setDispatchTime(LocalDateTime dispatchTime) { this.dispatchTime = dispatchTime; }
    public LocalDateTime getAcceptDeadline() { return acceptDeadline; }
    public void setAcceptDeadline(LocalDateTime acceptDeadline) { this.acceptDeadline = acceptDeadline; }
    public LocalDateTime getAcceptTime() { return acceptTime; }
    public void setAcceptTime(LocalDateTime acceptTime) { this.acceptTime = acceptTime; }
    public LocalDateTime getExpectedCompleteTime() { return expectedCompleteTime; }
    public void setExpectedCompleteTime(LocalDateTime expectedCompleteTime) { this.expectedCompleteTime = expectedCompleteTime; }
    public LocalDateTime getCompleteDeadline() { return completeDeadline; }
    public void setCompleteDeadline(LocalDateTime completeDeadline) { this.completeDeadline = completeDeadline; }
    public LocalDateTime getRepairSubmitTime() { return repairSubmitTime; }
    public void setRepairSubmitTime(LocalDateTime repairSubmitTime) { this.repairSubmitTime = repairSubmitTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
    public Integer getReworkCount() { return reworkCount; }
    public void setReworkCount(Integer reworkCount) { this.reworkCount = reworkCount; }
    public Integer getExceptionFlag() { return exceptionFlag; }
    public void setExceptionFlag(Integer exceptionFlag) { this.exceptionFlag = exceptionFlag; }
    public Integer getDuplicateFlag() { return duplicateFlag; }
    public void setDuplicateFlag(Integer duplicateFlag) { this.duplicateFlag = duplicateFlag; }
    public Long getDuplicateOrderId() { return duplicateOrderId; }
    public void setDuplicateOrderId(Long duplicateOrderId) { this.duplicateOrderId = duplicateOrderId; }
    public LocalDateTime getReportTime() { return reportTime; }
    public void setReportTime(LocalDateTime reportTime) { this.reportTime = reportTime; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
