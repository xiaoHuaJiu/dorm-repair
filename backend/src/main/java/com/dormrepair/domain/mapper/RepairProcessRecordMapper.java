package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairProcessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairProcessRecordMapper {
    int insert(RepairProcessRecord record);
    RepairProcessRecord selectById(@Param("id") Long id);
    List<RepairProcessRecord> selectByOrderId(@Param("orderId") Long orderId);
}
