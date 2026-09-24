package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class RepairOrderAlert {
    private Long id;
    private Long orderId;
    private Integer reworkNo;
    private String alertType;
    private Integer alertLevel;
    private Integer alertStatus;
    private String alertContent;
    private Long handledBy;
    private LocalDateTime handledTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
    public Integer getReworkNo(){return reworkNo;} public void setReworkNo(Integer v){reworkNo=v;}
    public String getAlertType(){return alertType;} public void setAlertType(String v){alertType=v;}
    public Integer getAlertLevel(){return alertLevel;} public void setAlertLevel(Integer v){alertLevel=v;}
    public Integer getAlertStatus(){return alertStatus;} public void setAlertStatus(Integer v){alertStatus=v;}
    public String getAlertContent(){return alertContent;} public void setAlertContent(String v){alertContent=v;}
    public Long getHandledBy(){return handledBy;} public void setHandledBy(Long v){handledBy=v;}
    public LocalDateTime getHandledTime(){return handledTime;} public void setHandledTime(LocalDateTime v){handledTime=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
    public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
}
