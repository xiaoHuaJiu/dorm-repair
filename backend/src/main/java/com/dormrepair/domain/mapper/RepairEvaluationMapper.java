package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepairEvaluationMapper {
    RepairEvaluation selectById(@Param("id") Long id);
    RepairEvaluation selectByOrderId(@Param("orderId") Long orderId);
    int insert(RepairEvaluation evaluation);
}
