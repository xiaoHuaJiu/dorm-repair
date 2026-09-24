package com.dormrepair.transfer.enums;
public enum TransferExecuteStatus {NOT_EXECUTED(0),EXECUTING(1),SUCCESS(2),FAILED(3);private final int code;TransferExecuteStatus(int code){this.code=code;}public int code(){return code;}}
