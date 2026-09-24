package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairMaterialUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairMaterialUsageMapper {
    int insert(RepairMaterialUsage usage);
    RepairMaterialUsage selectById(@Param("id") Long id);
    List<RepairMaterialUsage> selectByOrderId(@Param("orderId") Long orderId);
}
