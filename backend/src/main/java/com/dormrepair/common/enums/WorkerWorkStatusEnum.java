package com.dormrepair.common.enums;

import java.util.Arrays;
import java.util.Optional;

public enum WorkerWorkStatusEnum {
    NORMAL(0, "正常"), ON_LEAVE(1, "请假中"), DISABLED(2, "停用");
    private final int code;
    private final String description;
    WorkerWorkStatusEnum(int code, String description) { this.code = code; this.description = description; }
    public int getCode() { return code; }
    public String getDescription() { return description; }
    public static Optional<WorkerWorkStatusEnum> fromCode(Integer code) {
        return Arrays.stream(values()).filter(value -> Integer.valueOf(value.code).equals(code)).findFirst();
    }
}
