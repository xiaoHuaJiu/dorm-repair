package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairReworkRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairReworkRecordMapper {
    RepairReworkRecord selectById(@Param("id") Long id);
    List<RepairReworkRecord> selectByOrderId(@Param("orderId") Long orderId);
}
