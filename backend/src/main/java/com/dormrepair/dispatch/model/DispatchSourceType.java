package com.dormrepair.dispatch.model;
public enum DispatchSourceType {
 INITIAL_REPORT(1), ACCEPT_TIMEOUT(2), LEAVE_REASSIGN(4), TRANSFER_REASSIGN(3);
 private final int code; DispatchSourceType(int code){this.code=code;} public int getCode(){return code;}
}
