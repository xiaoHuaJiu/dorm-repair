package com.dormrepair.common.constant;

public final class RedisConstant {
    public static final String REPAIR_ORDER_CREATE_PREFIX = "idempotent:repair-order:create:";
    public static final long REPAIR_ORDER_CREATE_TTL_MINUTES = 10L;

    private RedisConstant() {}

    public static String repairOrderCreateKey(Long studentUid, String bizNo) {
        return REPAIR_ORDER_CREATE_PREFIX + studentUid + ":" + bizNo;
    }
}
