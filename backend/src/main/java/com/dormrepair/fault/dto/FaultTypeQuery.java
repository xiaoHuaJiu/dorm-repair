package com.dormrepair.fault.dto;

import com.dormrepair.common.result.PageQuery;

public class FaultTypeQuery extends PageQuery {
    private String typeCode;
    private String typeName;
    private Integer status;
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
