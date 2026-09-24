package com.dormrepair.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RepairHolidayCalendar {
    private Long id; private LocalDate holidayDate; private String holidayName; private Integer dayType;
    private Integer year; private LocalDateTime createTime; private LocalDateTime updateTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public LocalDate getHolidayDate(){return holidayDate;} public void setHolidayDate(LocalDate v){holidayDate=v;}
    public String getHolidayName(){return holidayName;} public void setHolidayName(String v){holidayName=v;}
    public Integer getDayType(){return dayType;} public void setDayType(Integer v){dayType=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
    public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
}
