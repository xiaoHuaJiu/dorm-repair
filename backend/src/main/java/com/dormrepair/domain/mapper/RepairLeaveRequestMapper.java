package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairLeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepairLeaveRequestMapper {
    RepairLeaveRequest selectById(@Param("id") Long id);
}
