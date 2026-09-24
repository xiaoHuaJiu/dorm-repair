package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairDispatchAlert {
    private Long id; private Long orderId; private Integer sourceType; private String failureReason;
    private Integer alertStatus; private String failureDetail; private Integer occurrenceCount;
    private LocalDateTime firstOccurredTime; private LocalDateTime lastOccurredTime;
    private Long handledBy; private LocalDateTime handledTime; private LocalDateTime createTime; private LocalDateTime updateTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
    public Integer getSourceType(){return sourceType;} public void setSourceType(Integer v){sourceType=v;}
    public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;}
    public Integer getAlertStatus(){return alertStatus;} public void setAlertStatus(Integer v){alertStatus=v;}
    public String getFailureDetail(){return failureDetail;} public void setFailureDetail(String v){failureDetail=v;}
    public Integer getOccurrenceCount(){return occurrenceCount;} public void setOccurrenceCount(Integer v){occurrenceCount=v;}
    public LocalDateTime getFirstOccurredTime(){return firstOccurredTime;} public void setFirstOccurredTime(LocalDateTime v){firstOccurredTime=v;}
    public LocalDateTime getLastOccurredTime(){return lastOccurredTime;} public void setLastOccurredTime(LocalDateTime v){lastOccurredTime=v;}
    public Long getHandledBy(){return handledBy;} public void setHandledBy(Long v){handledBy=v;}
    public LocalDateTime getHandledTime(){return handledTime;} public void setHandledTime(LocalDateTime v){handledTime=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
    public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
}
