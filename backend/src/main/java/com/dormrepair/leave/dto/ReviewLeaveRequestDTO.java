package com.dormrepair.leave.dto;
import jakarta.validation.constraints.*;

/**
 * 管理员审批请假请求。action 取值 APPROVE（通过）或 REJECT（驳回）。
 */
public record ReviewLeaveRequestDTO(
    @NotBlank @Size(max = 64) String bizNo,
    @NotBlank @Pattern(regexp = "APPROVE|REJECT", message = "action 必须为 APPROVE 或 REJECT") String action,
    @Size(max = 500) String remark) {}
