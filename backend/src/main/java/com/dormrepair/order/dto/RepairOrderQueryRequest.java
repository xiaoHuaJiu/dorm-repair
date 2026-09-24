package com.dormrepair.order.dto;

import com.dormrepair.common.result.PageQuery;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

public class RepairOrderQueryRequest extends PageQuery {
    private String orderNo;
    private List<Integer> statusList;
    private Long faultTypeId;
    private Long campusId;
    private Long areaId;
    private Long buildingId;
    private Long roomId;
    private Long workerId;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime reportStartTime;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime reportEndTime;
    private Boolean exceptionFlag;
    private Boolean suspectedDuplicate;
    private Boolean acceptTimeout;
    private Boolean completeTimeout;

    public boolean hasInvalidTimeRange(){return reportStartTime!=null&&reportEndTime!=null&&reportStartTime.isAfter(reportEndTime);}
    public String getOrderNo(){return orderNo;} public void setOrderNo(String v){orderNo=v;}
    public List<Integer> getStatusList(){return statusList;} public void setStatusList(List<Integer> v){statusList=v;}
    public Long getFaultTypeId(){return faultTypeId;} public void setFaultTypeId(Long v){faultTypeId=v;}
    public Long getCampusId(){return campusId;} public void setCampusId(Long v){campusId=v;}
    public Long getAreaId(){return areaId;} public void setAreaId(Long v){areaId=v;}
    public Long getBuildingId(){return buildingId;} public void setBuildingId(Long v){buildingId=v;}
    public Long getRoomId(){return roomId;} public void setRoomId(Long v){roomId=v;}
    public Long getWorkerId(){return workerId;} public void setWorkerId(Long v){workerId=v;}
    public LocalDateTime getReportStartTime(){return reportStartTime;} public void setReportStartTime(LocalDateTime v){reportStartTime=v;}
    public LocalDateTime getReportEndTime(){return reportEndTime;} public void setReportEndTime(LocalDateTime v){reportEndTime=v;}
    public Boolean getExceptionFlag(){return exceptionFlag;} public void setExceptionFlag(Boolean v){exceptionFlag=v;}
    public Boolean getSuspectedDuplicate(){return suspectedDuplicate;} public void setSuspectedDuplicate(Boolean v){suspectedDuplicate=v;}
    public Boolean getAcceptTimeout(){return acceptTimeout;} public void setAcceptTimeout(Boolean v){acceptTimeout=v;}
    public Boolean getCompleteTimeout(){return completeTimeout;} public void setCompleteTimeout(Boolean v){completeTimeout=v;}
}
