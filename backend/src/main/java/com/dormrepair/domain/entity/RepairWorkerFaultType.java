package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairWorkerFaultType {
    private Long id;
    private Long workerId;
    private Long faultTypeId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getFaultTypeId() { return faultTypeId; }
    public void setFaultTypeId(Long faultTypeId) { this.faultTypeId = faultTypeId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
