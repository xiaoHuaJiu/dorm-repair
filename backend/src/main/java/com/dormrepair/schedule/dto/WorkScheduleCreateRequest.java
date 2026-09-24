package com.dormrepair.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleCreateRequest(
    @NotBlank(message="方案名称不能为空") @Size(max=100,message="方案名称不能超过100个字符") String scheduleName,
    @NotNull(message="开始日期不能为空") LocalDate startDate, @NotNull(message="结束日期不能为空") LocalDate endDate,
    @NotNull(message="上班时间不能为空") LocalTime workStartTime, @NotNull(message="下班时间不能为空") LocalTime workEndTime,
    @NotNull(message="状态不能为空") Integer status, @Size(max=500,message="备注不能超过500个字符") String remark
) {}
