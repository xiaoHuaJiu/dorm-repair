package com.dormrepair.common.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 请假转派处理状态。
 * <p>对应 repair_leave_request.reassign_status：表示请假正式开始后，
 * 该维修人员已有未完成工单是否已经完成重新派单。
 * 状态机：0 未处理 → 1 处理中 → 2 已完成 / 3 部分失败。</p>
 */
public enum LeaveReassignStatusEnum {
    PENDING(0, "未处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    PARTIAL_FAILED(3, "部分失败");
    private final int code;
    private final String description;
    LeaveReassignStatusEnum(int code, String description) { this.code = code; this.description = description; }
    public int getCode() { return code; }
    public String getDescription() { return description; }
    public static Optional<LeaveReassignStatusEnum> fromCode(Integer code) {
        return Arrays.stream(values()).filter(value -> Integer.valueOf(value.code).equals(code)).findFirst();
    }
}
