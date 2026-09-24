package com.dormrepair.order.model;

import com.dormrepair.order.dto.RepairOrderQueryRequest;
import java.time.LocalDateTime;
import java.util.List;

public class RepairOrderQueryCondition {
    private final RepairOrderQueryRequest request;
    private Long studentUid;
    private Long currentAssigneeId;
    private LocalDateTime now;
    public RepairOrderQueryCondition(RepairOrderQueryRequest request){this.request=request;this.now=LocalDateTime.now();}
    public RepairOrderQueryRequest getRequest(){return request;}
    public String getOrderNo(){return request.getOrderNo();} public List<Integer> getStatusList(){return request.getStatusList();}
    public Long getFaultTypeId(){return request.getFaultTypeId();} public Long getCampusId(){return request.getCampusId();}
    public Long getAreaId(){return request.getAreaId();} public Long getBuildingId(){return request.getBuildingId();} public Long getRoomId(){return request.getRoomId();}
    public Long getWorkerId(){return request.getWorkerId();} public LocalDateTime getReportStartTime(){return request.getReportStartTime();}
    public LocalDateTime getReportEndTime(){return request.getReportEndTime();} public Boolean getExceptionFlag(){return request.getExceptionFlag();}
    public Boolean getSuspectedDuplicate(){return request.getSuspectedDuplicate();} public Boolean getAcceptTimeout(){return request.getAcceptTimeout();}
    public Boolean getCompleteTimeout(){return request.getCompleteTimeout();}
    public Long getStudentUid(){return studentUid;} public void setStudentUid(Long v){studentUid=v;}
    public Long getCurrentAssigneeId(){return currentAssigneeId;} public void setCurrentAssigneeId(Long v){currentAssigneeId=v;}
    public LocalDateTime getNow(){return now;} public void setNow(LocalDateTime v){now=v;}
}
