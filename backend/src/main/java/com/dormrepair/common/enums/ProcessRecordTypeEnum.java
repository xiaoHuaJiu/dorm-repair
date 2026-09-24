package com.dormrepair.common.enums;

public enum ProcessRecordTypeEnum {
    NORMAL(1), INTERRUPT(2), RESUME(3), SUBMIT_RESULT(4);
    private final int code;
    ProcessRecordTypeEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
