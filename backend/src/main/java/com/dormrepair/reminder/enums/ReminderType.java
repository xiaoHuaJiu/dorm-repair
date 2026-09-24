package com.dormrepair.reminder.enums;
public enum ReminderType { ACCEPT(1), COMPLETE(2); private final int code; ReminderType(int code){this.code=code;} public int code(){return code;} }
