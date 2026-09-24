package com.dormrepair.common.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UserRoleEnum {
    STUDENT(1, "学生"), WORKER(2, "维修人员"), ADMIN(3, "管理员");
    private final int code;
    private final String description;
    UserRoleEnum(int code, String description) { this.code = code; this.description = description; }
    public int getCode() { return code; }
    public String getDescription() { return description; }
    public static Optional<UserRoleEnum> fromCode(Integer code) {
        return Arrays.stream(values()).filter(role -> Integer.valueOf(role.code).equals(code)).findFirst();
    }
}
