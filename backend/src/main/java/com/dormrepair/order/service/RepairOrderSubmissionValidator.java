package com.dormrepair.order.service;

import com.dormrepair.common.enums.AreaTypeEnum;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairArea;
import com.dormrepair.domain.entity.RepairFaultType;
import com.dormrepair.domain.mapper.RepairAreaMapper;
import com.dormrepair.domain.mapper.RepairFaultTypeMapper;
import com.dormrepair.order.dto.RepairOrderDuplicateCheckRequest;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderSubmissionValidator {
    private final RepairAreaMapper areas; private final RepairFaultTypeMapper faults;
    public RepairOrderSubmissionValidator(RepairAreaMapper areas,RepairFaultTypeMapper faults){this.areas=areas;this.faults=faults;}
    public void validate(RepairOrderDuplicateCheckRequest request){
        RepairArea campus=active(request.getCampusId(),AreaTypeEnum.CAMPUS);
        RepairArea area=active(request.getAreaId(),AreaTypeEnum.AREA);
        RepairArea building=active(request.getBuildingId(),AreaTypeEnum.BUILDING);
        RepairArea room=active(request.getRoomId(),AreaTypeEnum.ROOM);
        if(!campus.getId().equals(area.getParentId())||!area.getId().equals(building.getParentId())||!building.getId().equals(room.getParentId()))
            throw new BusinessException(ResultCodeEnum.AREA_PARENT_INVALID);
        RepairFaultType fault=faults.selectById(request.getFaultTypeId());
        if(fault==null) throw new BusinessException(ResultCodeEnum.FAULT_NOT_FOUND);
        if(!Integer.valueOf(1).equals(fault.getStatus())) throw new BusinessException(ResultCodeEnum.FAULT_UNAVAILABLE);
    }
    private RepairArea active(Long id,AreaTypeEnum type){
        RepairArea node=areas.selectById(id);
        if(node==null||Integer.valueOf(1).equals(node.getDeleted())) throw new BusinessException(ResultCodeEnum.AREA_NOT_FOUND);
        if(!Integer.valueOf(1).equals(node.getStatus())) throw new BusinessException(ResultCodeEnum.AREA_UNAVAILABLE);
        if(!Integer.valueOf(type.getCode()).equals(node.getAreaType())) throw new BusinessException(ResultCodeEnum.AREA_PARENT_INVALID);
        return node;
    }
}
