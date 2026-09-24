package com.dormrepair.leave.vo;

import java.time.LocalDateTime;

/**
 * 请假申请详情。维修人员端与管理员端共用。
 */
public class LeaveRequestDetailResponse {
    private Long leaveId;
    private Long workerId;
    private String workerName;
    private String workerNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Integer approvalStatus;
    private String approvalStatusName;
    private Integer reassignStatus;
    private String reassignStatusName;
    private Long reviewAdminId;
    private String reviewAdminName;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    public Long getLeaveId(){return leaveId;} public void setLeaveId(Long v){leaveId=v;}
    public Long getWorkerId(){return workerId;} public void setWorkerId(Long v){workerId=v;}
    public String getWorkerName(){return workerName;} public void setWorkerName(String v){workerName=v;}
    public String getWorkerNo(){return workerNo;} public void setWorkerNo(String v){workerNo=v;}
    public LocalDateTime getStartTime(){return startTime;} public void setStartTime(LocalDateTime v){startTime=v;}
    public LocalDateTime getEndTime(){return endTime;} public void setEndTime(LocalDateTime v){endTime=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public Integer getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(Integer v){approvalStatus=v;}
    public String getApprovalStatusName(){return approvalStatusName;} public void setApprovalStatusName(String v){approvalStatusName=v;}
    public Integer getReassignStatus(){return reassignStatus;} public void setReassignStatus(Integer v){reassignStatus=v;}
    public String getReassignStatusName(){return reassignStatusName;} public void setReassignStatusName(String v){reassignStatusName=v;}
    public Long getReviewAdminId(){return reviewAdminId;} public void setReviewAdminId(Long v){reviewAdminId=v;}
    public String getReviewAdminName(){return reviewAdminName;} public void setReviewAdminName(String v){reviewAdminName=v;}
    public String getReviewRemark(){return reviewRemark;} public void setReviewRemark(String v){reviewRemark=v;}
    public LocalDateTime getReviewTime(){return reviewTime;} public void setReviewTime(LocalDateTime v){reviewTime=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
    public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
}
