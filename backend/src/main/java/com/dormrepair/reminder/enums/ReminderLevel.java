package com.dormrepair.reminder.enums;
public enum ReminderLevel {
    MINUS_10(1,9,10), MINUS_5(2,4,5), TIMEOUT(3,0,0);
    private final int code; private final int fromMinutes; private final int toMinutes;
    ReminderLevel(int code,int fromMinutes,int toMinutes){this.code=code;this.fromMinutes=fromMinutes;this.toMinutes=toMinutes;}
    public int code(){return code;} public int fromMinutes(){return fromMinutes;} public int toMinutes(){return toMinutes;}
}
