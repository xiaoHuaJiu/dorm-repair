package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairReminderRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepairReminderRecordMapper {
    RepairReminderRecord selectById(@Param("id") Long id);
}
