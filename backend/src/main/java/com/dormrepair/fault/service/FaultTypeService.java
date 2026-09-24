package com.dormrepair.fault.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.result.PageResult;
import com.dormrepair.domain.entity.RepairFaultType;
import com.dormrepair.domain.mapper.RepairFaultTypeMapper;
import com.dormrepair.fault.dto.*;
import com.dormrepair.fault.vo.FaultTypeResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FaultTypeService {
    private final RepairFaultTypeMapper mapper;
    public FaultTypeService(RepairFaultTypeMapper mapper){this.mapper=mapper;}

    @Transactional
    public Long create(FaultTypeCreateRequest request) {
        RepairFaultType value=new RepairFaultType();
        value.setTypeCode(request.typeCode().trim()); value.setTypeName(request.typeName().trim());
        value.setSortNo(request.sortNo()); value.setRemark(request.remark()); value.setStatus(1);
        try { mapper.insert(value); } catch (DuplicateKeyException ex) { throw new BusinessException(ResultCodeEnum.FAULT_CODE_EXISTS); }
        return value.getId();
    }
    @Transactional
    public void update(Long id, FaultTypeUpdateRequest request) {
        RepairFaultType value=require(id); value.setTypeName(request.typeName().trim());
        value.setSortNo(request.sortNo()); value.setRemark(request.remark()); mapper.update(value);
    }
    @Transactional public void updateStatus(Long id,Integer status){ requireStatus(status); require(id); mapper.updateStatus(id,status); }
    public FaultTypeResponse detail(Long id){return FaultTypeResponse.from(require(id));}
    public PageResult<FaultTypeResponse> page(FaultTypeQuery query){
        PageHelper.startPage(query.getPageNum(),query.getPageSize());
        PageInfo<RepairFaultType> page=new PageInfo<>(mapper.selectPage(query.getTypeCode(),query.getTypeName(),query.getStatus()));
        return PageResult.of(page.getTotal(),page.getPageNum(),page.getPageSize(),page.getList().stream().map(FaultTypeResponse::from).toList());
    }
    public List<FaultTypeResponse> enabled(){return mapper.selectEnabled().stream().map(FaultTypeResponse::from).toList();}
    private RepairFaultType require(Long id){RepairFaultType v=mapper.selectById(id);if(v==null)throw new BusinessException(ResultCodeEnum.FAULT_NOT_FOUND);return v;}
    private void requireStatus(Integer status){if(status==null||(status!=0&&status!=1))throw new BusinessException(ResultCodeEnum.PARAM_ERROR,"状态只能为0或1");}
}
