package com.dormrepair.order.vo;

import java.time.LocalDateTime;

public class RepairOrderListItem {
    private Long orderId; private String orderNo; private Integer status; private String statusName;
    private Long studentUid; private String studentName; private Long faultTypeId; private String faultTypeName;
    private Long campusId; private String campusName; private Long areaId; private String areaName;
    private Long buildingId; private String buildingName; private Long roomId; private String roomName;
    private String locationText; private Long currentAssigneeId; private String workerName;
    private LocalDateTime reportTime; private LocalDateTime acceptDeadline; private LocalDateTime completeDeadline; private LocalDateTime completeTime;
    private Integer exceptionFlag; private Integer duplicateFlag; private Boolean acceptTimeout; private Boolean completeTimeout;
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;} public String getOrderNo(){return orderNo;} public void setOrderNo(String v){orderNo=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;} public String getStatusName(){return statusName;} public void setStatusName(String v){statusName=v;}
    public Long getStudentUid(){return studentUid;} public void setStudentUid(Long v){studentUid=v;} public String getStudentName(){return studentName;} public void setStudentName(String v){studentName=v;}
    public Long getFaultTypeId(){return faultTypeId;} public void setFaultTypeId(Long v){faultTypeId=v;} public String getFaultTypeName(){return faultTypeName;} public void setFaultTypeName(String v){faultTypeName=v;}
    public Long getCampusId(){return campusId;} public void setCampusId(Long v){campusId=v;} public String getCampusName(){return campusName;} public void setCampusName(String v){campusName=v;}
    public Long getAreaId(){return areaId;} public void setAreaId(Long v){areaId=v;} public String getAreaName(){return areaName;} public void setAreaName(String v){areaName=v;}
    public Long getBuildingId(){return buildingId;} public void setBuildingId(Long v){buildingId=v;} public String getBuildingName(){return buildingName;} public void setBuildingName(String v){buildingName=v;}
    public Long getRoomId(){return roomId;} public void setRoomId(Long v){roomId=v;} public String getRoomName(){return roomName;} public void setRoomName(String v){roomName=v;}
    public String getLocationText(){return locationText;} public void setLocationText(String v){locationText=v;} public Long getCurrentAssigneeId(){return currentAssigneeId;} public void setCurrentAssigneeId(Long v){currentAssigneeId=v;}
    public String getWorkerName(){return workerName;} public void setWorkerName(String v){workerName=v;} public LocalDateTime getReportTime(){return reportTime;} public void setReportTime(LocalDateTime v){reportTime=v;}
    public LocalDateTime getAcceptDeadline(){return acceptDeadline;} public void setAcceptDeadline(LocalDateTime v){acceptDeadline=v;} public LocalDateTime getCompleteDeadline(){return completeDeadline;} public void setCompleteDeadline(LocalDateTime v){completeDeadline=v;}
    public LocalDateTime getCompleteTime(){return completeTime;} public void setCompleteTime(LocalDateTime v){completeTime=v;} public Integer getExceptionFlag(){return exceptionFlag;} public void setExceptionFlag(Integer v){exceptionFlag=v;}
    public Integer getDuplicateFlag(){return duplicateFlag;} public void setDuplicateFlag(Integer v){duplicateFlag=v;} public Boolean getAcceptTimeout(){return acceptTimeout;} public void setAcceptTimeout(Boolean v){acceptTimeout=v;}
    public Boolean getCompleteTimeout(){return completeTimeout;} public void setCompleteTimeout(Boolean v){completeTimeout=v;}
}
