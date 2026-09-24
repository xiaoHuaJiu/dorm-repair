package com.dormrepair.area.service;

import com.dormrepair.area.dto.*;
import com.dormrepair.area.vo.AreaTreeNode;
import com.dormrepair.common.enums.*;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairArea;
import com.dormrepair.domain.mapper.RepairAreaMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AreaService {
    private final RepairAreaMapper mapper;
    public AreaService(RepairAreaMapper mapper){this.mapper=mapper;}
    @Transactional public Long create(AreaCreateRequest r){
        AreaTypeEnum type;
        try{type=AreaTypeEnum.require(r.areaType());}catch(IllegalArgumentException ex){throw new BusinessException(ResultCodeEnum.AREA_PARENT_INVALID);}
        RepairArea parent=null;
        if(r.parentId()!=0){parent=require(r.parentId());if(parent.getStatus()!=1)throw new BusinessException(ResultCodeEnum.AREA_UNAVAILABLE);}
        if(!type.acceptsParent(parent==null?null:parent.getAreaType()))throw new BusinessException(ResultCodeEnum.AREA_PARENT_INVALID);
        if(mapper.countSiblingName(r.parentId(),r.areaType(),r.areaName().trim(),null)>0)throw new BusinessException(ResultCodeEnum.AREA_DUPLICATE);
        RepairArea value=new RepairArea(); value.setParentId(r.parentId());value.setAreaCode(r.areaCode().trim());
        value.setAreaName(r.areaName().trim());value.setAreaType(r.areaType());value.setSortNo(r.sortNo());
        value.setStatus(1);value.setRemark(r.remark());value.setDeleted(0);
        // 名称重复已在插入前查重拦截；此处唯一键冲突只可能来自 uk_area_code（位置编码全表唯一）。
        try{mapper.insert(value);}catch(DuplicateKeyException ex){throw new BusinessException(ResultCodeEnum.AREA_CODE_EXISTS);}
        return value.getId();
    }
    @Transactional public void update(Long id,AreaUpdateRequest r){RepairArea v=require(id);
        if(mapper.countSiblingName(v.getParentId(),v.getAreaType(),r.areaName().trim(),id)>0)throw new BusinessException(ResultCodeEnum.AREA_DUPLICATE);
        v.setAreaName(r.areaName().trim());v.setSortNo(r.sortNo());v.setRemark(r.remark());mapper.update(v);}
    @Transactional public void updateStatus(Long id,Integer status){if(status==null||(status!=0&&status!=1))throw new BusinessException(ResultCodeEnum.PARAM_ERROR);require(id);mapper.updateStatus(id,status);}
    public RepairArea detail(Long id){return require(id);}
    public List<AreaTreeNode> adminTree(){return tree(mapper.selectAll());}
    public List<AreaTreeNode> enabledTree(){return tree(mapper.selectEnabled());}
    public List<RepairArea> children(Long parentId,boolean enabledOnly){if(parentId!=0){RepairArea parent=require(parentId);if(enabledOnly&&parent.getStatus()!=1)return List.of();}return mapper.selectChildren(parentId,enabledOnly);}
    private List<AreaTreeNode> tree(List<RepairArea> values){Map<Long,AreaTreeNode> nodes=new LinkedHashMap<>();values.forEach(v->nodes.put(v.getId(),new AreaTreeNode(v)));
        List<AreaTreeNode> roots=new ArrayList<>();for(RepairArea v:values){AreaTreeNode n=nodes.get(v.getId());if(v.getParentId()==0)roots.add(n);else {AreaTreeNode p=nodes.get(v.getParentId());if(p!=null)p.getChildren().add(n);}}return roots;}
    private RepairArea require(Long id){RepairArea v=mapper.selectById(id);if(v==null||Integer.valueOf(1).equals(v.getDeleted()))throw new BusinessException(ResultCodeEnum.AREA_NOT_FOUND);return v;}
}
