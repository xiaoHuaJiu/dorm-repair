package com.dormrepair.transfer.enums;
public enum TransferReasonType {OUT_OF_SCOPE(1),NO_SKILL(2),UNAVAILABLE(3),NEED_OTHER_TRADE(4),OVERLOAD(5),OTHER(6);private final int code;TransferReasonType(int code){this.code=code;}public int code(){return code;}}
