package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysOperationLogMapper {
    SysOperationLog selectById(@Param("id") Long id);
    int insert(SysOperationLog log);
}
