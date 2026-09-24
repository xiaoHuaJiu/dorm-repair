package com.dormrepair.area.vo;

import com.dormrepair.domain.entity.RepairArea;
import java.util.ArrayList;
import java.util.List;

public class AreaTreeNode {
    private final Long id;
    private final Long parentId;
    private final String areaCode;
    private final String areaName;
    private final Integer areaType;
    private final Integer status;
    private final Integer sortNo;
    private final List<AreaTreeNode> children = new ArrayList<>();
    public AreaTreeNode(RepairArea area) {
        id=area.getId(); parentId=area.getParentId(); areaCode=area.getAreaCode();
        areaName=area.getAreaName(); areaType=area.getAreaType(); status=area.getStatus(); sortNo=area.getSortNo();
    }
    public Long getId(){return id;} public Long getParentId(){return parentId;}
    public String getAreaCode(){return areaCode;} public String getAreaName(){return areaName;}
    public Integer getAreaType(){return areaType;} public Integer getStatus(){return status;}
    public Integer getSortNo(){return sortNo;} public List<AreaTreeNode> getChildren(){return children;}
}
