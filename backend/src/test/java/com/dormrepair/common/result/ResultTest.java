package com.dormrepair.common.result;

import com.dormrepair.common.enums.ResultCodeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void createsSuccessAndFailureResults() {
        assertThat(Result.success().getCode()).isZero();
        assertThat(Result.success().getMessage()).isEqualTo("success");
        assertThat(Result.success().getData()).isNull();

        Result<String> withData = Result.success("value");
        assertThat(withData.getData()).isEqualTo("value");

        Result<Void> failed = Result.fail(ResultCodeEnum.PARAM_ERROR);
        assertThat(failed.getCode()).isEqualTo(10001);
        assertThat(failed.getMessage()).isEqualTo("请求参数错误");

        Result<Void> custom = Result.fail(29999, "测试失败");
        assertThat(custom.getCode()).isEqualTo(29999);
        assertThat(custom.getMessage()).isEqualTo("测试失败");
    }
}
