package com.dormrepair.common.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 请假审批状态。
 * <p>对应 repair_leave_request.status：审批状态与转派处理状态分开维护，
 * 审批通过只代表管理员同意，不代表请假已经开始（见 repair_leave_request.reassign_status）。</p>
 */
public enum LeaveApprovalStatusEnum {
    PENDING(0, "待审批"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");
    private final int code;
    private final String description;
    LeaveApprovalStatusEnum(int code, String description) { this.code = code; this.description = description; }
    public int getCode() { return code; }
    public String getDescription() { return description; }
    public static Optional<LeaveApprovalStatusEnum> fromCode(Integer code) {
        return Arrays.stream(values()).filter(value -> Integer.valueOf(value.code).equals(code)).findFirst();
    }
}
