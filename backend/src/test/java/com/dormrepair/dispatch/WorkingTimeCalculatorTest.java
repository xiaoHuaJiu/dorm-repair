package com.dormrepair.dispatch;

import com.dormrepair.dispatch.model.DispatchFailureReason;
import com.dormrepair.dispatch.service.DispatchException;
import com.dormrepair.dispatch.service.WorkingTimeCalculator;
import com.dormrepair.domain.entity.RepairHolidayCalendar;
import com.dormrepair.domain.entity.RepairWorkSchedule;
import com.dormrepair.domain.mapper.RepairHolidayCalendarMapper;
import com.dormrepair.domain.mapper.RepairWorkScheduleMapper;
import org.junit.jupiter.api.Test;
import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;

class WorkingTimeCalculatorTest {
 @Test void accumulatesAcrossHolidayAndAdjustedWeekend(){
  RepairWorkScheduleMapper schedules=mock(RepairWorkScheduleMapper.class); RepairHolidayCalendarMapper holidays=mock(RepairHolidayCalendarMapper.class);
  when(schedules.selectEnabledBetween(any(),any())).thenReturn(List.of(schedule(LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),LocalTime.of(8,0),LocalTime.of(17,0))));
  when(holidays.selectBetween(any(),any())).thenReturn(List.of(holiday(LocalDate.of(2026,9,25),1),holiday(LocalDate.of(2026,9,20),2)));
  WorkingTimeCalculator calculator=new WorkingTimeCalculator(schedules,holidays);
  assertThat(calculator.calculateDeadline(LocalDateTime.of(2026,9,19,16,50),Duration.ofMinutes(30)))
    .isEqualTo(LocalDateTime.of(2026,9,20,8,30));
 }
 @Test void movesStartBeforeWorkToOpeningAndCrossesDay(){
  RepairWorkScheduleMapper schedules=mock(RepairWorkScheduleMapper.class); RepairHolidayCalendarMapper holidays=mock(RepairHolidayCalendarMapper.class);
  when(schedules.selectEnabledBetween(any(),any())).thenReturn(List.of(schedule(LocalDate.of(2026,9,21),LocalDate.of(2026,9,22),LocalTime.of(8,0),LocalTime.of(17,0))));
  when(holidays.selectBetween(any(),any())).thenReturn(List.of());
  WorkingTimeCalculator calculator=new WorkingTimeCalculator(schedules,holidays);
  assertThat(calculator.calculateDeadline(LocalDateTime.of(2026,9,21,16,50),Duration.ofMinutes(30))).isEqualTo(LocalDateTime.of(2026,9,22,8,20));
 }
 @Test void failsWhenNoScheduleWithinSearchWindow(){
  RepairWorkScheduleMapper schedules=mock(RepairWorkScheduleMapper.class); RepairHolidayCalendarMapper holidays=mock(RepairHolidayCalendarMapper.class);
  when(schedules.selectEnabledBetween(any(),any())).thenReturn(List.of()); when(holidays.selectBetween(any(),any())).thenReturn(List.of());
  assertThatThrownBy(()->new WorkingTimeCalculator(schedules,holidays).calculateDeadline(LocalDateTime.of(2026,1,1,9,0),Duration.ofMinutes(30)))
   .isInstanceOf(DispatchException.class).extracting("reason").isEqualTo(DispatchFailureReason.NO_WORK_SCHEDULE);
 }
 private RepairWorkSchedule schedule(LocalDate a,LocalDate b,LocalTime s,LocalTime e){RepairWorkSchedule v=new RepairWorkSchedule();v.setStartDate(a);v.setEndDate(b);v.setWorkStartTime(s);v.setWorkEndTime(e);v.setStatus(1);return v;}
 private RepairHolidayCalendar holiday(LocalDate d,int type){RepairHolidayCalendar v=new RepairHolidayCalendar();v.setHolidayDate(d);v.setDayType(type);return v;}
}
