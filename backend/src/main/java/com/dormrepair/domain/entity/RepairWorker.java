package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairWorker {
    private Long id;
    private Long userId;
    private String workerNo;
    private Integer workStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getWorkerNo() { return workerNo; }
    public void setWorkerNo(String workerNo) { this.workerNo = workerNo; }
    public Integer getWorkStatus() { return workStatus; }
    public void setWorkStatus(Integer workStatus) { this.workStatus = workStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
