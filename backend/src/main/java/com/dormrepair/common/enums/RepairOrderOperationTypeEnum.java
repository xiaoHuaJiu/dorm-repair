package com.dormrepair.common.enums;

public enum RepairOrderOperationTypeEnum {
    CREATE(1), AUTO_DISPATCH(2), ACCEPT(3), INTERRUPT(4), RESUME(5), SUBMIT_RESULT(6);
    private final int code;
    RepairOrderOperationTypeEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
