package com.dormrepair.transfer;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.dispatch.model.DispatchResult;
import com.dormrepair.dispatch.model.DispatchFailureReason;
import com.dormrepair.dispatch.model.DispatchSourceType;
import com.dormrepair.dispatch.service.RepairDispatchService;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.entity.RepairTransferRequest;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.transfer.dto.ReviewTransferRequest;
import com.dormrepair.transfer.enums.TransferReviewAction;
import com.dormrepair.transfer.service.AdminTransferRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminTransferRequestServiceTest {
    RepairTransferRequestMapper transfers;
    RepairDispatchService dispatch;
    AdminTransferRequestService service;

    @BeforeEach void setup() {
        transfers = mock(RepairTransferRequestMapper.class);
        RepairOrderMapper orders = mock(RepairOrderMapper.class);
        dispatch = mock(RepairDispatchService.class);
        service = new AdminTransferRequestService(transfers, orders, mock(SysIdempotentRecordMapper.class),
            mock(SysOperationLogMapper.class), dispatch, new ObjectMapper());
        when(transfers.selectByIdForUpdate(8L)).thenReturn(transfer());
        when(transfers.casReview(eq(8L), anyInt(), eq(1L), any(), any())).thenReturn(1);
        when(transfers.markExecution(eq(8L), anyInt(), any(), any(), any())).thenReturn(1);
        when(orders.selectById(6L)).thenReturn(order(2, 10L));
        LoginUser admin = new LoginUser(1L, "admin", "管理员", UserRoleEnum.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void approveDispatchesAndExcludesOriginalWorker() {
        when(dispatch.dispatch(eq(6L), eq(Set.of(10L)), eq(DispatchSourceType.TRANSFER_REASSIGN)))
            .thenReturn(DispatchResult.success(11L));
        var out = service.review(8L, new ReviewTransferRequest("review-1", TransferReviewAction.APPROVE, "同意"));
        assertThat(out.dispatchSuccess()).isTrue();
        assertThat(out.newAssigneeId()).isEqualTo(11L);
        verify(transfers).markExecution(eq(8L), eq(2), eq(11L), isNull(), any());
    }

    @Test void rejectDoesNotDispatch() {
        var out = service.review(8L, new ReviewTransferRequest("review-2", TransferReviewAction.REJECT, "驳回"));
        assertThat(out.reviewSuccess()).isTrue();
        assertThat(out.dispatchSuccess()).isFalse();
        verifyNoInteractions(dispatch);
        verify(transfers, never()).markExecution(anyLong(), anyInt(), any(), any(), any());
    }

    @Test void approvedDispatchFailureIsReturnedAsBusinessResult() {
        when(dispatch.dispatch(eq(6L), eq(Set.of(10L)), eq(DispatchSourceType.TRANSFER_REASSIGN)))
            .thenReturn(DispatchResult.failure(DispatchFailureReason.NO_AVAILABLE_WORKER));
        var out = service.review(8L, new ReviewTransferRequest("review-failed", TransferReviewAction.APPROVE, "同意"));
        assertThat(out.reviewSuccess()).isTrue();
        assertThat(out.dispatchSuccess()).isFalse();
        assertThat(out.failureReason()).isEqualTo("NO_AVAILABLE_WORKER");
        verify(transfers).markExecution(eq(8L), eq(3), isNull(), eq("NO_AVAILABLE_WORKER"), any());
    }

    @Test void changedAssigneeCannotBeApproved() {
        RepairOrder changed = order(2, 99L);
        RepairOrderMapper orders = mock(RepairOrderMapper.class);
        when(orders.selectById(6L)).thenReturn(changed);
        service = new AdminTransferRequestService(transfers, orders, mock(SysIdempotentRecordMapper.class),
            mock(SysOperationLogMapper.class), dispatch, new ObjectMapper());
        assertThatThrownBy(() -> service.review(8L, new ReviewTransferRequest("review-3", TransferReviewAction.APPROVE, null)))
            .isInstanceOf(BusinessException.class).extracting("code").isEqualTo(19004);
        verifyNoInteractions(dispatch);
    }

    private RepairTransferRequest transfer() {
        RepairTransferRequest value = new RepairTransferRequest(); value.setId(8L); value.setOrderId(6L);
        value.setOriginalAssigneeId(10L); value.setApplicantWorkerId(10L); value.setApprovalStatus(0); return value;
    }
    private RepairOrder order(int status, Long worker) {
        RepairOrder value = new RepairOrder(); value.setId(6L); value.setStatus(status); value.setCurrentAssigneeId(worker); return value;
    }
}
