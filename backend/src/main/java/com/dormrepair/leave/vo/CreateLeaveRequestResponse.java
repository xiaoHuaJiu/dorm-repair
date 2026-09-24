package com.dormrepair.leave.vo;

/**
 * 提交请假申请响应。approvalStatus 为创建后的审批状态（待审批）。
 */
public record CreateLeaveRequestResponse(Long leaveId, Integer approvalStatus) {}
