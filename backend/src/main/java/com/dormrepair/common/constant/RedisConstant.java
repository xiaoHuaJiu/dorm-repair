package com.dormrepair.common.constant;

public final class RedisConstant {
    public static final String REPAIR_ORDER_CREATE_PREFIX = "idempotent:repair-order:create:";
    public static final long REPAIR_ORDER_CREATE_TTL_MINUTES = 10L;
    public static final String LEAVE_CREATE_PREFIX = "idempotent:leave-request:create:";
    public static final String LEAVE_REVIEW_PREFIX = "idempotent:leave-request:review:";
    public static final long LEAVE_IDEMPOTENT_TTL_MINUTES = 10L;

    private RedisConstant() {}

    public static String repairOrderCreateKey(Long studentUid, String bizNo) {
        return REPAIR_ORDER_CREATE_PREFIX + studentUid + ":" + bizNo;
    }

    public static String leaveCreateKey(Long userId, String bizNo) {
        return LEAVE_CREATE_PREFIX + userId + ":" + bizNo;
    }

    public static String leaveReviewKey(Long userId, String bizNo) {
        return LEAVE_REVIEW_PREFIX + userId + ":" + bizNo;
    }
}
