package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairTransferRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepairTransferRequestMapper {
    RepairTransferRequest selectById(@Param("id") Long id);
}
