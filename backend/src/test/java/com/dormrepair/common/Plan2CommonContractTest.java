package com.dormrepair.common;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.result.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Plan2CommonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesStablePlan2ErrorCodesAndHttpStatus() {
        assertThat(ResultCodeEnum.USERNAME_EXISTS.getCode()).isEqualTo(12001);
        assertThat(ResultCodeEnum.FAULT_CODE_EXISTS.getCode()).isEqualTo(13001);
        assertThat(ResultCodeEnum.AREA_PARENT_INVALID.getCode()).isEqualTo(14001);
        assertThat(ResultCodeEnum.AREA_CODE_EXISTS.getCode()).isEqualTo(14005);
        assertThat(new BusinessException(ResultCodeEnum.AREA_CODE_EXISTS).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResultCodeEnum.WORKER_NO_EXISTS.getCode()).isEqualTo(15001);
        assertThat(ResultCodeEnum.SCHEDULE_CONFLICT.getCode()).isEqualTo(16001);

        BusinessException exception = new BusinessException(ResultCodeEnum.USERNAME_EXISTS, HttpStatus.CONFLICT);
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void serializesPageItemsAsRecordsInsteadOfList() throws Exception {
        PageResult<String> page = PageResult.from(new PageInfo<>(List.of("一号楼")));

        String json = objectMapper.writeValueAsString(page);

        assertThat(json).contains("\"records\":[\"一号楼\"]");
        assertThat(json).doesNotContain("\"list\"");
    }
}
