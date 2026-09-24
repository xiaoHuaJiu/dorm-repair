package com.dormrepair.common.enums;

public enum ResultCodeEnum {
    SUCCESS(0, "success"),
    PARAM_ERROR(10001, "请求参数错误"),
    UNAUTHORIZED(10002, "未登录或登录状态已失效"),
    FORBIDDEN(10003, "无权限访问"),
    DATA_NOT_FOUND(10004, "数据不存在"),
    DATA_CONFLICT(10005, "数据状态已发生变化"),
    OPERATION_NOT_ALLOWED(10006, "当前操作不允许"),
    LOGIN_FAILED(11001, "用户名或密码错误"),
    ACCOUNT_UNAVAILABLE(11002, "账号不可用"),
    USERNAME_EXISTS(12001, "该用户名已被使用，请更换后重试"),
    USER_NOT_FOUND(12002, "用户不存在"),
    PASSWORD_MISMATCH(12003, "两次输入的密码不一致"),
    FAULT_CODE_EXISTS(13001, "故障类型编码已存在"),
    FAULT_NOT_FOUND(13002, "故障类型不存在"),
    FAULT_UNAVAILABLE(13003, "故障类型已停用"),
    AREA_PARENT_INVALID(14001, "位置父子关系不合法"),
    AREA_NOT_FOUND(14002, "位置节点不存在"),
    AREA_DUPLICATE(14003, "同级位置名称已存在"),
    AREA_UNAVAILABLE(14004, "位置节点不可用"),
    WORKER_NO_EXISTS(15001, "维修人员编号已存在"),
    WORKER_NOT_FOUND(15002, "维修人员不存在"),
    WORKER_USER_EXISTS(15003, "该用户已绑定维修人员"),
    WORKER_USER_INVALID(15004, "用户不能绑定为维修人员"),
    WORKER_UNAVAILABLE(15005, "维修人员不可用"),
    WORKER_SCOPE_INVALID(15006, "维修人员负责区域不合法"),
    SCHEDULE_CONFLICT(16001, "工作时间方案存在生效区间冲突"),
    SCHEDULE_NOT_FOUND(16002, "工作时间方案不存在"),
    SCHEDULE_TIME_INVALID(16003, "工作时间方案的日期或时间不合法"),
    ORDER_NOT_FOUND(17001, "工单不存在"),
    ORDER_ACCESS_DENIED(17002, "无权访问该工单"),
    ORDER_IDEMPOTENT_PROCESSING(17003, "报修正在提交中，请勿重复操作"),
    ORDER_BIZ_NO_CONFLICT(17004, "当前提交标识已被使用，请刷新页面后重新提交"),
    ORDER_EVALUATION_EXISTS(17005, "该工单已评价，请勿重复评价"),
    FILE_EMPTY(18001,"上传文件不能为空"), FILE_BIZ_TYPE_INVALID(18002,"文件业务类型不合法"),
    FILE_TYPE_NOT_ALLOWED(18003,"文件格式不允许"), FILE_TOO_LARGE(18004,"文件大小超过限制"),
    FILE_NOT_FOUND(18005,"文件不存在"), FILE_NOT_OWNER(18006,"无权操作该文件"),
    FILE_ALREADY_BOUND(18007,"文件已经绑定业务"), FILE_UPLOAD_FAILED(18008,"文件上传失败"),
    FILE_DELETE_FAILED(18009,"文件删除失败"),
    SYSTEM_ERROR(50000, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    ResultCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
