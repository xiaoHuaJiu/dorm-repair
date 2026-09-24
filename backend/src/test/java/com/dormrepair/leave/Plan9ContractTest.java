package com.dormrepair.leave;

import com.dormrepair.common.enums.LeaveApprovalStatusEnum;
import com.dormrepair.common.enums.LeaveReassignStatusEnum;
import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.dto.ReviewLeaveRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-9 请假接口契约测试：DTO 参数校验与状态枚举语义（暂未运行）。
 */
class Plan9ContractTest {
    private Validator validator;

    @BeforeEach
    void setup() { validator = Validation.buildDefaultValidatorFactory().getValidator(); }

    @Test
    void createLeaveValidation() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 26, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 28, 18, 0);
        assertThat(validator.validate(new CreateLeaveRequestDTO("", start, end, "事假"))).isNotEmpty();
        assertThat(validator.validate(new CreateLeaveRequestDTO("biz-1", null, end, "事假"))).isNotEmpty();
        assertThat(validator.validate(new CreateLeaveRequestDTO("biz-1", start, null, "事假"))).isNotEmpty();
        assertThat(validator.validate(new CreateLeaveRequestDTO("biz-1", start, end, "  "))).isNotEmpty();
        assertThat(validator.validate(new CreateLeaveRequestDTO("biz-1", start, end, "x".repeat(501)))).isNotEmpty();
        assertThat(validator.validate(new CreateLeaveRequestDTO("x".repeat(65), start, end, "事假"))).isNotEmpty();
        assertThat(validator.validate(new CreateLeaveRequestDTO("biz-1", start, end, "事假"))).isEmpty();
    }

    @Test
    void reviewLeaveValidation() {
        assertThat(validator.validate(new ReviewLeaveRequestDTO("biz-1", "AGREE", "同意"))).isNotEmpty();
        assertThat(validator.validate(new ReviewLeaveRequestDTO("biz-1", "approve", "同意"))).isNotEmpty();
        assertThat(validator.validate(new ReviewLeaveRequestDTO("", "APPROVE", null))).isNotEmpty();
        assertThat(validator.validate(new ReviewLeaveRequestDTO("biz-1", "REJECT", "x".repeat(501)))).isNotEmpty();
        assertThat(validator.validate(new ReviewLeaveRequestDTO("biz-1", "APPROVE", null))).isEmpty();
        assertThat(validator.validate(new ReviewLeaveRequestDTO("biz-1", "REJECT", "不批准"))).isEmpty();
    }

    @Test
    void approvalStatusCodes() {
        assertThat(LeaveApprovalStatusEnum.PENDING.getCode()).isEqualTo(0);
        assertThat(LeaveApprovalStatusEnum.APPROVED.getCode()).isEqualTo(1);
        assertThat(LeaveApprovalStatusEnum.REJECTED.getCode()).isEqualTo(2);
        assertThat(LeaveApprovalStatusEnum.fromCode(0)).contains(LeaveApprovalStatusEnum.PENDING);
        assertThat(LeaveApprovalStatusEnum.fromCode(9)).isEmpty();
    }

    @Test
    void reassignStatusCodes() {
        assertThat(LeaveReassignStatusEnum.PENDING.getCode()).isEqualTo(0);
        assertThat(LeaveReassignStatusEnum.PROCESSING.getCode()).isEqualTo(1);
        assertThat(LeaveReassignStatusEnum.COMPLETED.getCode()).isEqualTo(2);
        assertThat(LeaveReassignStatusEnum.PARTIAL_FAILED.getCode()).isEqualTo(3);
        assertThat(LeaveReassignStatusEnum.fromCode(3)).contains(LeaveReassignStatusEnum.PARTIAL_FAILED);
        assertThat(LeaveReassignStatusEnum.fromCode(4)).isEmpty();
    }
}
