package com.dormrepair.order.dto;

import jakarta.validation.constraints.NotNull;

public class RepairOrderDuplicateCheckRequest {
    @NotNull private Long campusId;
    @NotNull private Long areaId;
    @NotNull private Long buildingId;
    @NotNull private Long roomId;
    @NotNull private Long faultTypeId;
    public Long getCampusId(){return campusId;} public void setCampusId(Long v){campusId=v;}
    public Long getAreaId(){return areaId;} public void setAreaId(Long v){areaId=v;}
    public Long getBuildingId(){return buildingId;} public void setBuildingId(Long v){buildingId=v;}
    public Long getRoomId(){return roomId;} public void setRoomId(Long v){roomId=v;}
    public Long getFaultTypeId(){return faultTypeId;} public void setFaultTypeId(Long v){faultTypeId=v;}
}
