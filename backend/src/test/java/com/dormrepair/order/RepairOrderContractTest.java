package com.dormrepair.order;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.order.dto.RepairOrderQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RepairOrderContractTest {
    @Test
    void publicQueryDoesNotExposeOwnershipFields() {
        var names = Arrays.stream(RepairOrderQueryRequest.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName).toList();
        assertThat(names).contains("orderNo", "statusList", "roomId", "acceptTimeout", "completeTimeout");
        assertThat(names).doesNotContain("studentUid", "currentAssigneeId");
    }

    @Test
    void orderErrorsUseNumericCodesAndHttpSemantics() {
        assertThat(ResultCodeEnum.ORDER_NOT_FOUND.getCode()).isEqualTo(17001);
        assertThat(new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ResultCodeEnum.ORDER_ACCESS_DENIED.getCode()).isEqualTo(17002);
        assertThat(new BusinessException(ResultCodeEnum.ORDER_ACCESS_DENIED).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void invalidTimeRangeIsRejected() {
        RepairOrderQueryRequest request = new RepairOrderQueryRequest();
        request.setReportStartTime(LocalDateTime.of(2026, 9, 24, 0, 0));
        request.setReportEndTime(LocalDateTime.of(2026, 9, 23, 0, 0));
        assertThat(request.hasInvalidTimeRange()).isTrue();
    }
}
