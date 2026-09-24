package com.dormrepair.domain.mapper;
import com.dormrepair.domain.entity.RepairHolidayCalendar;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
import java.time.LocalDate; import java.util.List;
@Mapper public interface RepairHolidayCalendarMapper {
 RepairHolidayCalendar selectById(@Param("id") Long id);
 RepairHolidayCalendar selectByDate(@Param("date") LocalDate date);
 List<RepairHolidayCalendar> selectBetween(@Param("start") LocalDate start,@Param("end") LocalDate end);
}
