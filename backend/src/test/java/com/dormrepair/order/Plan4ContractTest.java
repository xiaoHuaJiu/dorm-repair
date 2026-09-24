package com.dormrepair.order;

import com.dormrepair.common.constant.RedisConstant;
import com.dormrepair.common.enums.RepairOrderStatusEnum;
import com.dormrepair.order.dto.CreateRepairOrderRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class Plan4ContractTest {
    @Test void redisKeyUsesCentralConstant() {
        assertThat(RedisConstant.repairOrderCreateKey(12L, "biz-1"))
            .isEqualTo("idempotent:repair-order:create:12:biz-1");
    }

    @Test void openStatusesExcludeCompletedAndCancelled() {
        assertThat(RepairOrderStatusEnum.getOpenStatusCodes()).containsExactly(0, 1, 2, 3, 4, 5);
    }

    @Test void createRequestDoesNotExposeIdentityOrDuplicateFlagAndRequiresBizNo() {
        assertThat(FieldNames.of(CreateRepairOrderRequest.class)).doesNotContain("studentUid", "duplicateFlag");
        var request = new CreateRepairOrderRequest();
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(request);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("bizNo", "campusId", "roomId");
    }

    private static final class FieldNames {
        static java.util.List<String> of(Class<?> type) {
            return java.util.Arrays.stream(type.getDeclaredFields()).map(Field::getName).toList();
        }
    }
}
