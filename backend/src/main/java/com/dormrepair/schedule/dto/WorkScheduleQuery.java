package com.dormrepair.schedule.dto;

import com.dormrepair.common.result.PageQuery;
import java.time.LocalDate;

public class WorkScheduleQuery extends PageQuery {
    private String scheduleName; private Integer status; private LocalDate date;
    public String getScheduleName(){return scheduleName;} public void setScheduleName(String v){scheduleName=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;}
}
