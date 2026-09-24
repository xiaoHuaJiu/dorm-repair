package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairWorkSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RepairWorkScheduleMapper {
    RepairWorkSchedule selectById(@Param("id") Long id);
    int insert(RepairWorkSchedule value);
    int update(RepairWorkSchedule value);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int countEnabledOverlap(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                            @Param("excludeId") Long excludeId);
    List<RepairWorkSchedule> selectPage(@Param("scheduleName") String scheduleName,
                                        @Param("status") Integer status, @Param("date") LocalDate date);
    List<RepairWorkSchedule> selectEnabledBetween(@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);
}
