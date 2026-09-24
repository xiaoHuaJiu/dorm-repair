package com.dormrepair.transfer.enums;
public enum TransferApprovalStatus {PENDING(0),APPROVED(1),REJECTED(2),INVALID(3);private final int code;TransferApprovalStatus(int code){this.code=code;}public int code(){return code;}}
