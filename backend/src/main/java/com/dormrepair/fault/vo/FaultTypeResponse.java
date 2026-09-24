package com.dormrepair.fault.vo;

import com.dormrepair.domain.entity.RepairFaultType;

public record FaultTypeResponse(Long id, String typeCode, String typeName, Integer status,
                                Integer sortNo, String remark) {
    public static FaultTypeResponse from(RepairFaultType value) {
        return new FaultTypeResponse(value.getId(), value.getTypeCode(), value.getTypeName(),
            value.getStatus(), value.getSortNo(), value.getRemark());
    }
}
