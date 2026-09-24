package com.dormrepair.common.enums;

import java.util.Arrays;

public enum AreaTypeEnum {
    CAMPUS(1, "单位/校区"), AREA(2, "区域"), BUILDING(3, "楼栋"), ROOM(4, "房间");

    private final int code;
    private final String description;
    AreaTypeEnum(int code, String description) { this.code = code; this.description = description; }
    public int getCode() { return code; }
    public String getDescription() { return description; }
    public static AreaTypeEnum require(int code) {
        return Arrays.stream(values()).filter(value -> value.code == code).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知位置类型"));
    }
    public boolean acceptsParent(Integer parentType) {
        return this == CAMPUS ? parentType == null : parentType != null && parentType == code - 1;
    }
}
