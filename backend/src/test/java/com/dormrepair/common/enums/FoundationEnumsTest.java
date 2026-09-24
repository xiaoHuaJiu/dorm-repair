package com.dormrepair.common.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FoundationEnumsTest {
    @Test void exposesStableRoleCodesAndDescriptions() {
        assertThat(UserRoleEnum.STUDENT.getCode()).isEqualTo(1);
        assertThat(UserRoleEnum.WORKER.getDescription()).isEqualTo("维修人员");
        assertThat(UserRoleEnum.ADMIN.getDescription()).isEqualTo("管理员");
    }

    @Test void exposesStableWorkerAndOrderStatuses() {
        assertThat(WorkerWorkStatusEnum.NORMAL.getCode()).isZero();
        assertThat(WorkerWorkStatusEnum.ON_LEAVE.getDescription()).isEqualTo("请假中");
        assertThat(WorkerWorkStatusEnum.DISABLED.getCode()).isEqualTo(2);
        assertThat(RepairOrderStatusEnum.values()).extracting(RepairOrderStatusEnum::getCode)
            .containsExactly(0, 1, 2, 3, 4, 5, 6, 7);
        assertThat(RepairOrderStatusEnum.fromCode(6)).contains(RepairOrderStatusEnum.COMPLETED);
        assertThat(RepairOrderStatusEnum.fromCode(99)).isEmpty();
    }
}
