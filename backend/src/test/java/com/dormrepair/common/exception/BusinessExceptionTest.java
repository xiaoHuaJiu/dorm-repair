package com.dormrepair.common.exception;

import com.dormrepair.common.enums.ResultCodeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {
    @Test
    void preservesCodeAndAllowsSafeCustomMessage() {
        BusinessException standard = new BusinessException(ResultCodeEnum.DATA_NOT_FOUND);
        assertThat(standard.getCode()).isEqualTo(10004);
        assertThat(standard.getMessage()).isEqualTo("数据不存在");

        BusinessException custom = new BusinessException(ResultCodeEnum.DATA_NOT_FOUND, "记录不存在");
        assertThat(custom.getCode()).isEqualTo(10004);
        assertThat(custom.getMessage()).isEqualTo("记录不存在");
    }
}
