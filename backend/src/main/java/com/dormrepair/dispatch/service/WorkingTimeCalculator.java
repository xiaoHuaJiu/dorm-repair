package com.dormrepair.dispatch.service;

import com.dormrepair.dispatch.model.DispatchFailureReason;
import com.dormrepair.domain.entity.*; import com.dormrepair.domain.mapper.*;
import org.springframework.stereotype.Service;
import java.time.*; import java.util.*; import java.util.function.Function; import java.util.stream.Collectors;

@Service public class WorkingTimeCalculator {
 private static final int MAX_SEARCH_DAYS=366;
 private final RepairWorkScheduleMapper schedules; private final RepairHolidayCalendarMapper holidays;
 public WorkingTimeCalculator(RepairWorkScheduleMapper schedules,RepairHolidayCalendarMapper holidays){this.schedules=schedules;this.holidays=holidays;}
 public LocalDateTime calculateDeadline(LocalDateTime start,Duration duration){
  if(start==null||duration==null||duration.isNegative()||duration.isZero())throw new IllegalArgumentException("开始时间和有效时长必须有效");
  LocalDate endDate=start.toLocalDate().plusDays(MAX_SEARCH_DAYS-1L);
  List<RepairWorkSchedule> plans=schedules.selectEnabledBetween(start.toLocalDate(),endDate);
  Map<LocalDate,RepairHolidayCalendar> calendar=holidays.selectBetween(start.toLocalDate(),endDate).stream().collect(Collectors.toMap(RepairHolidayCalendar::getHolidayDate,Function.identity()));
  long remaining=duration.toMinutes(); LocalDateTime cursor=start;
  for(int offset=0;offset<MAX_SEARCH_DAYS;offset++){
   LocalDate date=start.toLocalDate().plusDays(offset); RepairWorkSchedule plan=findPlan(plans,date);
   RepairHolidayCalendar special=calendar.get(date); boolean workday=special!=null?Integer.valueOf(2).equals(special.getDayType()):date.getDayOfWeek()!=DayOfWeek.SATURDAY&&date.getDayOfWeek()!=DayOfWeek.SUNDAY;
   if(plan==null||!workday)continue;
   LocalDateTime open=date.atTime(plan.getWorkStartTime()),close=date.atTime(plan.getWorkEndTime());
   LocalDateTime segmentStart=cursor.isAfter(open)?cursor:open; if(!segmentStart.isBefore(close))continue;
   long available=Duration.between(segmentStart,close).toMinutes();
   if(available>=remaining)return segmentStart.plusMinutes(remaining);
   remaining-=available; cursor=date.plusDays(1).atStartOfDay();
  }
  throw new DispatchException(DispatchFailureReason.NO_WORK_SCHEDULE,"366天内没有足够的有效工作时间");
 }
 private RepairWorkSchedule findPlan(List<RepairWorkSchedule> plans,LocalDate date){return plans.stream().filter(p->!date.isBefore(p.getStartDate())&&!date.isAfter(p.getEndDate())).findFirst().orElse(null);}
}
