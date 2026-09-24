package com.dormrepair.common.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.List;

public enum RepairOrderStatusEnum {
    PENDING_DISPATCH(0, "待派单"), PENDING_ACCEPTANCE(1, "待接单"), REPAIRING(2, "维修中"),
    PENDING_CONFIRMATION(3, "待确认"), REWORKING(4, "返工中"), INTERRUPTED(5, "已中断"),
    COMPLETED(6, "已完成"), CANCELLED(7, "已取消");
    private final int code;
    private final String description;
    RepairOrderStatusEnum(int code, String description) { this.code = code; this.description = description; }
    public int getCode() { return code; }
    public String getDescription() { return description; }
    public static Optional<RepairOrderStatusEnum> fromCode(Integer code) {
        return Arrays.stream(values()).filter(value -> Integer.valueOf(value.code).equals(code)).findFirst();
    }
    public static List<Integer> getOpenStatusCodes() { return List.of(0,1,2,3,4,5); }
}
