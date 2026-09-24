package com.dormrepair.order.vo;

import java.time.LocalDateTime;

public class SuspectedRepairOrder {
    private Long orderId; private String orderNo; private String faultTypeName; private String locationText;
    private LocalDateTime reportTime; private Integer status; private String statusName;
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
    public String getOrderNo(){return orderNo;} public void setOrderNo(String v){orderNo=v;}
    public String getFaultTypeName(){return faultTypeName;} public void setFaultTypeName(String v){faultTypeName=v;}
    public String getLocationText(){return locationText;} public void setLocationText(String v){locationText=v;}
    public LocalDateTime getReportTime(){return reportTime;} public void setReportTime(LocalDateTime v){reportTime=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;}
    public String getStatusName(){return statusName;} public void setStatusName(String v){statusName=v;}
}
