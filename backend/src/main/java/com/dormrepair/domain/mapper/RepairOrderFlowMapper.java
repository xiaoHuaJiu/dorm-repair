package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairOrderFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairOrderFlowMapper {
    RepairOrderFlow selectById(@Param("id") Long id);
    int insert(RepairOrderFlow flow);
    List<RepairOrderFlow> selectByOrderId(@Param("orderId") Long orderId);
}
