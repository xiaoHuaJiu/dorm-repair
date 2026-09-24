package com.dormrepair.schedule.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.result.PageResult;
import com.dormrepair.domain.entity.RepairWorkSchedule;
import com.dormrepair.domain.mapper.RepairWorkScheduleMapper;
import com.dormrepair.schedule.dto.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkScheduleService {
    private final RepairWorkScheduleMapper mapper;
    public WorkScheduleService(RepairWorkScheduleMapper mapper){this.mapper=mapper;}
    @Transactional public Long create(WorkScheduleCreateRequest r,Long currentUserId){validate(r.startDate(),r.endDate(),r.workStartTime(),r.workEndTime(),r.status(),null);
        RepairWorkSchedule v=new RepairWorkSchedule();copy(v,r.scheduleName(),r.startDate(),r.endDate(),r.workStartTime(),r.workEndTime(),r.status(),r.remark());v.setCreateBy(currentUserId);mapper.insert(v);return v.getId();}
    @Transactional public void update(Long id,WorkScheduleUpdateRequest r){require(id);validate(r.startDate(),r.endDate(),r.workStartTime(),r.workEndTime(),r.status(),id);
        RepairWorkSchedule v=new RepairWorkSchedule();v.setId(id);copy(v,r.scheduleName(),r.startDate(),r.endDate(),r.workStartTime(),r.workEndTime(),r.status(),r.remark());mapper.update(v);}
    @Transactional public void updateStatus(Long id,Integer status){RepairWorkSchedule v=require(id);if(status==null||(status!=0&&status!=1))throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        if(status==1&&mapper.countEnabledOverlap(v.getStartDate(),v.getEndDate(),id)>0)throw new BusinessException(ResultCodeEnum.SCHEDULE_CONFLICT);mapper.updateStatus(id,status);}
    public RepairWorkSchedule detail(Long id){return require(id);}
    public PageResult<RepairWorkSchedule> page(WorkScheduleQuery q){PageHelper.startPage(q.getPageNum(),q.getPageSize());return PageResult.from(new PageInfo<>(mapper.selectPage(q.getScheduleName(),q.getStatus(),q.getDate())));}
    private void validate(java.time.LocalDate sd,java.time.LocalDate ed,java.time.LocalTime st,java.time.LocalTime et,Integer status,Long exclude){
        if(sd.isAfter(ed)||!st.isBefore(et)||status==null||(status!=0&&status!=1))throw new BusinessException(ResultCodeEnum.SCHEDULE_TIME_INVALID);
        if(status==1&&mapper.countEnabledOverlap(sd,ed,exclude)>0)throw new BusinessException(ResultCodeEnum.SCHEDULE_CONFLICT);}
    private void copy(RepairWorkSchedule v,String name,java.time.LocalDate sd,java.time.LocalDate ed,java.time.LocalTime st,java.time.LocalTime et,Integer status,String remark){v.setScheduleName(name.trim());v.setStartDate(sd);v.setEndDate(ed);v.setWorkStartTime(st);v.setWorkEndTime(et);v.setStatus(status);v.setRemark(remark);}
    private RepairWorkSchedule require(Long id){RepairWorkSchedule v=mapper.selectById(id);if(v==null)throw new BusinessException(ResultCodeEnum.SCHEDULE_NOT_FOUND);return v;}
}
