package com.dormrepair.common.enums;
public enum RepairOrderAlertTypeEnum {
    REWORK_WARNING(2), REWORK_EXCEPTION(3);
    private final int level; RepairOrderAlertTypeEnum(int level){this.level=level;} public int getLevel(){return level;}
}
