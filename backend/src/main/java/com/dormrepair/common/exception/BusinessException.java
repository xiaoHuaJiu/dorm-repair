package com.dormrepair.common.exception;

import com.dormrepair.common.enums.ResultCodeEnum;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final Integer code;
    private final HttpStatus httpStatus;

    public BusinessException(ResultCodeEnum resultCode) {
        this(resultCode, resultCode.getMessage(), defaultStatus(resultCode));
    }

    public BusinessException(ResultCodeEnum resultCode, String message) {
        this(resultCode, message, defaultStatus(resultCode));
    }

    public BusinessException(ResultCodeEnum resultCode, HttpStatus httpStatus) {
        this(resultCode, resultCode.getMessage(), httpStatus);
    }

    public BusinessException(ResultCodeEnum resultCode, String message, HttpStatus httpStatus) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = httpStatus;
    }

    public Integer getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }

    private static HttpStatus defaultStatus(ResultCodeEnum resultCode) {
        return switch (resultCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case DATA_NOT_FOUND, USER_NOT_FOUND, FAULT_NOT_FOUND, AREA_NOT_FOUND,
                WORKER_NOT_FOUND, SCHEDULE_NOT_FOUND, ORDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ORDER_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case DATA_CONFLICT, USERNAME_EXISTS, FAULT_CODE_EXISTS, AREA_DUPLICATE,
                WORKER_NO_EXISTS, WORKER_USER_EXISTS, SCHEDULE_CONFLICT,
                ORDER_IDEMPOTENT_PROCESSING, ORDER_BIZ_NO_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
