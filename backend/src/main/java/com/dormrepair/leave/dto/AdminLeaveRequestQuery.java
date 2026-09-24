package com.dormrepair.leave.dto;

import com.dormrepair.common.result.PageQuery;
import java.time.LocalDateTime;

/**
 * 管理员查询请假申请列表。startTime/endTime 按请假开始时间区间过滤。
 */
public class AdminLeaveRequestQuery extends PageQuery {
    private Long workerId;
    private Integer approvalStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
