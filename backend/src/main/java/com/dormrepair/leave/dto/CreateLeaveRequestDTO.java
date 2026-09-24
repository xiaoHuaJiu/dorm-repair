package com.dormrepair.leave.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 维修人员提交请假申请请求。
 * workerId 由后端通过登录用户上下文解析，前端不得指定。
 */
public record CreateLeaveRequestDTO(
    @NotBlank @Size(max = 64) String bizNo,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotBlank @Size(max = 500) String reason) {}
